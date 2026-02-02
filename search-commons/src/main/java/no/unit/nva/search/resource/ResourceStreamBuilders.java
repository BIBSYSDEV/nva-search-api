package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.ASTERISK;
import static no.unit.nva.constants.Words.COLON;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.CONTRIBUTORS;
import static no.unit.nva.constants.Words.COUNTRY_CODE;
import static no.unit.nva.constants.Words.CREATOR;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.constants.Words.FUNDINGS;
import static no.unit.nva.constants.Words.IDENTIFIER;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.KEYWORD;
import static no.unit.nva.constants.Words.NO;
import static no.unit.nva.constants.Words.NOT_VERIFIED;
import static no.unit.nva.constants.Words.ROLE;
import static no.unit.nva.constants.Words.SOURCE;
import static no.unit.nva.constants.Words.SPACE;
import static no.unit.nva.constants.Words.TYPE;
import static no.unit.nva.constants.Words.VALUE;
import static no.unit.nva.constants.Words.VERIFICATION_STATUS;
import static no.unit.nva.constants.Words.VERIFIED;
import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static no.unit.nva.search.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search.resource.Constants.ENTITY_CONTRIBUTORS;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search.resource.Constants.PARENT_PUBLICATION_TYPE;
import static no.unit.nva.search.resource.Constants.PARENT_SCIENTIFIC_PUBLISHER;
import static no.unit.nva.search.resource.Constants.PARENT_SCIENTIFIC_SERIES;
import static no.unit.nva.search.resource.Constants.PUBLICATION_CONTEXT_TYPE_KEYWORD;
import static no.unit.nva.search.resource.Constants.REFERENCE_PUBLICATION_CONTEXT_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_OTHER;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_PUBLISHER;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_SERIES;
import static no.unit.nva.search.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search.resource.ResourceParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search.resource.ResourceParameter.TITLE;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.existsQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.multiMatchQuery;
import static org.opensearch.index.query.QueryBuilders.nestedQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import static org.opensearch.index.query.QueryBuilders.termsQuery;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.common.QueryKeys;
import no.unit.nva.search.common.builder.FuzzyKeywordQuery;
import no.unit.nva.search.common.constant.Functions;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;

/**
 * Stream builders for resource queries.
 *
 * @author Stig Norland
 */
public class ResourceStreamBuilders {

