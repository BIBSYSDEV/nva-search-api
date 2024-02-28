package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.search2.ticket.TicketClient;
import no.unit.nva.search2.ticket.TicketQuery;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.List;

import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search2.common.enums.PublicationStatus.DELETED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search2.ticket.TicketClient.defaultClient;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;

public class SearchTicketAuthHandler extends ApiGatewayHandler<Void, String> {

    private final TicketClient opensearchClient;

    @JacocoGenerated
    public SearchTicketAuthHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchTicketAuthHandler(Environment environment, TicketClient TicketClient) {
        super(Void.class, environment);
        this.opensearchClient = TicketClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException, UnauthorizedException {

        validateAccessRight(requestInfo);

        return
            TicketQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE)
                .validate()
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA, DELETED, UNPUBLISHED)
                .withOrganization(requestInfo.getCurrentCustomer())
                .doSearch(opensearchClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    private void validateAccessRight(RequestInfo requestInfo) throws UnauthorizedException {
        if (requestInfo.userIsAuthorized(AccessRight.MANAGE_OWN_AFFILIATION)
            || requestInfo.userIsAuthorized(AccessRight.MANAGE_RESOURCES_STANDARD)) {
            return;
        }

        throw new UnauthorizedException();
    }
}
