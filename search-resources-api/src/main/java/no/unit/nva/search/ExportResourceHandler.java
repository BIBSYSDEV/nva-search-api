package no.unit.nva.search;

import static no.unit.nva.search.common.constant.Words.NONE;
import static no.unit.nva.search.common.constant.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.NODES_INCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;

import com.amazonaws.services.lambda.runtime.Context;

import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import no.unit.nva.search.scroll.ScrollClient;
import no.unit.nva.search.scroll.ScrollQuery;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import nva.commons.apigateway.ApiS3GatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Sondre Vestad
 * @author Stig Norland
 */
public class ExportResourceHandler extends ApiS3GatewayHandler<Void> {

    public static final String MAX_HITS_PER_PAGE = "2500";
    public static final String SCROLL_TTL = "1m";
    public static final String INCLUDED_NODES = String.join(COMMA, ResourceCsvTransformer.getJsonFields());
    private final ResourceClient opensearchClient;
    private final ScrollClient scrollClient;

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
                .withParameter(FROM, ZERO)
                .withParameter(SIZE, MAX_HITS_PER_PAGE)
                .withParameter(AGGREGATION, NONE)
                .withParameter(NODES_INCLUDED, INCLUDED_NODES)
                .build()
                .withFilter().requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .withScrollTime(SCROLL_TTL)
                .doSearch(opensearchClient)
                .swsResponse();

        return
            ScrollQuery.builder()
                .withInitialResponse(initialResponse)
                .withScrollTime(SCROLL_TTL)
                .build()
                .doSearch(scrollClient)
                .toCsvText();
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