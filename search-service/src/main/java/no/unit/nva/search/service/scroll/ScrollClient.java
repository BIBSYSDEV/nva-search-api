package no.unit.nva.search.service.scroll;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.model.jwt.Tools.getCachedJwtProvider;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.unit.nva.search.model.OpenSearchClient;
import no.unit.nva.search.model.jwt.CachedJwtProvider;
import no.unit.nva.search.model.records.SwsResponse;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.function.BinaryOperator;

/**
 * ScrollClient is a class that sends a request to the search index.
 *
 * @author Sondre Vestad
 */
public class ScrollClient extends OpenSearchClient<SwsResponse, ScrollQuery> {

    public ScrollClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @JacocoGenerated
    public static ScrollClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        return new ScrollClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }

    @Override
    public SwsResponse doSearch(ScrollQuery query) {
        queryBuilderStart = query.getStartTime();
        return query.assemble()
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst()
                .orElseThrow()
                .join();
    }

    @Override
    protected SwsResponse jsonToResponse(HttpResponse<String> response)
            throws JsonProcessingException {
        return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
    }

    @Override
    @JacocoGenerated
    protected BinaryOperator<SwsResponse> responseAccumulator() {
        // not in use
        return null;
    }

    @Override
    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException>
            logAndReturnResult() {
        return result -> {
            logger.info(buildLogInfo(result));
            return result;
        };
    }
}
