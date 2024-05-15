package no.unit.nva.search2;

import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.ZERO;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search2.resource.ResourceParameter.INCLUDES;
import static no.unit.nva.search2.resource.ResourceParameter.PAGE;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search.ResourceCsvTransformer;
import no.unit.nva.search2.common.records.SwsResponse;
import no.unit.nva.search2.scroll.ScrollClient;
import no.unit.nva.search2.scroll.ScrollQuery;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.search2.resource.ResourceSearchQuery;
import nva.commons.apigateway.ApiS3GatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class ExportResourceHandler extends ApiS3GatewayHandler<Void> {

    public static final int MAX_PAGES = 4;
    public static final String MAX_HITS_PER_PAGE = "2500";
    public static final String SCROLL_TTL = "1m";
    public static final String INCLUDED_NODES = String.join(COMMA, ResourceCsvTransformer.getJsonFields());
    private final ResourceClient opensearchClient;
    private final ScrollClient scrollClient;
    private static final Logger logger = LoggerFactory.getLogger(ExportResourceHandler.class);

    @JacocoGenerated
    public ExportResourceHandler() {
        this(ResourceClient.defaultClient(),
            ScrollClient.defaultClient(),
            defaultS3Client(),
            defaultS3Presigner());
    }

    public ExportResourceHandler(ResourceClient resourceClient,
                                 ScrollClient scrollClient,
                                 S3Client s3Client,
                                 S3Presigner s3Presigner
    ) {
        super(Void.class, s3Client, s3Presigner);
        this.opensearchClient = resourceClient;
        this.scrollClient = scrollClient;
    }

    @Override
    public String processS3Input(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
        var initialResponse =
            ResourceSearchQuery.builder()
                .fromRequestInfo(requestInfo)
                .withParameter(PAGE, ZERO)
                .withParameter(SIZE, MAX_HITS_PER_PAGE)
                .withParameter(AGGREGATION, NONE)
                .withParameter(INCLUDES, INCLUDED_NODES)
                .build()
                .withFilter().requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .withScrollTime(SCROLL_TTL)
                .doSearch(opensearchClient)
                .swsResponse();

        return ResourceCsvTransformer
            .transform(scrollFetch(initialResponse, 0).toList());
    }


    private Stream<JsonNode> scrollFetch(SwsResponse previousResponse, int level) {
        if (shouldStopRecursion(level + 1, previousResponse)) {
            return previousResponse.getSearchHits().stream();
        }
        var scrollId = previousResponse._scroll_id();
        var currentResponse =
            new ScrollQuery(scrollId, SCROLL_TTL)
                .doSearch(this.scrollClient)
                .swsResponse();

        return Stream.concat(
            previousResponse.getSearchHits().stream(),
            scrollFetch(currentResponse, level + 1)
        );
    }


    private boolean shouldStopRecursion(Integer level, SwsResponse previousResponse) {
        if (Objects.isNull(previousResponse._scroll_id())) {
            logger.warn("Stopped recurssion due to no scroll_id");
            return true;
        }

        if (previousResponse.getSearchHits().isEmpty()) {
            logger.info("Stopped recurssion due to no more hits");
            return true;
        }

        if (level >= MAX_PAGES) {
            logger.warn("Stopped recurssion due to too many pages");
            return true;
        }

        return false;
    }


    @Override
    protected String getContentType() {
        return "text/csv";
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return super.getSuccessStatusCode(input, output);
    }

}
