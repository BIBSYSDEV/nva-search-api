package no.unit.nva.search2.scroll;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.common.xcontent.XContentHelper.toXContent;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.records.ResponseFormatter;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import no.unit.nva.search2.common.records.SwsResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.common.xcontent.XContentType;

/**
 * @author Sondre Vestad
 */
public final class ScrollQuery extends Query<ScrollParameters> {

    private static final int MAX_PAGES = 4;
    private static final String SEARCH_SCROLL = "_search/scroll";
    private final String ttl;
    private String scrollId;
    private SwsResponse firstResponse;

    public ScrollQuery(String scrollId, String ttl) {
        super();
        this.ttl = ttl;
        this.scrollId = scrollId;
    }

    ScrollQuery(String ttl, SwsResponse firstResponse) {
        this(firstResponse._scroll_id(), ttl);
        this.firstResponse = firstResponse;
    }

    public static ScrollBuilder builder() {
        return new ScrollBuilder();
    }

    @Override
    protected URI openSearchUri() {
        return
            fromUri(infrastructureApiUri)
                .addChild(SEARCH_SCROLL)
                .getUri();
    }

    private ScrollQuery withOpenSearchUri(final URI uri) {
        infrastructureApiUri = uri;
        return this;
    }

    @Override
    public Stream<QueryContentWrapper> assemble() {
        var scrollRequest = new SearchScrollRequest(scrollId).scroll(ttl);
        return Stream.of(new QueryContentWrapper(scrollRequestToString(scrollRequest), this.openSearchUri()));
    }

    @Override
    public <R, Q extends Query<ScrollParameters>> ResponseFormatter<ScrollParameters> doSearch(OpenSearchClient<R, Q> queryClient) {
        var response =
            buildSwsResponse(
                scrollFetch(firstResponse, 0, (ScrollClient) queryClient)
            );
        return new ResponseFormatter<>(response, CSV_UTF_8);
    }

    private Stream<JsonNode> scrollFetch(SwsResponse previousResponse, int level, ScrollClient scrollClient) {

        if (shouldStopRecursion(level + 1, previousResponse)) {
            return previousResponse.getSearchHits().stream();
        }
        scrollId = previousResponse._scroll_id();
        var currentResponse = scrollClient.doSearch(this);

        return Stream.concat(
            previousResponse.getSearchHits().stream(),
            scrollFetch(currentResponse, level + 1, scrollClient)
        );
    }

    private SwsResponse buildSwsResponse(Stream<JsonNode> results) {
        var hits = results
            .map(hit -> new SwsResponse.HitsInfo.Hit(null, null, null, 0, hit, null))
            .toList();
        return new SwsResponse(
            0,
            false,
            null,
            new SwsResponse.HitsInfo(null, 0, hits),
            null,
            "");
    }

    private boolean shouldStopRecursion(Integer level, SwsResponse previousResponse) {
        return Objects.isNull(previousResponse._scroll_id())
            || previousResponse.getSearchHits().isEmpty()
            || level >= MAX_PAGES;
    }

    private String scrollRequestToString(SearchScrollRequest request) {
        return attempt(() -> toXContent(request, XContentType.JSON, true).utf8ToString())
            .orElseThrow();
    }

    public static class ScrollBuilder {
        private SwsResponse firstResponse;
        private URI openSearchUri;
        private String ttl;

        public ScrollQuery build() {
            return nonNull(openSearchUri)
                ? new ScrollQuery(ttl, firstResponse).withOpenSearchUri(openSearchUri)
                : new ScrollQuery(ttl, firstResponse);
        }

        public ScrollBuilder withDockerHostUri(URI uri) {
            this.openSearchUri = uri;
            return this;
        }

        public ScrollBuilder withInitialResponse(SwsResponse initialResponse) {
            this.firstResponse = initialResponse;
            return this;
        }

        public ScrollBuilder withScrollTime(String scrollTtl) {
            this.ttl = scrollTtl;
            return this;
        }
    }
}