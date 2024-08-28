package no.unit.nva.search.common.builder;

import static no.unit.nva.search.common.constant.Words.COMMA;
import static no.unit.nva.search.common.enums.FieldOperator.ANY_OF;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ANY_OF;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.QueryBuilder;

/**
 * Abstract class for building OpenSearch queries.
 * <ul>
 * <li>The query can be built as a single query or as a multi query.</li>
 * <li>One or more values can be added to a query.</li>
 * <li>One or mode fields can be added to a query. </li>
 * <li>SHOULD, MUST_NOT implement OR operator </li>
 * <li>MUST  implement AND operator </li>
 * </ul>
 * @author Stig Norland
 */
public abstract class AbstractBuilder<K extends Enum<K> & ParameterKey<K>> {

    protected abstract Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values);

    protected abstract Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values);

    public Stream<Map.Entry<K, QueryBuilder>> buildQuery(K key, String value) {
        final var values = splitAndFixMissingRangeValue(key, value);
        return buildQuery(key, values);
    }

    public Stream<Map.Entry<K, QueryBuilder>> buildQuery(K key, String... values) {
        return isSearchAny(key)
            ? buildMatchAnyKeyValuesQuery(key, values)
            : buildMatchAllValuesQuery(key, values);
    }


    private boolean isSearchAny(K key) {
        return key.searchOperator().equals(ANY_OF) || key.searchOperator().equals(NOT_ANY_OF);
    }

    private boolean isRangeMissingComma(K key, String value) {
        return key.searchOperator().equals(BETWEEN) && !value.contains(COMMA);
    }

    private String[] splitAndFixMissingRangeValue(K key, String value) {
        return isRangeMissingComma(key, value)
            ? new String[] {value, value}
            : value.split(COMMA);
    }
}