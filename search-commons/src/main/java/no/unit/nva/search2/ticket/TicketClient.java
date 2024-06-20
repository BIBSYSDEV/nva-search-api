package no.unit.nva.search2.ticket;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;

import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

/**
 * @author Stig Norland
 */
public class TicketClient extends OpenSearchClient<SwsResponse, TicketSearchQuery> {

    public TicketClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @Override
    protected SwsResponse handleResponse(CompletableFuture<HttpResponse<String>> response) {
        return response.thenApply(action -> {
                if (action.statusCode() != HTTP_OK) {
                    throw new RuntimeException(action.body());
                }
                return attempt(() -> singleLineObjectMapper.readValue(action.body(), SwsResponse.class))
                    .map(logAndReturnResult())
                    .orElseThrow();
            })
            .join();
    }

    @Override
    protected BinaryOperator<SwsResponse> responseAccumulator() {
        return (a, b) -> SwsResponse.SwsResponseBuilder.swsResponseBuilder().merge(a).merge(b).build();
    }

    @JacocoGenerated
    public static TicketClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        return new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }

}
