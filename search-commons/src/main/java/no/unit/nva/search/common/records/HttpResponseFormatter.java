package no.unit.nva.search.common.records;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Defaults.BIBTEX_UTF_8;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.search.common.constant.Functions.hasContent;
import static nva.commons.apigateway.MediaType.CSV_UTF_8;
import static nva.commons.core.paths.UriWrapper.fromUri;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.AggregationFormat;
import no.unit.nva.search.common.QueryKeys;
import no.unit.nva.search.common.bibtex.ResourceBibTexTransformer;
import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import no.unit.nva.search.common.enums.ParameterKey;
import nva.commons.apigateway.MediaType;

/**
 * HttpResponseFormatter is a class that formats a search response.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public final class HttpResponseFormatter<K extends Enum<K> & ParameterKey<K>> {
  private static final String HEADER_LINK = "Link";
  private static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";
  private static final String HEADER_ACCESS_CONTROL_EXPOSE_HEADERS =
      "Access-Control-Expose-Headers";
  private static final String EXPOSED_PAGINATION_HEADERS =
      String.join(", ", HEADER_LINK, HEADER_X_TOTAL_COUNT);
  private static final String REL_FIRST = "first";
  private static final String REL_PREV = "prev";
  private static final String REL_NEXT = "next";
  private static final String REL_LAST = "last";
  private static final String LINK_VALUE_FORMAT = "<%s>; rel=\"%s\"";
  private static final String LINK_HEADER_SEPARATOR = ", ";

  private final SwsResponse response;
  private final MediaType mediaType;
  private final URI source;
  private final Integer offset;
  private final Integer size;
  private final Map<String, String> facetPaths;
  private final QueryKeys<K> queryKeys;
  private List<JsonNodeMutator> mutators;

  public HttpResponseFormatter(
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

  public HttpResponseFormatter(SwsResponse response, MediaType mediaType) {
    this(response, mediaType, null, 0, 0, Map.of(), null);
  }

  public HttpResponseFormatter<K> withMutators(JsonNodeMutator... mutators) {
    this.mutators = List.of(mutators);
    return this;
  }

  public QueryKeys<K> parameters() {
    return queryKeys;
  }

  public SwsResponse swsResponse() {
    return response;
  }

  public List<JsonNode> toMutatedHits() {
    return response.getSearchHits().stream()
        .flatMap(hit -> getMutators().map(mutator -> mutator.transform(hit)))
        .toList();
  }

  public PagedSearch toPagedResponse() {
    final var aggregationFormatted =
        AggregationFormat.apply(response.aggregations(), facetPaths).toString();
    final var hits = toMutatedHits();

    return new PagedSearchBuilder()
        .withTotalHits(response.getTotalSize())
        .withHits(hits)
        .withIds(source, getRequestParameter(), offset, size)
        .withNextResultsBySortKey(nextResultsBySortKey(getRequestParameter(), source))
        .withAggregations(aggregationFormatted)
        .build();
  }

  private Stream<JsonNodeMutator> getMutators() {
    if (this.mutators == null) {
      return Stream.of(defaultMutator());
    }
    return this.mutators.stream();
  }

  private JsonNodeMutator defaultMutator() {
    return source -> source;
  }

  public String toCsvText() {
    return ResourceCsvTransformer.transform(response.getSearchHits());
  }

  public String toBibTexText() {
    return ResourceBibTexTransformer.transform(response.getSearchHits());
  }

  public Map<String, String> paginationHeaders() {
    if (!isPlainTextMediaType()) {
      return Map.of();
    }
    var total = response.getTotalSize();
    var headers = new LinkedHashMap<String, String>();
    headers.put(HEADER_X_TOTAL_COUNT, String.valueOf(total));
    headers.put(HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_PAGINATION_HEADERS);
    buildLinkHeaderValue(total).ifPresent(value -> headers.put(HEADER_LINK, value));
    return headers;
  }

  private boolean isPlainTextMediaType() {
    return nonNull(mediaType) && (CSV_UTF_8.matches(mediaType) || BIBTEX_UTF_8.matches(mediaType));
  }

  private Optional<String> buildLinkHeaderValue(int total) {
    if (!isPaginatable(total)) {
      return Optional.empty();
    }
    var current = nonNull(offset) ? offset : 0;
    var links = new ArrayList<String>();
    links.add(formatLink(uriWithFrom(0), REL_FIRST));
    if (current > 0) {
      links.add(formatLink(uriWithFrom(Math.max(0, current - size)), REL_PREV));
    }
    if (current + size < total) {
      links.add(formatLink(uriWithFrom(current + size), REL_NEXT));
    }
    links.add(formatLink(uriWithFrom(lastPageOffset(total)), REL_LAST));
    return Optional.of(String.join(LINK_HEADER_SEPARATOR, links));
  }

  private boolean isPaginatable(int total) {
    return total > 0 && nonNull(size) && size > 0 && total > size;
  }

  private int lastPageOffset(int total) {
    return (total - 1) / size * size;
  }

  private URI uriWithFrom(int newFrom) {
    var params = getRequestParameter();
    params.put(Words.FROM, String.valueOf(newFrom));
    params.put(Words.SIZE, String.valueOf(size));
    return fromUri(source).addQueryParameters(params).getUri();
  }

  private static String formatLink(URI uri, String rel) {
    return LINK_VALUE_FORMAT.formatted(uri, rel);
  }

  private Map<String, String> getRequestParameter() {
    return nonNull(queryKeys) ? queryKeys.asMap() : new LinkedHashMap<>();
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
    if (CSV_UTF_8.matches(this.mediaType)) {
      return toCsvText();
    }
    if (BIBTEX_UTF_8.matches(this.mediaType)) {
      return toBibTexText();
    }
    return toPagedResponse().toJsonString();
  }
}
