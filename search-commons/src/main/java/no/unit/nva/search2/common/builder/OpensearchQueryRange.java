package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.common.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ParameterKind;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpensearchQueryRange<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    private static final Logger logger = LoggerFactory.getLogger(OpensearchQueryRange.class);

    @JacocoGenerated    // never used
    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return queryAsEntryStream(key, values);
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return queryAsEntryStream(key, values);
    }

    protected Stream<Entry<K, QueryBuilder>> queryAsEntryStream(K key, String... values) {
        final var searchField = queryTools.getFirstSearchField(key);
        var firstParam = getFirstParam(values, key);
        var secondParam = getSecondParam(values, key);


        logger.info(firstParam + " - " + secondParam);
        return queryTools.queryToEntry(key, switch (key.searchOperator()) {
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders
                .rangeQuery(searchField)
                .gte(firstParam)
                .queryName("GreaterOrEqual-" + key.asCamelCase());
            case LESS_THAN -> QueryBuilders
                .rangeQuery(searchField)
                .lt(firstParam)
                .queryName("LessThan-" + key.asCamelCase());
            case BETWEEN -> QueryBuilders
                .rangeQuery(searchField)
                .from(firstParam, true)
                .to(secondParam, true)
                .queryName("Between-" + key.asCamelCase());
            default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        });
    }

    private String getSecondParam(String[] values, K key) {
        return values.length == 1 ? null :
            valueOrNull(values[1])
                .map(date -> expandDateLast(date, key))
                .findFirst()
                .orElse(null);
    }

    private String getFirstParam(String[] values, K key) {
        return
            valueOrNull(values[0])
                .map(date -> expandDateFirst(date, key))
                .findFirst()
                .orElse(null);
    }

    private Stream<String> valueOrNull(String value) {
        return Stream.ofNullable(value.isBlank() ? null : value);
    }

    private String expandDateFirst(String date, K key) {
        var retval = date;
        if (key.fieldType() != ParameterKind.DATE) {
            return retval;
        }
        if (retval.length() == 4) {
            retval += "-01";
        }
        if (retval.length() == 7) {
            retval += "-01";
        }
        return retval;
    }

    private String expandDateLast(String date, K key) {
        var retval = date;
        if (key.fieldType() != ParameterKind.DATE) {
            return retval;
        }
        if (retval.length() == 4) {
            retval += "-12";
        }
        if (retval.length() == 7) {
            retval += "-31";
        }
        return retval;
    }
}
