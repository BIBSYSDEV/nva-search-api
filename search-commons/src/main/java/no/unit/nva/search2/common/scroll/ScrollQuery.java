package no.unit.nva.search2.common.scroll;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.common.xcontent.ToXContent.EMPTY_PARAMS;
import static org.opensearch.common.xcontent.XContentHelper.toXContent;
import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import no.unit.nva.search2.common.records.SwsResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.common.xcontent.XContentType;

@SuppressWarnings("PMD.GodClass")
public final class ScrollQuery extends Query<ScrollParameters> {

    private final String scrollId;
    private final String ttl;

    public ScrollQuery(String scrollId, String ttl) {
        super();
        this.scrollId = scrollId;
        this.ttl = ttl;
    }

    @Override
    protected URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild("_search/scroll")
                .getUri();
    }

    public <R, Q extends ScrollQuery> SwsResponse doSearchRaw(OpenSearchClient<R, Q> queryClient) {
        return (SwsResponse) queryClient.doSearch((Q) this);
    }

    @Override
    public Stream<QueryContentWrapper> assemble() {
        var scrollRequest  = new SearchScrollRequest(scrollId).scroll(ttl);
        return Stream.of(new QueryContentWrapper(scrollRequestToString(scrollRequest), this.getOpenSearchUri()));
    }

    private static String scrollRequestToString(SearchScrollRequest request)  {
        return attempt(() -> toXContent(request, XContentType.JSON, EMPTY_PARAMS, true).utf8ToString())
                   .orElseThrow();
    }
}