  public static final String COUNTRY_CODE_PATH =
      jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, COUNTRY_CODE, KEYWORD);
  public static final String CONTRIBUTOR_ROLE_PATH =
      jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, ROLE, TYPE, KEYWORD);
  public static final String VERIFICATION_STATUS_PATH =
      jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, VERIFICATION_STATUS);
  public static final String VERIFICATION_STATUS_KEYWORD =
      jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, VERIFICATION_STATUS, KEYWORD);
  public static final String ADDITIONAL_IDENTIFIERS_VALUE_PATH =
      jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD);
  public static final String ADDITIONAL_IDENTIFIERS_TYPE_PATH =
      jsonPath(ADDITIONAL_IDENTIFIERS, TYPE, KEYWORD);
  public static final String FUNDING_SOURCE_IDENTIFIER =
      jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD);
  public static final String CONTRIBUTOR_AFFILIATIONS =
      jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS);
  public static final String FUNDINGS_IDENTIFIER = jsonPath(FUNDINGS, IDENTIFIER, KEYWORD);
  private final QueryKeys<ResourceParameter> parameters;

  public ResourceStreamBuilders(QueryKeys<ResourceParameter> parameters) {
    this.parameters = parameters;
  }

  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> searchAllWithBoostsQuery(
      Map<String, Float> fields) {
    var sevenValues =
        parameters.get(SEARCH_ALL).asSplitStream(SPACE).limit(7).collect(Collectors.joining(SPACE));
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

    if (fields.containsKey(ENTITY_DESCRIPTION_MAIN_TITLE) || fields.containsKey(ASTERISK)) {
      query.should(
          matchPhrasePrefixQuery(ENTITY_DESCRIPTION_MAIN_TITLE, fifteenValues)
              .boost(TITLE.fieldBoost()));
    }
    if (fields.containsKey(ENTITY_ABSTRACT) || fields.containsKey(ASTERISK)) {
      query.should(matchPhraseQuery(ENTITY_ABSTRACT, fifteenValues).boost(ABSTRACT.fieldBoost()));
    }
    return Functions.queryToEntry(SEARCH_ALL, query);
  }

  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> additionalIdentifierQuery(
      ResourceParameter key, String type) {
    String value = parameters.get(key).as();
    var query =
        nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(ADDITIONAL_IDENTIFIERS_VALUE_PATH, value))
                .must(termQuery(ADDITIONAL_IDENTIFIERS_TYPE_PATH, type)),
            ScoreMode.None);

    return Functions.queryToEntry(key, query);
  }

  @JacocoGenerated
  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> unIdentifiedNorwegians(
      ResourceParameter key) {
    var query =
        nestedQuery(
            ENTITY_CONTRIBUTORS,
            boolQuery()
                .must(termQuery(COUNTRY_CODE_PATH, NO))
                .must(termQuery(CONTRIBUTOR_ROLE_PATH, CREATOR))
                .should(boolQuery().mustNot(existsQuery(VERIFICATION_STATUS_PATH)))
                .should(termQuery(VERIFICATION_STATUS_KEYWORD, NOT_VERIFIED))
                .minimumShouldMatch(1),
            ScoreMode.None);
    return Functions.queryToEntry(key, query);
  }

  @JacocoGenerated
  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> unIdentifiedContributorOrInstitution(
      ResourceParameter key) {
    var query =
        boolQuery()
            .must(termQuery(CONTRIBUTOR_ROLE_PATH, CREATOR))
            .should(boolQuery().mustNot(termQuery(VERIFICATION_STATUS_KEYWORD, VERIFIED)))
            .should(boolQuery().mustNot(existsQuery(CONTRIBUTOR_AFFILIATIONS)))
            .minimumShouldMatch(1);
    return Functions.queryToEntry(key, query);
  }

  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> fundingQuery(ResourceParameter key) {
    var values = parameters.get(key).split(COLON);
    var query =
        nestedQuery(
            FUNDINGS,
            boolQuery()
                .must(termQuery(FUNDINGS_IDENTIFIER, values[1]))
                .must(termQuery(FUNDING_SOURCE_IDENTIFIER, values[0])),
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

  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> scientificValueQuery(
      ResourceParameter key) {

    var values = parameters.get(key).split(COMMA);
    var scientificValuesBaseQuery =
        boolQuery()
            .should(
                boolQuery()
                    .must(existsQuery(SCIENTIFIC_SERIES))
                    .must(termsQuery(SCIENTIFIC_SERIES, values)))
            .should(
                boolQuery()
                    .mustNot(existsQuery(SCIENTIFIC_SERIES))
                    .must(termsQuery(SCIENTIFIC_PUBLISHER, values)))
            .should(termsQuery(SCIENTIFIC_OTHER, values));

    var scientificValuesParentQuery =
        boolQuery()
            .should(
                boolQuery()
                    .mustNot(existsQuery(PARENT_SCIENTIFIC_SERIES))
                    .must(termsQuery(PARENT_SCIENTIFIC_PUBLISHER, values)))
            .should(
                boolQuery()
                    .must(existsQuery(PARENT_SCIENTIFIC_SERIES))
                    .must(termsQuery(PARENT_SCIENTIFIC_SERIES, values)))
            .minimumShouldMatch(1);

    var combinedQuery =
        boolQuery()
            .should(scientificValuesBaseQuery)
            .should(scientificValuesParentQuery)
            .minimumShouldMatch(1);

    return Functions.queryToEntry(key, combinedQuery);
  }

  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> allScientificValuesQuery(
      ResourceParameter key) {
    var values = parameters.get(key).split(COMMA);

    var seriesExistsWithValuesOrMissingQuery =
        boolQuery()
            .should(
                boolQuery()
                    .must(existsQuery(SCIENTIFIC_SERIES))
                    .must(termsQuery(SCIENTIFIC_SERIES, values)))
            .should(boolQuery().mustNot(existsQuery(SCIENTIFIC_SERIES)))
            .minimumShouldMatch(1);

    var publisherExistsWithValuesOrMissingQuery =
        boolQuery()
            .should(
                boolQuery()
                    .must(existsQuery(SCIENTIFIC_PUBLISHER))
                    .must(termsQuery(SCIENTIFIC_PUBLISHER, values)))
            .should(boolQuery().mustNot(existsQuery(SCIENTIFIC_PUBLISHER)))
            .minimumShouldMatch(1);

    var journalExistsWithValuesOrMissingQuery =
        boolQuery()
            .should(
                boolQuery()
                    .must(existsQuery(SCIENTIFIC_OTHER))
                    .must(termsQuery(SCIENTIFIC_OTHER, values)))
            .should(boolQuery().mustNot(existsQuery(SCIENTIFIC_OTHER)))
            .minimumShouldMatch(1);

    var atLeastOneScientificValueExistsQuery =
        boolQuery()
            .should(existsQuery(SCIENTIFIC_SERIES))
            .should(existsQuery(SCIENTIFIC_PUBLISHER))
            .should(existsQuery(SCIENTIFIC_OTHER));

    var query =
        boolQuery()
            .must(seriesExistsWithValuesOrMissingQuery)
            .must(publisherExistsWithValuesOrMissingQuery)
            .must(journalExistsWithValuesOrMissingQuery)
            .must(atLeastOneScientificValueExistsQuery);

    return Functions.queryToEntry(key, query);
  }

  public Stream<Entry<ResourceParameter, QueryBuilder>> createHasParentQuery(
      ResourceParameter resourceParameter) {
    var query =
        boolQuery()
            .must(existsQuery(REFERENCE_PUBLICATION_CONTEXT_ID_KEYWORD))
            .must(termQuery(PUBLICATION_CONTEXT_TYPE_KEYWORD, "Anthology"));

    return Functions.queryToEntry(resourceParameter, query);
  }

  public Stream<Entry<ResourceParameter, QueryBuilder>> createHasNoParentQuery(
      ResourceParameter resourceParameter) {
    var query =
        boolQuery()
            .mustNot(existsQuery(REFERENCE_PUBLICATION_CONTEXT_ID_KEYWORD))
            .must(termQuery(PUBLICATION_CONTEXT_TYPE_KEYWORD, "Anthology"));

    return Functions.queryToEntry(resourceParameter, query);
  }

  public Stream<Entry<ResourceParameter, QueryBuilder>> createExcludeParentTypeQuery(
      ResourceParameter resourceParameter) {

    var values = parameters.get(resourceParameter).split(COMMA);
    var query =
        boolQuery()
            .must(existsQuery(PARENT_PUBLICATION_TYPE))
            .mustNot(termsQuery(PARENT_PUBLICATION_TYPE, values));

    return Functions.queryToEntry(resourceParameter, query);
  }

  private Boolean shouldSearchSpecifiedInstitutionOnly() {
    return parameters.get(EXCLUDE_SUBUNITS).asBoolean();
  }
}
