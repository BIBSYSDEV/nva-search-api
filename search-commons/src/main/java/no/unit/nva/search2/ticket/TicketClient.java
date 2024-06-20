package no.unit.nva.search2.ticket;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search2.common.jwt.Tools.getCachedJwtProvider;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.function.BinaryOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;

/**
 * @author Stig Norland
 */
public class TicketClient extends OpenSearchClient<SwsResponse, TicketSearchQuery> {

    public TicketClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }


    @JacocoGenerated
    public static TicketClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        return new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }

    @Override
    protected SwsResponse jsonToResponse(HttpResponse<String> response) throws JsonProcessingException {
        return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
    }

    @Override
    protected BinaryOperator<SwsResponse> responseAccumulator() {
        return (a, b) -> SwsResponse.SwsResponseBuilder.swsResponseBuilder().merge(a).merge(b).build();
    }

    @Override
    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
        return result -> {
            logger.info(buildLogInfo(result));
            return result;
        };
    }

}
