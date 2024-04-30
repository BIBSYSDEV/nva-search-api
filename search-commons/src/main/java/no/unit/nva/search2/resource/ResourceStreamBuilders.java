package no.unit.nva.search2.resource;

import no.unit.nva.search2.common.QueryKeys;
import no.unit.nva.search2.common.QueryTools;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.FUNDINGS;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_TRUE;
import static no.unit.nva.search2.common.constant.Words.SOURCE;
import static no.unit.nva.search2.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search2.common.constant.Words.VALUE;
import static no.unit.nva.search2.resource.Constants.selectByLicense;
import static no.unit.nva.search2.resource.ResourceParameter.EXCLUDE_SUBUNITS;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;

import java.util.Map;
import java.util.stream.Stream;

public class ResourceStreamBuilders {

    protected final transient QueryTools<ResourceParameter> queryTools;
    private final QueryKeys<ResourceParameter> parameters;


    public ResourceStreamBuilders(QueryTools<ResourceParameter> queryTools, QueryKeys<ResourceParameter> parameters) {
        this.queryTools = queryTools;
        this.parameters = parameters;
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> licenseQuery(ResourceParameter key) {
        var query = QueryBuilders.scriptQuery(selectByLicense(parameters.get(key).as()));
        return queryTools.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> additionalIdentifierQuery(
        ResourceParameter key, String source) {
        var query = QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), parameters.get(key).as()))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

        return queryTools.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> fundingQuery(ResourceParameter key) {
        var values = parameters.get(key).split(COLON);
        var query = QueryBuilders.nestedQuery(
            FUNDINGS,
            boolQuery()
                .must(termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                .must(termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
            ScoreMode.None);
        return queryTools.queryToEntry(key, query);
    }


    public Stream<Map.Entry<ResourceParameter, QueryBuilder>> subUnitIncludedQuery(ResourceParameter key) {
        var query =
            parameters.get(EXCLUDE_SUBUNITS).asBoolean()
                ? termQuery(extractTermPath(key), parameters.get(key).as())
                : termQuery(extractMatchPath(key), parameters.get(key).as());

        return queryTools.queryToEntry(key, query);
    }

    private String extractTermPath(ResourceParameter key) {
        return key.searchFields(KEYWORD_TRUE).findFirst().orElseThrow();
    }

    private String extractMatchPath(ResourceParameter key) {
        return key.searchFields(KEYWORD_TRUE).skip(1).findFirst().orElseThrow();
    }

}
