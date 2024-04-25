package no.unit.nva.search2;

import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search2.ticket.TicketClient.defaultClient;
import static no.unit.nva.search2.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.search2.ticket.TicketClient;
import no.unit.nva.search2.ticket.TicketQuery;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SearchTicketAuthHandler extends ApiGatewayHandler<Void, String> {

    private final TicketClient opensearchClient;

    @JacocoGenerated
    public SearchTicketAuthHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchTicketAuthHandler(Environment environment, TicketClient ticketClient) {
        super(Void.class, environment);
        this.opensearchClient = ticketClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException, UnauthorizedException {

        return
            TicketQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .build()
                .withFilter().fromRequestInfo(requestInfo)
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

}
