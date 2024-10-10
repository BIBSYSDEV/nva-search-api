package no.unit.nva.search.model.records;

import static no.unit.nva.search.model.constant.Functions.hasContent;

import static java.util.Objects.nonNull;

import com.google.common.net.MediaType;

import no.unit.nva.search.model.AggregationFormat;
import no.unit.nva.search.model.QueryKeys;
import no.unit.nva.search.model.constant.Words;
import no.unit.nva.search.model.csv.ResourceCsvTransformer;
import no.unit.nva.search.model.enums.ParameterKey;

import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HttpResponseFormatter is a class that formats a search response.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public final class HttpResponseFormatter<K extends Enum<K> & ParameterKey<K>> {
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

    public PagedSearch toPagedResponse() {
        final var aggregationFormatted =
                AggregationFormat.apply(response.aggregations(), facetPaths).toString();
        final var hits =
                response.getSearchHits().stream()
                        .flatMap(hit -> getMutators().map(mutator -> mutator.transform(hit)))
                        .toList();

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

    private Map<String, String> getRequestParameter() {
        return queryKeys.asMap();
    }

    private URI nextResultsBySortKey(Map<String, String> requestParameter, URI gatewayUri) {
        requestParameter.remove(Words.FROM);
        var sortParameter =
                response.getSort().stream()
                        .map(value -> nonNull(value) ? value : "null")
                        .collect(Collectors.joining(Words.COMMA));
        if (!hasContent(sortParameter)) {
            return null;
        }
        var searchAfter = Words.SEARCH_AFTER.toLowerCase(Locale.getDefault());
        requestParameter.put(searchAfter, sortParameter);
        return UriWrapper.fromUri(gatewayUri).addQueryParameters(requestParameter).getUri();
    }

    @Override
    public String toString() {
        return MediaType.CSV_UTF_8.is(this.mediaType)
                ? toCsvText()
                : toPagedResponse().toJsonString();
    }
}
