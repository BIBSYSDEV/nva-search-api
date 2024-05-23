package no.unit.nva.search2.resource;

import no.unit.nva.search2.common.QueryKeys;

import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.FUNDINGS;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_TRUE;
import static no.unit.nva.search2.common.constant.Words.SOURCE;
import static no.unit.nva.search2.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search2.common.constant.Words.SPACE;
import static no.unit.nva.search2.common.constant.Words.VALUE;
import static no.unit.nva.search2.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search2.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search2.resource.ResourceParameter.EXCLUDE_SUBUNITS;

import no.unit.nva.search2.common.builder.OpensearchQueryFuzzyKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryKeyword;
import no.unit.nva.search2.common.constant.Functions;
import no.unit.nva.search2.ticket.TicketParameter;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import static no.unit.nva.search2.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search2.resource.ResourceParameter.TITLE;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceStreamBuilders {

    private final QueryKeys<ResourceParameter> parameters;


    public ResourceStreamBuilders(QueryKeys<ResourceParameter> parameters) {
        this.parameters = parameters;
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> searchAllWithBoostsQuery(Map<String, Float> fields) {
        var sevenValues = parameters.get(SEARCH_ALL).asSplitStream(SPACE)
            .limit(7)
            .collect(Collectors.joining(SPACE));
        var fifteenValues = parameters.get(SEARCH_ALL).asSplitStream(SPACE)
            .limit(15)
            .collect(Collectors.joining(SPACE));

        var query = boolQuery()
            .queryName(SEARCH_ALL.asCamelCase())
            .must(QueryBuilders.multiMatchQuery(sevenValues)
                .fields(fields)
                .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                .operator(Operator.AND));

        if (fields.containsKey(ENTITY_DESCRIPTION_MAIN_TITLE) || fields.containsKey(ASTERISK)) {
            query.should(
                matchPhrasePrefixQuery(ENTITY_DESCRIPTION_MAIN_TITLE, fifteenValues).boost(TITLE.fieldBoost())
            );
        }
        if (fields.containsKey(ENTITY_ABSTRACT) || fields.containsKey(ASTERISK)) {
            query.should(matchPhraseQuery(ENTITY_ABSTRACT, fifteenValues).boost(ABSTRACT.fieldBoost()));
        }
        return Functions.queryToEntry(SEARCH_ALL, query);
    }


    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> additionalIdentifierQuery(
        ResourceParameter key, String source) {
        var query = QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), parameters.get(key).as()))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> fundingQuery(ResourceParameter key) {
        var values = parameters.get(key).split(COLON);
        var query = QueryBuilders.nestedQuery(
            FUNDINGS,
            boolQuery()
                .must(termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                .must(termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
            ScoreMode.None);
        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> subUnitIncludedQuery(ResourceParameter key) {
        var searchKey = parameters.get(ResourceParameter.EXCLUDE_SUBUNITS).asBoolean()
            ? ResourceParameter.EXCLUDE_SUBUNITS
            : key;

        return
            new OpensearchQueryFuzzyKeyword<ResourceParameter>().buildQuery(searchKey, parameters.get(key).as());
    }

//    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> subUnitIncludedQuery(ResourceParameter key) {
//        var query =
//            parameters.get(EXCLUDE_SUBUNITS).asBoolean()
//                ? termQuery(extractTermPath(key), parameters.get(key).as())
//                : termQuery(extractMatchPath(key), parameters.get(key).as());
//
//        return Functions.queryToEntry(key, query);
//    }
//
//    private String extractTermPath(ResourceParameter key) {
//        return key.searchFields(KEYWORD_TRUE).findFirst().orElseThrow();
//    }
//
//    private String extractMatchPath(ResourceParameter key) {
//        return key.searchFields(KEYWORD_TRUE).skip(1).findFirst().orElseThrow();
//    }

}
