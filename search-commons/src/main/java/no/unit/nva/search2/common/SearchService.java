package no.unit.nva.search2.common;

import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.search2.resource.ResourceQuery;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;

public class SearchService {

    private final ResourceClient resourceClient;

    public SearchService(ResourceClient resourceClient) {
        this.resourceClient = resourceClient;
    }

    public String searchResources(RequestInfo requestInfo) throws BadRequestException {
        return ResourceQuery.builder()
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .validate()
            .build()
            .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
            .doSearch(resourceClient);
    }

}
