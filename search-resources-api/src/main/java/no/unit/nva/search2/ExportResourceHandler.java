package no.unit.nva.search2;

import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.resource.ResourceClient.defaultClient;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.search2.resource.ResourceQuery;
import nva.commons.apigateway.ApiS3GatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class ExportResourceHandler extends ApiS3GatewayHandler<Void> {

    private final ResourceClient opensearchClient;

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
        return ResourceQuery.builder()
                   .fromRequestInfo(requestInfo)
                   .validate()
                   .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                   .withoutRange()
                   .withoutAggregation()
                   .withOnlyCsvFields()
                   .doExport(opensearchClient);
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
