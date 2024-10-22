package no.unit.nva.search.service.resource;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.model.jwt.Tools.getCachedJwtProvider;
import static no.unit.nva.search.model.records.SwsResponse.SwsResponseBuilder.swsResponseBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.unit.nva.search.model.OpenSearchClient;
import no.unit.nva.search.model.jwt.CachedJwtProvider;
import no.unit.nva.search.model.records.SwsResponse;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;

/**
 * Client for searching resources.
 *
 * @author Stig Norland
 */
public class ResourceClient extends OpenSearchClient<SwsResponse, ResourceSearchQuery> {

    @SuppressWarnings("PMD.DoNotUseThreads")
    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);

    private final UserSettingsClient userSettingsClient;

    public ResourceClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        this(client, cachedJwtProvider, new UserSettingsClient(client, cachedJwtProvider));
    }

    public ResourceClient(
            HttpClient client,
            CachedJwtProvider jwtProvider,
            UserSettingsClient userSettingsClient) {
        super(client, jwtProvider);
        this.userSettingsClient = userSettingsClient;
    }

    @JacocoGenerated
    public static ResourceClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        var httpClient =
                HttpClient.newBuilder()
                        .executor(executorService)
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        return new ResourceClient(httpClient, cachedJwtProvider);
    }

    @Override
    public SwsResponse doSearch(ResourceSearchQuery query) {
        return super.doSearch(query.withUserSettings(userSettingsClient));
    }

    @Override
    protected SwsResponse jsonToResponse(HttpResponse<String> response)
            throws JsonProcessingException {
        return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
    }

    @Override
    protected BinaryOperator<SwsResponse> responseAccumulator() {
        return (a, b) -> swsResponseBuilder().merge(a).merge(b).build();
    }

    @Override
    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException>
            logAndReturnResult() {
        return result -> {
            OpenSearchClient.logger.info(buildLogInfo(result));
            return result;
        };
    }
}
