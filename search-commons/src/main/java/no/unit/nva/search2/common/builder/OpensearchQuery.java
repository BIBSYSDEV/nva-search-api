package no.unit.nva.search2.common.builder;

import no.unit.nva.search2.common.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;

import java.util.Map;
import java.util.stream.Stream;

import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static nva.commons.core.StringUtils.EMPTY_STRING;

/**
 * Abstract class for building OpenSearch queries.
 * <ul>
 * <li>The query can be built as a single query or as a multi query.</li>
 * <li>One or more values can be added to a query.</li>
 * <li>One or mode fields can be added to a query. </li>
 * <li>SHOULD, MUST_NOT implement OR operator </li>
 * <li>MUST  implement AND operator </li>
 * </ul>
 */
public abstract class OpensearchQuery<K extends Enum<K> & ParameterKey> {

    public Stream<Map.Entry<K, QueryBuilder>> buildQuery(K key, String value) {
        final var values = value.split(COMMA);
        return hasMoreThanOne(values)
            ? multiValueQuery(key, values)
            : valueQuery(key, value);

    }

    protected abstract Stream<Map.Entry<K, QueryBuilder>> valueQuery(K key, String value) ;

    protected abstract Stream<Map.Entry<K, QueryBuilder>> multiValueQuery(K key, String... values) ;

    protected Stream<Map.Entry<K, QueryBuilder>> queryToEntry(K key, QueryBuilder qb) {
        final var entry = new Map.Entry<K, QueryBuilder>() {
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

    protected String getFirstSearchField(K key) {
        return getSearchFields(key)[0];
    }

    protected String[] getSearchFields(K key) {
        return key.searchFields().stream()
            .map(String::trim)
            .map(trimmed -> isNotKeyword(key)
                ? trimmed.replace(DOT + KEYWORD, EMPTY_STRING)
                : trimmed)
            .toArray(String[]::new);
    }

    protected boolean hasMoreThanOne(String... keys) {
        return keys.length > 1;
    }

    protected Operator operatorByKey(K key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case SHOULD, MUST_NOT -> Operator.OR;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        };
    }

    protected boolean isOperatorAnd(K key) {
        return Operator.AND.equals(operatorByKey(key));
    }

    protected boolean isNotKeyword(K key) {
        return !ParameterKey.ParamKind.KEYWORD.equals(key.fieldType());
    }

}
