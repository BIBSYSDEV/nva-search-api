package no.unit.nva.search.resource;

import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static no.unit.nva.search.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.common.constant.Words.ASTERISK;
import static no.unit.nva.search.common.constant.Words.COLON;
import static no.unit.nva.search.common.constant.Words.FUNDINGS;
import static no.unit.nva.search.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search.common.constant.Words.KEYWORD;
import static no.unit.nva.search.common.constant.Words.SOURCE;
import static no.unit.nva.search.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search.common.constant.Words.SPACE;
import static no.unit.nva.search.common.constant.Words.VALUE;
import static no.unit.nva.search.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search.resource.ResourceParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search.resource.ResourceParameter.TITLE;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.common.QueryKeys;
import no.unit.nva.search.common.builder.FuzzyKeywordQuery;
import no.unit.nva.search.common.constant.Functions;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * StreamBuilders for Resource.
 *
 * @author Stig Norland
 */
public class ResourceStreamBuilders {

  private final QueryKeys<ResourceParameter> parameters;

  public ResourceStreamBuilders(QueryKeys<ResourceParameter> parameters) {
    this.parameters = parameters;
  }

  /**
   * Query for searching all fields with boosts.
   *
   * @param fields the fields
   * @return the stream
   */
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
                QueryBuilders.multiMatchQuery(sevenValues)
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

  /**
   * Query for searching for additional identifiers.
   *
   * @param key the key
   * @param source the source
   * @return the stream
   */
  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> additionalIdentifierQuery(
      ResourceParameter key, String source) {
    var query =
        QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(
                    termQuery(
                        jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), parameters.get(key).as()))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

    return Functions.queryToEntry(key, query);
  }

  /**
   * Query for searching for fundings.
   *
   * @param key the key
   * @return the stream
   */
  public Stream<Map.Entry<ResourceParameter, QueryBuilder>> fundingQuery(ResourceParameter key) {
    var values = parameters.get(key).split(COLON);
    var query =
        QueryBuilders.nestedQuery(
            FUNDINGS,
            boolQuery()
                .must(termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                .must(termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
            ScoreMode.None);
    return Functions.queryToEntry(key, query);
  }

  /**
   * Query for searching for subunits.
   *
   * @param key the key
   * @return the stream
   */
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
