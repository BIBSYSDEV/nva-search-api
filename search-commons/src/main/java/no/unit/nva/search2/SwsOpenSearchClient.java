package no.unit.nva.search2;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search2.model.GatewayResponse;
import no.unit.nva.search2.model.SwsOpenSearchResponse;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Function;

import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureAuthUri;
import static nva.commons.core.attempt.Try.attempt;

public class SwsOpenSearchClient {

    private final RawContentRetriever contentRetriever;
    private final String mediaType;

    public SwsOpenSearchClient(RawContentRetriever contentRetriever, String mediaType) {
        this.contentRetriever = contentRetriever;
        this.mediaType = mediaType;
    }

    @JacocoGenerated
    public static SwsOpenSearchClient defaultSwsClient() {
        var uri = URI.create(readSearchInfrastructureAuthUri()).toString();
        var retriver = new AuthorizedBackendUriRetriever(uri,SEARCH_INFRASTRUCTURE_CREDENTIALS);

        return new SwsOpenSearchClient(retriver, "application/json");
    }

    protected GatewayResponse<SwsOpenSearchResponse> doSearch(URI requestUri) {
        return
            contentRetriever.fetchResponse(requestUri, mediaType)
                .map(toOpenSearchResponse())
                .orElseThrow();
    }

    @NotNull
    private Function<HttpResponse<String>, GatewayResponse<SwsOpenSearchResponse>>
        toOpenSearchResponse() {
        return response -> {
            var openSearchResponseDto =
                attempt(() -> objectMapperWithEmpty.readValue(response.body(), SwsOpenSearchResponse.class))
                    .orElseThrow();
            var statusCode = response.statusCode();
            var headers =
                response.headers().map().entrySet().stream()
                    .map(SwsOpenSearchClient::mapListToString)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return new GatewayResponse<>(openSearchResponseDto, statusCode, headers);
        };
    }

    @NotNull
    private static Map.Entry<String, String> mapListToString(Entry<String, List<String>> entry) {
        return Map.entry(entry.getKey(), String.join(";", entry.getValue()));
    }

}
