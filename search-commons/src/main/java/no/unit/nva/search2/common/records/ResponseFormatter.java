package no.unit.nva.search2.common.records;

import com.google.common.net.MediaType;
import no.unit.nva.search.ResourceCsvTransformer;
import no.unit.nva.search2.common.AggregationFormat;
import no.unit.nva.search2.common.constant.Words;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static nva.commons.core.paths.UriWrapper.fromUri;

public final class ResponseFormatter {
    private final SwsResponse response;
    private final MediaType mediaType;
    private final URI source;
    private final Integer offset;
    private final Integer size;
    private final Map<String, String> facetPaths;
    private final Map<String, String> requestParameter;

    public ResponseFormatter(
        SwsResponse response,
        MediaType mediaType,
        URI source,
        Integer offset,
        Integer size,
        Map<String, String> facetPaths,
        Map<String, String> requestParameter
    ) {
        this.response = response;
        this.mediaType = mediaType;
        this.source = source;
        this.offset = offset;
        this.size = size;
        this.facetPaths = facetPaths;
        this.requestParameter = requestParameter;
    }

    public ResponseFormatter(SwsResponse response, MediaType mediaType) {
        this(response, mediaType, null, 0, 0, Map.of(), Map.of());
    }

    public SwsResponse swsResponse() {
        return response;
    }


    public String toCsvText() {
        return ResourceCsvTransformer.transform(response.getSearchHits());
    }

    public PagedSearch toPagedResponse() {
        final var aggregationFormatted = AggregationFormat.apply(response.aggregations(), facetPaths)
            .toString();

        return
            new PagedSearchBuilder()
                .withTotalHits(response.getTotalSize())
                .withHits(response.getSearchHits())
                .withIds(source, requestParameter, offset, size)
                .withNextResultsBySortKey(nextResultsBySortKey(requestParameter, source))
                .withAggregations(aggregationFormatted)
                .build();
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
        return fromUri(gatewayUri)
            .addQueryParameters(requestParameter)
            .getUri();
    }

    @Override
    public String toString() {
        return CSV_UTF_8.is(this.mediaType)
            ? toCsvText()
            : toPagedResponse().toJsonString();
    }

}
