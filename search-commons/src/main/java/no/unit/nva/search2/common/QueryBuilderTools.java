package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.queryToEntry;
import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.LESS_THAN;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ParamKind;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public final class QueryBuilderTools {

    public static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static <K extends Enum<K> & ParameterKey> Stream<QueryBuilder> buildQuery(K key, String[] values) {
        final var searchFields = getSearchFields(key);
        if (hasMultipleFields(searchFields)) {
            return Arrays.stream(values)
                .map(singleValue -> getMultiMatchQueryBuilder(singleValue, searchFields));
        }
        return Arrays.stream(values)
            .map(singleValue -> getMatchQueryBuilder(key, singleValue));

    }

    public static <K extends Enum<K> & ParameterKey> Stream<Entry<ParameterKey, QueryBuilder>> rangeQuery(K key,
                                                                                                          String value) {
        final var searchField = getFirstSearchField(key);

        return queryToEntry(key, switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        });
    }

    public static <K extends Enum<K> & ParameterKey> Stream<Entry<ParameterKey, QueryBuilder>> fundingQuery(K key,
                                                                                                            String value) {
        final var values = value.split(COLON);
        return queryToEntry(key,
                            QueryBuilders.nestedQuery(
                                "fundings",
                                QueryBuilders.boolQuery()
                                    .must(QueryBuilders.termQuery("fundings.identifier", values[1]))
                                    .must(QueryBuilders.termQuery("fundings.source.identifier", values[0])),
                                ScoreMode.None));
    }

    static <K extends Enum<K> & ParameterKey> boolean isNumber(K key) {
        return key.searchOperator() == GREATER_THAN_OR_EQUAL_TO || key.searchOperator() == LESS_THAN;
    }

    static <K extends Enum<K> & ParameterKey> boolean isFundingKey(K key) {
        return Words.FUNDING.equals(key.name());
    }

    static <K extends Enum<K> & ParameterKey> boolean isSearchAll(K key) {
        return Words.SEARCH_ALL.equals(key.name());
    }

    private static <K extends Enum<K> & ParameterKey> MatchQueryBuilder getMatchQueryBuilder(K key,
                                                                                             String singleValue) {
        final var searchField = getFirstSearchField(key);
        return QueryBuilders
            .matchQuery(searchField, singleValue)
            .boost(key.fieldBoost())
            .operator(operatorByKey(key));
    }

    private static MultiMatchQueryBuilder getMultiMatchQueryBuilder(
        String singleValue, String[] searchFields) {
        return QueryBuilders
            .multiMatchQuery(singleValue, searchFields)
            .type(Type.CROSS_FIELDS)
            .operator(Operator.OR);
    }

    static String mergeWithColonOrComma(String oldValue, String newValue) {
        if (nonNull(oldValue)) {
            var delimiter = newValue.matches(PATTERN_IS_ASC_DESC_VALUE) ? COLON : COMMA;
            return String.join(delimiter, oldValue, newValue);
        } else {
            return newValue;
        }
    }

    private static <K extends Enum<K> & ParameterKey> String[] getSearchFields(K key) {
        return key.searchFields().stream()
            .map(String::trim)
            .map(trimmed -> isNotKeyword(key)
                ? trimmed.replace(DOT + KEYWORD, EMPTY_STRING)
                : trimmed)
            .toArray(String[]::new);
    }

    private static <K extends Enum<K> & ParameterKey> boolean isNotKeyword(K key) {
        return !key.fieldType().equals(ParamKind.KEYWORD);
    }

    private static <K extends Enum<K> & ParameterKey> Operator operatorByKey(K key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case SHOULD, MUST_NOT -> Operator.OR;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        };
    }

    public static boolean hasMultipleFields(String... keys) {
        return keys.length > 1;
    }

    private static <K extends Enum<K> & ParameterKey> String getFirstSearchField(K key) {
        return getSearchFields(key)[0];
    }
}
