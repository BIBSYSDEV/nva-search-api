package no.unit.nva.indexing.testutils;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.ContextParser;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.aggregations.bucket.terms.StringTerms;
import org.opensearch.search.aggregations.metrics.ParsedTopHits;
import org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opensearch.common.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS;

public final class SearchResponseUtil {

    private SearchResponseUtil() {

    }

    private static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(TopHitsAggregationBuilder.NAME, (p, c) -> ParsedTopHits.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
        List<NamedXContentRegistry.Entry> entries = map.entrySet().stream()
                .map(entry -> new NamedXContentRegistry.Entry(
                        Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
        return entries;
    }

    public static SearchResponse getSearchResponseFromJson(String jsonResponse) throws IOException {
        NamedXContentRegistry registry = new NamedXContentRegistry(getDefaultNamedXContents());
        try (XContentParser parser = JsonXContent.jsonXContent.createParser(
                registry, IGNORE_DEPRECATIONS, jsonResponse)) {
            return SearchResponse.fromXContent(parser);
        }
    }
}
