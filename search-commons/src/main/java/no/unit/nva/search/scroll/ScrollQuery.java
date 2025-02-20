package no.unit.nva.search.scroll;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.core.xcontent.XContentHelper.toXContent;

import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.Query;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.common.records.QueryContentWrapper;
import no.unit.nva.search.common.records.SwsResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.ToXContent;

public final class ScrollQuery extends Query<ScrollParameter> {
  private static final String SEARCH_SCROLL = "_search/scroll";
  private final String ttl;
  private final String scrollId;

  public ScrollQuery(String scrollId, String ttl) {
    super();
    this.ttl = ttl;
    this.scrollId = scrollId;
  }

  public String getTtl() {
    return ttl;
  }

  public String getScrollId() {
    return scrollId;
  }

  public static ScrollQueryBuilder builder() {
    return new ScrollQueryBuilder();
  }

  @Override
  protected URI openSearchUri() {
    return fromUri(infrastructureApiUri).addChild(SEARCH_SCROLL).getUri();
  }

  private ScrollQuery withOpenSearchUri(final URI uri) {
    if (nonNull(uri)) {
      infrastructureApiUri = uri;
    }
    return this;
  }

  @Override
  public Stream<QueryContentWrapper> assemble() {
    var scrollRequest = new SearchScrollRequest(scrollId).scroll(ttl);
    return Stream.of(
        new QueryContentWrapper(scrollRequestToString(scrollRequest), this.openSearchUri()));
  }

  @Override
  public <R, Q extends Query<ScrollParameter>> HttpResponseFormatter<ScrollParameter> doSearch(
      OpenSearchClient<R, Q> queryClient) {
    var response = scrollFetch((ScrollClient) queryClient);
    return new HttpResponseFormatter<>(response, CSV_UTF_8);
  }

  private SwsResponse scrollFetch(ScrollClient scrollClient) {

    return scrollClient.doSearch(this);
  }

  private String scrollRequestToString(SearchScrollRequest request) {
    return attempt(
            () ->
                toXContent(request, XContentType.JSON, ToXContent.EMPTY_PARAMS, true)
                    .utf8ToString())
        .orElseThrow();
  }

  public static class ScrollQueryBuilder {
    private URI openSearchUri;
    private String ttl;
    private String scrollId;

    public ScrollQuery build() {
      return new ScrollQuery(scrollId, ttl).withOpenSearchUri(openSearchUri);
    }

    public ScrollQueryBuilder withDockerHostUri(URI uri) {
      this.openSearchUri = uri;
      return this;
    }

    public ScrollQueryBuilder withScrollId(String scrollId) {
      this.scrollId = scrollId;
      return this;
    }

    public ScrollQueryBuilder withTtl(String ttl) {
      this.ttl = ttl;
      return this;
    }
  }
}
