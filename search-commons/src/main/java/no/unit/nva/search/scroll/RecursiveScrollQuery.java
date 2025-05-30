package no.unit.nva.search.scroll;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.core.xcontent.XContentHelper.toXContent;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;
import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.Query;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.common.records.QueryContentWrapper;
import no.unit.nva.search.common.records.SwsResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.ToXContent;

public final class RecursiveScrollQuery extends Query<ScrollParameter> {

  private static final int MAX_PAGES = 4;
  private static final String SEARCH_SCROLL = "_search/scroll";
  private final String ttl;
  private String scrollId;
  private SwsResponse firstResponse;

  public RecursiveScrollQuery(String scrollId, String ttl) {
    super();
    this.ttl = ttl;
    this.scrollId = scrollId;
  }

  RecursiveScrollQuery(String ttl, SwsResponse firstResponse) {
    this(firstResponse._scroll_id(), ttl);
    this.firstResponse = firstResponse;
  }

  public static RecursiveScrollQueryBuilder builder() {
    return new RecursiveScrollQueryBuilder();
  }

  @Override
  protected URI openSearchUri(String indexName) {
    return fromUri(infrastructureApiUri).addChild(SEARCH_SCROLL).getUri();
  }

  private RecursiveScrollQuery withOpenSearchUri(final URI uri) {
    if (nonNull(uri)) {
      infrastructureApiUri = uri;
    }
    return this;
  }

  @Override
  public Stream<QueryContentWrapper> assemble(String indexName) {
    var scrollRequest = new SearchScrollRequest(scrollId).scroll(ttl);
    return Stream.of(
        new QueryContentWrapper(
            scrollRequestToString(scrollRequest), this.openSearchUri(indexName)));
  }

  @Override
  public <R, Q extends Query<ScrollParameter>> HttpResponseFormatter<ScrollParameter> doSearch(
      OpenSearchClient<R, Q> queryClient, String indexName) {
    var response =
        buildSwsResponse(scrollFetch(firstResponse, 0, (ScrollClient) queryClient, indexName));
    return new HttpResponseFormatter<>(response, CSV_UTF_8);
  }

  private Stream<JsonNode> scrollFetch(
      SwsResponse previousResponse, int level, ScrollClient scrollClient, String indexName) {

    if (shouldStopRecursion(level + 1, previousResponse)) {
      return previousResponse.getSearchHits().stream();
    }
    scrollId = previousResponse._scroll_id();
    var currentResponse = scrollClient.doSearch(this, indexName);

    return Stream.concat(
        previousResponse.getSearchHits().stream(),
        scrollFetch(currentResponse, level + 1, scrollClient, indexName));
  }

  private SwsResponse buildSwsResponse(Stream<JsonNode> results) {
    var hits =
        results
            .map(hit -> new SwsResponse.HitsInfo.Hit(null, null, null, 0, hit, null, null))
            .toList();
    return new SwsResponse(0, false, null, new SwsResponse.HitsInfo(null, 0, hits), null, "");
  }

  private boolean shouldStopRecursion(Integer level, SwsResponse previousResponse) {
    return Objects.isNull(previousResponse._scroll_id())
        || previousResponse.getSearchHits().isEmpty()
        || level >= MAX_PAGES;
  }

  private String scrollRequestToString(SearchScrollRequest request) {
    return attempt(
            () ->
                toXContent(request, XContentType.JSON, ToXContent.EMPTY_PARAMS, true)
                    .utf8ToString())
        .orElseThrow();
  }

  public static class RecursiveScrollQueryBuilder {
    private SwsResponse firstResponse;
    private URI openSearchUri;
    private String ttl;

    public RecursiveScrollQuery build() {
      return new RecursiveScrollQuery(ttl, firstResponse).withOpenSearchUri(openSearchUri);
    }

    public RecursiveScrollQueryBuilder withDockerHostUri(URI uri) {
      this.openSearchUri = uri;
      return this;
    }

    public RecursiveScrollQueryBuilder withInitialResponse(SwsResponse initialResponse) {
      this.firstResponse = initialResponse;
      return this;
    }

    public RecursiveScrollQueryBuilder withScrollTime(String scrollTtl) {
      this.ttl = scrollTtl;
      return this;
    }
  }
}
