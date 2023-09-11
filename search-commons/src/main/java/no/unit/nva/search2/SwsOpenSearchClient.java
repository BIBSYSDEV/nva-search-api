package no.unit.nva.search2;

import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.function.Function;

import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureAuthUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class SwsOpenSearchClient {

    private static final Logger logger = LoggerFactory.getLogger(SwsOpenSearchClient.class);
    private static final String REQUESTING_SEARCH_FROM = "SwsOpenSearchClient search from {}";
    private final RawContentRetriever contentRetriever;
    private final String mediaType;

    public SwsOpenSearchClient(RawContentRetriever contentRetriever, String mediaType) {
        this.contentRetriever = contentRetriever;
        this.mediaType = mediaType;
    }

    @JacocoGenerated
    public static SwsOpenSearchClient defaultSwsClient() {
        var uri = URI.create(readSearchInfrastructureAuthUri()).toString();
        var retriever = new AuthorizedBackendUriRetriever(uri,SEARCH_INFRASTRUCTURE_CREDENTIALS);

        return new SwsOpenSearchClient(retriever, APPLICATION_JSON.toString());
    }

    protected OpenSearchSwsResponse doSearch(URI requestUri) {
        logger.info(REQUESTING_SEARCH_FROM, requestUri);
        return
            contentRetriever.fetchResponse(requestUri, mediaType)
                .map(toOpenSearchResponse())
                .orElseThrow();
    }

    @NotNull
    private Function<HttpResponse<String>, OpenSearchSwsResponse> toOpenSearchResponse() {
        return response -> attempt(() -> objectMapperWithEmpty.readValue(response.body(), OpenSearchSwsResponse.class))
                .orElseThrow();
    }

}
