package no.unit.nva.search2.resource;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search2.common.constant.Words.AMPERSAND;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;

import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.records.SwsResponse;
import no.unit.nva.search2.common.records.SwsResponse.SwsResponseBuilder;
import nva.commons.core.JacocoGenerated;
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
        queryBuilderStart = query.getStartTime();
        queryParameters = query.parameters().asMap()
            .entrySet().stream()
            .map(Object::toString)
            .collect(joining(AMPERSAND));
        return
            query.withUserSettings(userSettingsClient)
                .assemble()
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .reduce(responseAccumulator())
                .orElseThrow();
    }

    @Override
    protected BinaryOperator<SwsResponse> responseAccumulator() {
        return (a, b) -> SwsResponseBuilder.swsResponseBuilder().merge(a).merge(b).build();
    }

    @Override
    protected SwsResponse handleResponse(CompletableFuture<HttpResponse<String>> futureResponse) {
        return futureResponse
            .thenApply(action -> {
                if (action.statusCode() != HTTP_OK) {
                    throw new RuntimeException(action.body());
                }
                return attempt(() -> singleLineObjectMapper.readValue(action.body(), SwsResponse.class))
                    .map(logAndReturnResult())
                    .orElseThrow();
            })
            .join();
    }
}