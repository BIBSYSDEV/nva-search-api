package no.unit.nva.search.service.resource;

import static no.unit.nva.search.model.constant.Functions.jsonPath;
import static no.unit.nva.search.model.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.model.constant.Words.AFFILIATIONS;
import static no.unit.nva.search.model.constant.Words.ASTERISK;
import static no.unit.nva.search.model.constant.Words.COLON;
import static no.unit.nva.search.model.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search.model.constant.Words.COUNTRY_CODE;
import static no.unit.nva.search.model.constant.Words.CREATOR;
import static no.unit.nva.search.model.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.model.constant.Words.FUNDINGS;
import static no.unit.nva.search.model.constant.Words.IDENTIFIER;
import static no.unit.nva.search.model.constant.Words.IDENTITY;
import static no.unit.nva.search.model.constant.Words.KEYWORD;
import static no.unit.nva.search.model.constant.Words.NO;
import static no.unit.nva.search.model.constant.Words.NOT_VERIFIED;
import static no.unit.nva.search.model.constant.Words.ROLE;
import static no.unit.nva.search.model.constant.Words.SOURCE;
import static no.unit.nva.search.model.constant.Words.SOURCE_NAME;
import static no.unit.nva.search.model.constant.Words.SPACE;
import static no.unit.nva.search.model.constant.Words.TYPE;
import static no.unit.nva.search.model.constant.Words.VALUE;
import static no.unit.nva.search.model.constant.Words.VERIFICATION_STATUS;
import static no.unit.nva.search.model.constant.Words.VERIFIED;
import static no.unit.nva.search.service.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search.service.resource.ResourceParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search.service.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search.service.resource.ResourceParameter.TITLE;

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.existsQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.multiMatchQuery;
import static org.opensearch.index.query.QueryBuilders.nestedQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;

import no.unit.nva.search.model.QueryKeys;
import no.unit.nva.search.model.builder.FuzzyKeywordQuery;
import no.unit.nva.search.model.constant.Functions;

import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stream builders for resource queries.
 *
 * @author Stig Norland
 */
public class ResourceStreamBuilders {

    private final QueryKeys<ResourceParameter> parameters;

    public ResourceStreamBuilders(QueryKeys<ResourceParameter> parameters) {
        this.parameters = parameters;
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> searchAllWithBoostsQuery(
            Map<String, Float> fields) {
        var sevenValues =
                parameters
                        .get(SEARCH_ALL)
                        .asSplitStream(SPACE)
                        .limit(7)
                        .collect(Collectors.joining(SPACE));
        var fifteenValues =
                parameters
                        .get(SEARCH_ALL)
                        .asSplitStream(SPACE)
                        .limit(15)
                        .collect(Collectors.joining(SPACE));

        var query =
                boolQuery()
                        .queryName(SEARCH_ALL.asCamelCase())
                        .must(
                                multiMatchQuery(sevenValues)
                                        .fields(fields)
                                        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                                        .operator(Operator.AND));

        if (fields.containsKey(Constants.ENTITY_DESCRIPTION_MAIN_TITLE) || fields.containsKey(ASTERISK)) {
            query.should(
                    matchPhrasePrefixQuery(Constants.ENTITY_DESCRIPTION_MAIN_TITLE, fifteenValues)
                            .boost(TITLE.fieldBoost()));
        }
        if (fields.containsKey(Constants.ENTITY_ABSTRACT) || fields.containsKey(ASTERISK)) {
            query.should(
                    matchPhraseQuery(Constants.ENTITY_ABSTRACT, fifteenValues).boost(ABSTRACT.fieldBoost()));
        }
        return Functions.queryToEntry(SEARCH_ALL, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> additionalIdentifierQuery(
            ResourceParameter key, String source) {
        var query =
                nestedQuery(
                        ADDITIONAL_IDENTIFIERS,
                        boolQuery()
                                .must(
                                        termQuery(
                                                Functions.jsonPath(
                                                    ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD),
                                                parameters.get(key).as()))
                                .must(
                                        termQuery(
                                                Functions.jsonPath(
                                                        ADDITIONAL_IDENTIFIERS,
                                                        SOURCE_NAME,
                                                        KEYWORD),
                                                source)),
                        ScoreMode.None);

        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> unIdentifiedNorwegians(
            ResourceParameter key) {
        var query =
                boolQuery()
                        .must(
                                termQuery(
                                        Functions.jsonPath(
                                                ENTITY_DESCRIPTION,
                                                CONTRIBUTORS,
                                                AFFILIATIONS,
                                                COUNTRY_CODE,
                                                KEYWORD),
                                        NO))
                        .must(
                                termQuery(
                                        Functions.jsonPath(
                                                ENTITY_DESCRIPTION,
                                                CONTRIBUTORS,
                                                ROLE,
                                                TYPE,
                                                KEYWORD),
                                        CREATOR))
                        .should(
                                boolQuery()
                                        .mustNot(
                                                existsQuery(
                                                        Functions.jsonPath(
                                                                ENTITY_DESCRIPTION,
                                                                CONTRIBUTORS,
                                                                IDENTITY,
                                                                VERIFICATION_STATUS))))
                        .should(
                                termQuery(
                                        Functions.jsonPath(
                                                ENTITY_DESCRIPTION,
                                                CONTRIBUTORS,
                                                IDENTITY,
                                                VERIFICATION_STATUS,
                                                KEYWORD),
                                        NOT_VERIFIED))
                        .minimumShouldMatch(1);
        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> unIdentifiedContributorOrInstitution(
            ResourceParameter key) {
        var query =
                boolQuery()
                        .must(
                                termQuery(
                                        Functions.jsonPath(
                                                ENTITY_DESCRIPTION,
                                                CONTRIBUTORS,
                                                ROLE,
                                                TYPE,
                                                KEYWORD),
                                        CREATOR))
                        .should(
                                boolQuery()
                                        .mustNot(
                                                termQuery(
                                                        Functions.jsonPath(
                                                                ENTITY_DESCRIPTION,
                                                                CONTRIBUTORS,
                                                                IDENTITY,
                                                                VERIFICATION_STATUS,
                                                                KEYWORD),
                                                        VERIFIED)))
                        .should(
                                boolQuery()
                                        .mustNot(
                                                existsQuery(
                                                        Functions.jsonPath(
                                                                ENTITY_DESCRIPTION,
                                                                CONTRIBUTORS,
                                                                AFFILIATIONS))))
                        .minimumShouldMatch(1);
        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> fundingQuery(ResourceParameter key) {
        var values = parameters.get(key).split(COLON);
        var query =
                nestedQuery(
                        FUNDINGS,
                        boolQuery()
                                .must(termQuery(
                                    Functions.jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                                .must(
                                        termQuery(
                                                Functions.jsonPath(
                                                    FUNDINGS, SOURCE, IDENTIFIER, KEYWORD),
                                                values[0])),
                        ScoreMode.None);
        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> subUnitIncludedQuery(
            ResourceParameter key) {
        var searchKey = shouldSearchSpecifiedInstitutionOnly() ? key : EXCLUDE_SUBUNITS;

        return new FuzzyKeywordQuery<ResourceParameter>()
                .buildQuery(searchKey, parameters.get(key).toString())
                .map(query -> Map.entry(key, query.getValue()));
    }

    private Boolean shouldSearchSpecifiedInstitutionOnly() {
        return parameters.get(EXCLUDE_SUBUNITS).asBoolean();
    }
}
