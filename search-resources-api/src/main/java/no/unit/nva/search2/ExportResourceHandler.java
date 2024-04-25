package no.unit.nva.search2;

import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.resource.ResourceClient.defaultClient;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.unit.nva.search.ResourceCsvTransformer;
import no.unit.nva.search2.common.records.SwsResponse;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.search2.resource.ResourceQuery;
import nva.commons.apigateway.ApiS3GatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class ExportResourceHandler extends ApiS3GatewayHandler<Void> {

    private final ResourceClient opensearchClient;
    private static final Logger logger = LoggerFactory.getLogger(ExportResourceHandler.class);

    @JacocoGenerated
    public ExportResourceHandler() {
        this(defaultClient(),
             defaultS3Client(),
             defaultS3Presigner());
    }

    public ExportResourceHandler(ResourceClient resourceClient,
                                 S3Client s3Client,
                                 S3Presigner s3Presigner
    ) {
        super(Void.class, s3Client, s3Presigner);
        this.opensearchClient = resourceClient;
    }

    @Override
    public String processS3Input(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
        var initalResponse = ResourceQuery.builder()
                   .fromRequestInfo(requestInfo)
                   .validate()
                   .build()
                   .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
                   .withoutRange()
                   .withoutAggregation()
                   .withScrollTime("10m")
                   .withOnlyCsvFields()
                   .doSearchRaw(opensearchClient);

        logger.info("scroll_id" + initalResponse._scroll_id());
        logger.info("hits" + initalResponse.hits().total().value());

        return toCsv(List.of(initalResponse));
    }

    private String toCsv(List<SwsResponse> initalResponse) {
        return ResourceCsvTransformer.transform(initalResponse.get(0).getSearchHits());
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
