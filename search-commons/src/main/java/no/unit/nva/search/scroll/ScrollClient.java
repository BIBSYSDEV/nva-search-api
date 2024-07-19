package no.unit.nva.search.scroll;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.common.jwt.Tools.getCachedJwtProvider;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;


/**
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
        return
            query.assemble()
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow().join();
    }

    @Override
    protected SwsResponse jsonToResponse(HttpResponse<String> response) throws JsonProcessingException {
        return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
    }

    @Override
    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
        return result -> {
            logger.info(buildLogInfo(result));
            return result;
        };
    }

}