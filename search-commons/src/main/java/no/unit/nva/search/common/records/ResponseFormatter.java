package no.unit.nva.search.common.records;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static java.util.Objects.nonNull;
import static no.unit.nva.search.common.constant.Functions.hasContent;
import static no.unit.nva.search.common.constant.Words.COMMA;
import static nva.commons.core.paths.UriWrapper.fromUri;

import com.google.common.net.MediaType;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.search.common.AggregationFormat;
import no.unit.nva.search.common.QueryKeys;
import no.unit.nva.search.common.constant.Words;
import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import no.unit.nva.search.common.enums.ParameterKey;

/**
 * @author Stig Norland
 */
public final class ResponseFormatter<K extends Enum<K> & ParameterKey<K>> {
  private final SwsResponse response;
  private final MediaType mediaType;
  private final URI source;
  private final Integer offset;
  private final Integer size;
  private final Map<String, String> facetPaths;
  private final QueryKeys<K> queryKeys;

  public ResponseFormatter(
      SwsResponse response,
      MediaType mediaType,
      URI source,
      Integer offset,
      Integer size,
      Map<String, String> facetPaths,
      QueryKeys<K> requestParameter) {
    this.response = response;
    this.mediaType = mediaType;
    this.source = source;
    this.offset = offset;
    this.size = size;
    this.facetPaths = facetPaths;
    this.queryKeys = requestParameter;
  }

  public ResponseFormatter(SwsResponse response, MediaType mediaType) {
    this(response, mediaType, null, 0, 0, Map.of(), null);
  }

  public QueryKeys<K> parameters() {
    return queryKeys;
  }

  public SwsResponse swsResponse() {
    return response;
  }

  public PagedSearch toPagedResponse() {
    final var aggregationFormatted =
        AggregationFormat.apply(response.aggregations(), facetPaths).toString();

    return new PagedSearchBuilder()
        .withTotalHits(response.getTotalSize())
        .withHits(response.getSearchHits())
        .withIds(source, getRequestParameter(), offset, size)
        .withNextResultsBySortKey(nextResultsBySortKey(getRequestParameter(), source))
        .withAggregations(aggregationFormatted)
        .build();
  }

  public String toCsvText() {
    return ResourceCsvTransformer.transform(response.getSearchHits());
  }

  private Map<String, String> getRequestParameter() {
    return queryKeys.asMap();
  }

  private URI nextResultsBySortKey(Map<String, String> requestParameter, URI gatewayUri) {
    requestParameter.remove(Words.FROM);
    var sortParameter =
        response.getSort().stream()
            .map(value -> nonNull(value) ? value : "null")
            .collect(Collectors.joining(COMMA));
    if (!hasContent(sortParameter)) {
      return null;
    }
    var searchAfter = Words.SEARCH_AFTER.toLowerCase(Locale.getDefault());
    requestParameter.put(searchAfter, sortParameter);
    return fromUri(gatewayUri).addQueryParameters(requestParameter).getUri();
  }

  @Override
  public String toString() {
    return CSV_UTF_8.is(this.mediaType) ? toCsvText() : toPagedResponse().toJsonString();
  }
}
