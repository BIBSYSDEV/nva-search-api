package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_ENTITY_TOO_LARGE;
import static no.unit.nva.constants.Words.NONE;
import static no.unit.nva.constants.Words.ZERO;
import static no.unit.nva.search.ExportResourceHandler.AttemptResponse.AttemptStatus.OTHER_FAILURE;
import static no.unit.nva.search.ExportResourceHandler.AttemptResponse.AttemptStatus.SIZE_LIMIT_EXCEEDED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.NODES_INCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.concurrent.CompletionException;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.OpenSearchClientException;
import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.scroll.RecursiveScrollQuery;
import no.unit.nva.search.scroll.ScrollClient;
import nva.commons.apigateway.ApiS3GatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Handler for exporting resources to CSV.
 *
 * @author Sondre Vestad
 * @author Stig Norland
 */
public class ExportResourceHandler extends ApiS3GatewayHandler<Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportResourceHandler.class);

  public static final int INITIAL_HITS_PER_PAGE = 500;
  public static final String SCROLL_TTL = "1m";
  public static final String INCLUDED_NODES =
      String.join(COMMA, ResourceCsvTransformer.getJsonFields());
  private static final int SPLIT_LIMIT = 100;
  private static final int TWO = 2;
  private final ResourceClient opensearchClient;
  private final ScrollClient scrollClient;

  @JacocoGenerated
  public ExportResourceHandler() {
    this(
        ResourceClient.defaultClient(),
        ScrollClient.defaultClient(),
        defaultS3Client(),
        defaultS3Presigner(),
        new Environment());
  }

  public ExportResourceHandler(
      ResourceClient resourceClient,
      ScrollClient scrollClient,
      S3Client s3Client,
      S3Presigner s3Presigner,
      Environment environment) {
    super(Void.class, s3Client, s3Presigner, environment);
    this.opensearchClient = resourceClient;
    this.scrollClient = scrollClient;
  }

  @Override
  public String processS3Input(Void input, RequestInfo requestInfo, Context context)
      throws BadRequestException {

    var currentPageSize = INITIAL_HITS_PER_PAGE;
    AttemptResponse response;
    do {
      response = attemptsWithPageSize(currentPageSize, requestInfo);
      if (SIZE_LIMIT_EXCEEDED.equals(response.status)) {
        var nextPageSize = currentPageSize / TWO;
        LOGGER.info(
            "Request entity too large encountered with page size {}, trying again with {}",
            currentPageSize,
            nextPageSize);
        currentPageSize = nextPageSize;
      } else if (OTHER_FAILURE.equals(response.status)) {
        throw new RuntimeException(response.causeOfFailure);
      }
    } while (SIZE_LIMIT_EXCEEDED.equals(response.status));

    return response.result;
  }

  AttemptResponse attemptsWithPageSize(int pageSize, RequestInfo requestInfo)
      throws BadRequestException {
    try {
      var initialResponse =
          ResourceSearchQuery.builder()
              .fromRequestInfo(requestInfo)
              .withParameter(FROM, ZERO)
              .withParameter(SIZE, Integer.toString(pageSize))
              .withParameter(AGGREGATION, NONE)
              .withParameter(NODES_INCLUDED, INCLUDED_NODES)
              .withRequiredParameters(SORT)
              .build()
              .withFilter()
              .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
              .apply()
              .withScrollTime(SCROLL_TTL)
              .doSearch(opensearchClient, Words.RESOURCES)
              .swsResponse();

      return AttemptResponse.success(
          RecursiveScrollQuery.builder()
              .withInitialResponse(initialResponse)
              .withScrollTime(SCROLL_TTL)
              .build()
              .doSearch(scrollClient, Words.RESOURCES)
              .toCsvText());
    } catch (CompletionException completionException) {
      if (isSizeLimitExceededError(completionException, pageSize)) {
        return AttemptResponse.sizeLimitExceeded();
      }
      return AttemptResponse.otherFailure(completionException);
    }
  }

  @JacocoGenerated
  @Override
  protected String getContentType() {
    return "text/csv";
  }

  @JacocoGenerated
  @Override
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) {
    // Do nothing
  }

  @JacocoGenerated
  @Override
  protected Integer getSuccessStatusCode(Void input, Void output) {
    return super.getSuccessStatusCode(input, output);
  }

  record AttemptResponse(AttemptStatus status, String result, Throwable causeOfFailure) {
    protected enum AttemptStatus {
      SUCCESS,
      SIZE_LIMIT_EXCEEDED,
      OTHER_FAILURE
    }

    public static AttemptResponse success(String result) {
      return new AttemptResponse(AttemptStatus.SUCCESS, result, null);
    }

    public static AttemptResponse otherFailure(Throwable causeOfFailure) {
      return new AttemptResponse(OTHER_FAILURE, null, causeOfFailure);
    }

    public static AttemptResponse sizeLimitExceeded() {
      return new AttemptResponse(SIZE_LIMIT_EXCEEDED, null, null);
    }
  }

  private boolean isSizeLimitExceededError(CompletionException exception, int pageSize) {
    if (!(exception.getCause() instanceof OpenSearchClientException cause)) {
      return false;
    }
    return cause.getStatusCode() == HTTP_ENTITY_TOO_LARGE && pageSize > SPLIT_LIMIT;
  }
}
