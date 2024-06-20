package no.unit.nva.search2.resource;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search2.common.jwt.Tools.getCachedJwtProvider;
import static no.unit.nva.search2.common.records.SwsResponse.SwsResponseBuilder.swsResponseBuilder;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;


/**
 * @author Stig Norland
 */
public class ResourceClient extends OpenSearchClient<SwsResponse, ResourceSearchQuery> {

    private final UserSettingsClient userSettingsClient;
    @SuppressWarnings("PMD.DoNotUseThreads")
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);


    public ResourceClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
        this.userSettingsClient = new UserSettingsClient(client, cachedJwtProvider);
    }

    public ResourceClient(HttpClient client, UserSettingsClient userSettingsClient,
                          CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
        this.userSettingsClient = userSettingsClient;
    }

    @JacocoGenerated
    public static ResourceClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        var httpClient = HttpClient.newBuilder()
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
    protected SwsResponse jsonToResponse(HttpResponse<String> response) throws JsonProcessingException {
        return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
    }

    @Override
    protected BinaryOperator<SwsResponse> responseAccumulator() {
        return (a, b) -> swsResponseBuilder().merge(a).merge(b).build();
    }

    @Override
    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
        return result -> {
            logger.info(buildLogInfo(result));
            return result;
        };
    }

}