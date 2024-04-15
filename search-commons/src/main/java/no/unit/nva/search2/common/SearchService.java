package no.unit.nva.search2.common;

import static no.unit.nva.search2.common.enums.PublicationStatus.DELETED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search2.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.search2.resource.ResourceQuery;
import no.unit.nva.search2.ticket.TicketClient;
import no.unit.nva.search2.ticket.TicketParameter;
import no.unit.nva.search2.ticket.TicketQuery;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.testcontainers.shaded.org.bouncycastle.cert.ocsp.Req;

public class SearchService {


    public String searchResources(ResourceClient resourceClient, RequestInfo requestInfo) throws BadRequestException {
        return ResourceQuery.builder()
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .validate()
            .build()
            .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
            .doSearch(resourceClient);
    }

    public String searchResourcesAuthenticated(ResourceClient resourceClient, RequestInfo requestInfo)
        throws BadRequestException, UnauthorizedException {
        return
            ResourceQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .validate()
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA, DELETED, UNPUBLISHED)
                .withOrganization(requestInfo.getCurrentCustomer())
                .doSearch(resourceClient);
    }

    public String searchTickets(TicketClient ticketClient, RequestInfo requestInfo) throws BadRequestException,
                                                                                UnauthorizedException {
            return TicketQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(TicketParameter.FROM, TicketParameter.SIZE, TicketParameter.AGGREGATION)
                .build()
                .applyContextAndAuthorize(requestInfo)
                .doSearch(ticketClient);
    }

}
