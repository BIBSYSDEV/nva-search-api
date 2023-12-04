package no.unit.nva.search2.common;

import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.SPACE;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ParamKind;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;

public final class QueryBuilderTools {

    private static final Integer SINGLE_FIELD = 1;

    public static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static void addKeywordQuery(ParameterKey key, String value, BoolQueryBuilder bq) {
        final var searchFields = key.searchFields()
            .toArray(String[]::new);
        final var values = Arrays.stream(value.split(COMMA))
            .map(String::trim)
            .toArray(String[]::new);
        final var multipleFields = hasMultipleFields(searchFields);

        Arrays.stream(searchFields).forEach(searchField -> {
            final var termsQuery = QueryBuilders.termsQuery(searchField, values).boost(key.fieldBoost());
            switch (key.searchOperator()) {
                case MUST -> {
                    if (multipleFields) {
                        bq.should(termsQuery);
                    } else {
                        bq.must(termsQuery);
                    }
                }
                case MUST_NOT -> bq.mustNot(termsQuery);
                case SHOULD -> bq.should(termsQuery);
                default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            }
        });
    }

    public static QueryBuilder buildQuery(ParameterKey key, String value) {
        final var values = value.replace(COMMA, SPACE);
        final var searchFields =
            key.searchFields().stream()
                .map(String::trim)
                .map(trimmed -> !key.fieldType().equals(ParamKind.KEYWORD)
                    ? trimmed.replace(DOT + KEYWORD, EMPTY_STRING)
                    : trimmed)
                .toArray(String[]::new);
        if (hasMultipleFields(searchFields)) {
            return QueryBuilders
                .multiMatchQuery(values, searchFields)
                .type(Type.BEST_FIELDS)
                .operator(operatorByKey(key));
        }
        var searchField = searchFields[0];
        return QueryBuilders
            .matchQuery(searchField, values)
            .boost(key.fieldBoost())
            .operator(operatorByKey(key));
    }

    public static RangeQueryBuilder rangeQuery(ParameterKey key, String value) {
        final var searchField = key.searchFields().toArray()[0].toString();

        return switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        };
    }

    public static Operator operatorByKey(ParameterKey key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case SHOULD, MUST_NOT -> Operator.OR;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        };
    }

    public static boolean hasMultipleFields(String... swsKeys) {
        return swsKeys.length > SINGLE_FIELD;
    }
}
