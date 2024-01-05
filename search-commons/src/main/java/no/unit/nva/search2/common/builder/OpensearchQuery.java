package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.constant.Words.COMMA;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search2.common.QueryTools;
import no.unit.nva.search2.enums.ParameterKey;
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
 */
public abstract class OpensearchQuery<K extends Enum<K> & ParameterKey> {

    public QueryTools<K> queryTools = new QueryTools<>();

    public Stream<Map.Entry<K, QueryBuilder>> buildQuery(K key, String value) {
        final var values = value.split(COMMA);
        return queryAsEntryStream(key, values);
    }

    protected abstract Stream<Map.Entry<K, QueryBuilder>> queryAsEntryStream(K key, String... values);


}