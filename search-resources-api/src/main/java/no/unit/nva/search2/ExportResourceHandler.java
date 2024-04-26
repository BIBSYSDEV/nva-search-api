package no.unit.nva.search2;

import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import no.unit.nva.search.ResourceCsvTransformer;
import no.unit.nva.search2.common.records.SwsResponse;
import no.unit.nva.search2.common.scroll.ScrollClient;
import no.unit.nva.search2.common.scroll.ScrollQuery;
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

    public static final int MAX_PAGES = 20;
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
        var initalResponse = ResourceSearchQuery.builder()
                   .fromRequestInfo(requestInfo)
                   .validate()
                   .build()
                   .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
                   .withoutRange()
                   .withoutAggregation()
                   .withScrollTime("10m")
                   .withOnlyCsvFields()
                   .doSearchRaw(opensearchClient);

        var allPages = new ArrayList<SwsResponse>();
        allPages.add(initalResponse);
        scrollResults(allPages, initalResponse);

        return toCsv(allPages);
    }

    private void scrollResults(List<SwsResponse> allPages, SwsResponse previousResponse) {
        if (Objects.isNull(previousResponse._scroll_id()) || previousResponse.getSearchHits().isEmpty()) {
            return;
        }

        if (allPages.size() > MAX_PAGES) {
            logger.warn("Stopped recurssion due to too many pages");
        }
        var scrollId = previousResponse._scroll_id();
        logger.info("Scrolling on scrollId" + scrollId + " pagecount " + allPages.size());

        var scrollResponse = ScrollQuery.forScrollId(scrollId).doSearchRaw(this.scrollClient);

        allPages.add(scrollResponse);
        scrollResults(allPages, scrollResponse);
    }

    private String toCsv(List<SwsResponse> responses) {
        var allHits = responses.stream()
                          .map(SwsResponse::getSearchHits).flatMap(List::stream).toList();
        return ResourceCsvTransformer.transform(allHits);
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
