package no.unit.nva.search.common.builder;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.ExistsQueryBuilder;
import org.opensearch.index.query.QueryBuilder;

import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static no.unit.nva.search.common.constant.Words.KEYWORD_FALSE;
import static org.opensearch.index.query.QueryBuilders.existsQuery;

/**
 * @author Stig Norland
 */
public class OpensearchQueryExists<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    public static final String EXISTS_ANY = "ExistsAny-";

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, Boolean.valueOf(values[0]))
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, Boolean.valueOf(values[0]))
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private BoolQueryBuilder buildAnyComboMustHitQuery(K key, Boolean value) {
        return key.searchFields(KEYWORD_FALSE).map(fieldName -> existsQuery(fieldName)
                .queryName(EXISTS_ANY)
                .boost(key.fieldBoost()))
            .collect(BoolQueryBuilder::new,
                new BiConsumer<>() {
                    @Override
                    public void accept(BoolQueryBuilder boolQueryBuilder, ExistsQueryBuilder queryBuilder) {
                        if (value) {
                            boolQueryBuilder.must(queryBuilder);
                        } else {
                            boolQueryBuilder.mustNot(queryBuilder);
                        }
                    }
                },
                new BiConsumer<>() {
                    @Override
                    public void accept(BoolQueryBuilder boolQueryBuilder, BoolQueryBuilder boolQueryBuilder2) {
                        if (value) {
                            boolQueryBuilder.must(boolQueryBuilder2);
                        } else {
                            boolQueryBuilder.mustNot(boolQueryBuilder2);
                        }

                    }
                });

    }


}