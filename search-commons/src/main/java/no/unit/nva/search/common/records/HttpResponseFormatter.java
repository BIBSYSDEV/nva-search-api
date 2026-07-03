package no.unit.nva.search.common.records;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Defaults.BIBTEX_UTF_8;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.search.common.constant.Functions.hasContent;
import static nva.commons.apigateway.MediaType.CSV_UTF_8;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.apache.hc.core5.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;

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
import no.unit.nva.search.common.bibliography.SchemaOrgBibliographyTransformer;
import no.unit.nva.search.common.bibtex.ResourceBibTexTransformer;
import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import no.unit.nva.search.common.enums.ParameterKey;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.MediaTypes;

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
  private static final String EXPOSED_PAGINATION_HEADERS =
      String.join(", ", HEADER_LINK, HEADER_X_TOTAL_COUNT);
  private static final String REL_FIRST = "first";
  private static final String REL_NEXT = "next";
  private static final String NULL_SORT_VALUE = "null";
  private static final String LINK_VALUE_FORMAT = "<%s>; rel=\"%s\"";
  private static final String LINK_HEADER_SEPARATOR = ", ";
  private static final String SCHEMA_ORG_PROFILE_LINK = "<https://schema.org>; rel=\"profile\"";

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

  public String toSchemaOrgText() {
    return SchemaOrgBibliographyTransformer.transform(
        response.getSearchHits(), response.getTotalSize());
  }

  public Map<String, String> paginationHeaders() {
    if (!hasPaginationHeaders()) {
      return Map.of();
    }
    var total = response.getTotalSize();
    var headers = new LinkedHashMap<String, String>();
    headers.put(HEADER_X_TOTAL_COUNT, String.valueOf(total));
    headers.put(ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_PAGINATION_HEADERS);

    var profileLink =
        (MediaTypes.APPLICATION_JSON_LD.matches(mediaType)
                || MediaTypes.SCHEMA_ORG.matches(mediaType))
            ? Optional.of(SCHEMA_ORG_PROFILE_LINK)
            : Optional.<String>empty();

    Stream.of(profileLink, buildLinkHeaderValue(total))
        .flatMap(Optional::stream)
        .reduce((left, right) -> left + LINK_HEADER_SEPARATOR + right)
        .ifPresent(value -> headers.put(HEADER_LINK, value));

    return headers;
  }

  private boolean hasPaginationHeaders() {
    return nonNull(mediaType)
        && (CSV_UTF_8.matches(mediaType)
            || BIBTEX_UTF_8.matches(mediaType)
            || MediaTypes.APPLICATION_JSON_LD.matches(mediaType)
            || MediaTypes.SCHEMA_ORG.matches(mediaType));
  }

  private Optional<String> buildLinkHeaderValue(int total) {
    if (!isPaginatable(total)) {
      return Optional.empty();
    }
    var links = new ArrayList<String>();
    links.add(formatLink(firstPageUri(), REL_FIRST));
    nextPageUri().ifPresent(uri -> links.add(formatLink(uri, REL_NEXT)));
    return Optional.of(String.join(LINK_HEADER_SEPARATOR, links));
  }

  private boolean isPaginatable(int total) {
    return total > 0 && nonNull(size) && size > 0 && total > size;
  }

  private URI firstPageUri() {
    var params = getRequestParameter();
    params.remove(Words.FROM);
    params.remove(searchAfterParameterName());
    params.put(Words.SIZE, String.valueOf(size));
    return fromUri(source).addQueryParameters(params).getUri();
  }

  private Optional<URI> nextPageUri() {
    return doesNotHaveFullPage() || doesNotHaveSortValues()
        ? Optional.empty()
        : Optional.of(generateNextPageUri());
  }

  private URI generateNextPageUri() {
    var params = getRequestParameter();
    params.remove(Words.FROM);
    params.put(Words.SIZE, String.valueOf(size));
    params.put(searchAfterParameterName(), sortValuesOfLastHit());
    return fromUri(source).addQueryParameters(params).getUri();
  }

  private boolean doesNotHaveFullPage() {
    return response.getSearchHits().size() < size;
  }

  private boolean doesNotHaveSortValues() {
    return !hasContent(sortValuesOfLastHit());
  }

  private String sortValuesOfLastHit() {
    return response.getSort().stream()
        .map(value -> nonNull(value) ? value : NULL_SORT_VALUE)
        .collect(Collectors.joining(COMMA));
  }

  private static String searchAfterParameterName() {
    return Words.SEARCH_AFTER.toLowerCase(Locale.getDefault());
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
    if (MediaTypes.APPLICATION_JSON_LD.matches(this.mediaType)
        || MediaTypes.SCHEMA_ORG.matches(this.mediaType)) {
      return toSchemaOrgText();
    }
    return toPagedResponse().toJsonString();
  }
}
