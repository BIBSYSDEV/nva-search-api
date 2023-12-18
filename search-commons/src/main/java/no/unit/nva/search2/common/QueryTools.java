package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.constant.Functions.jsonPath;
import static no.unit.nva.search2.constant.Words.AMPERSAND;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.EQUAL;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.ONE;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.LESS_THAN;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.constant.Defaults;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ParamKind;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.sort.SortOrder;

public final class QueryTools<K extends Enum<K> & ParameterKey> {

    public static Boolean valueToBoolean(String value) {
        return ONE.equals(value) ? Boolean.TRUE : Boolean.valueOf(value);
    }

    public static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static Collection<Entry<String, String>> queryToMapEntries(URI uri) {
        return queryToMapEntries(uri.getQuery());
    }

    public static Collection<Entry<String, String>> queryToMapEntries(String query) {
        return
            nonNull(query)
                ? Arrays.stream(query.split(AMPERSAND))
                .map(s -> s.split(EQUAL))
                .map(QueryTools::stringsToEntry)
                .toList()
                : Collections.emptyList();
    }

    public static Entry<String, String> stringsToEntry(String... strings) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return strings[0];
            }

            @Override
            public String getValue() {
                return attempt(() -> strings[1]).orElse((f) -> EMPTY_STRING);
            }

            @Override
            @JacocoGenerated
            public String setValue(String value) {
                return null;
            }
        };
    }

    public static Entry<String, SortOrder> entryToSortEntry(Entry<String, String> entry) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return entry.getKey();
            }

            @Override
            public SortOrder getValue() {
                var sortOrder = nonNull(entry.getValue()) && !entry.getValue().isEmpty()
                    ? entry.getValue()
                    : Defaults.DEFAULT_SORT_ORDER;
                return SortOrder.fromString(sortOrder);
            }

            @Override
            @JacocoGenerated
            public SortOrder setValue(SortOrder value) {
                return null;
            }
        };
    }

    static URI nextResultsBySortKey(SwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {

        requestParameter.remove(Words.FROM);
        var sortedP =
            response.getSort().stream()
                .map(Object::toString)
                .collect(Collectors.joining(COMMA));
        requestParameter.put(Words.SEARCH_AFTER, sortedP);
        return fromUri(gatewayUri)
            .addQueryParameters(requestParameter)
            .getUri();
    }

    static String[] splitValues(String value) {
        return Arrays.stream(value.split(COMMA))
            .map(String::trim)
            .toArray(String[]::new);
    }

    public Stream<Entry<K, QueryBuilder>> queryToEntry(K key, QueryBuilder qb) {
        final var entry = new Entry<K, QueryBuilder>() {
            @Override
            public K getKey() {
                return key;
            }

            @Override
            public QueryBuilder getValue() {
                return qb;
            }

            @Override
            @JacocoGenerated
            public QueryBuilder setValue(QueryBuilder value) {
                return null;
            }
        };
        return Stream.of(entry);
    }

    Stream<QueryBuilder> buildQuery(K key, String... values) {
        final var searchFields = getSearchFields(key);
        if (hasMultipleFields(searchFields)) {
            return Arrays.stream(values)
                .map(singleValue -> getMultiMatchQueryBuilder(singleValue, searchFields));
        }
        return Arrays.stream(values)
            .map(singleValue -> getMatchQueryBuilder(key, singleValue));
    }

    Stream<Entry<K, QueryBuilder>> rangeQuery(K key, String value) {
        final var searchField = getFirstSearchField(key);

        return queryToEntry(key, switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        });
    }

    Stream<Entry<K, QueryBuilder>> fundingQuery(K key, String value) {
        final var values = value.split(COLON);
        return queryToEntry(
            key,
            QueryBuilders.nestedQuery(
                FUNDINGS,
                QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                    .must(QueryBuilders.termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
                ScoreMode.None));
    }

    boolean isNumber(K key) {
        return key.searchOperator() == GREATER_THAN_OR_EQUAL_TO || key.searchOperator() == LESS_THAN;
    }

    boolean isFundingKey(K key) {
        return Words.FUNDING.equals(key.name());
    }

    boolean isSearchAll(K key) {
        return Words.SEARCH_ALL.equals(key.name());
    }

    private boolean hasMultipleFields(String... keys) {
        return keys.length > 1;
    }

    private MultiMatchQueryBuilder getMultiMatchQueryBuilder(String singleValue, String... searchFields) {
        return QueryBuilders
            .multiMatchQuery(singleValue, searchFields)
            .type(Type.CROSS_FIELDS)
            .operator(Operator.OR);
    }

    private MatchQueryBuilder getMatchQueryBuilder(K key, String singleValue) {
        final var searchField = getFirstSearchField(key);
        return QueryBuilders
            .matchQuery(searchField, singleValue)
            .boost(key.fieldBoost())
            .operator(operatorByKey(key));
    }

    private String[] getSearchFields(K key) {
        return key.searchFields().stream()
            .map(String::trim)
            .map(trimmed -> isNotKeyword(key)
                ? trimmed.replace(DOT + KEYWORD, EMPTY_STRING)
                : trimmed)
            .toArray(String[]::new);
    }

    private boolean isNotKeyword(K key) {
        return !key.fieldType().equals(ParamKind.KEYWORD);
    }

    private Operator operatorByKey(K key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case SHOULD, MUST_NOT -> Operator.OR;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        };
    }

    private String getFirstSearchField(K key) {
        return getSearchFields(key)[0];
    }
}
