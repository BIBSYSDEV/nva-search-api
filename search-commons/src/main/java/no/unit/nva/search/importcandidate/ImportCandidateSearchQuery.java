package no.unit.nva.search.importcandidate;

import static no.unit.nva.search.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static no.unit.nva.search.common.constant.Functions.trimSpace;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.common.constant.Words.COMMA;
import static no.unit.nva.search.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search.common.constant.Words.KEYWORD;
import static no.unit.nva.search.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search.common.constant.Words.NONE;
import static no.unit.nva.search.common.constant.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search.common.constant.Words.SCOPUS_AS_TYPE;
import static no.unit.nva.search.common.constant.Words.SEARCH;
import static no.unit.nva.search.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search.common.constant.Words.VALUE;
import static no.unit.nva.search.importcandidate.Constants.FACET_IMPORT_CANDIDATE_PATHS;
import static no.unit.nva.search.importcandidate.Constants.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search.importcandidate.Constants.IMPORT_CANDIDATES_INDEX_NAME;
import static no.unit.nva.search.importcandidate.Constants.selectByLicense;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.AGGREGATION;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.IMPORT_CANDIDATE_PARAMETER_SET;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.NODES_EXCLUDED;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.NODES_INCLUDED;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.NODES_SEARCHED;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.PAGE;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SEARCH_AFTER;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SIZE;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SORT;

import static nva.commons.core.paths.UriWrapper.fromUri;

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;

import no.unit.nva.search.common.AsType;
import no.unit.nva.search.common.ParameterValidator;
import no.unit.nva.search.common.SearchQuery;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.common.enums.ValueEncoding;

import nva.commons.core.JacocoGenerated;

import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * @author Stig Norland
 */
public final class ImportCandidateSearchQuery extends SearchQuery<ImportCandidateParameter> {

    ImportCandidateSearchQuery() {
        super();
    }

    public static ImportCandidateValidator builder() {
        return new ImportCandidateValidator();
    }

    @Override
    protected ImportCandidateParameter keyAggregation() {
        return AGGREGATION;
    }

    @Override
    protected ImportCandidateParameter keyFields() {
        return NODES_SEARCHED;
    }

    @Override
    protected ImportCandidateParameter keySearchAfter() {
        return SEARCH_AFTER;
    }

    @Override
    protected ImportCandidateParameter toKey(String keyName) {
        return ImportCandidateParameter.keyFromString(keyName);
    }

    @Override
    protected SortKey toSortKey(String sortName) {
        return ImportCandidateSort.fromSortKey(sortName);
    }

    @Override
    protected AsType<ImportCandidateParameter> from() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<ImportCandidateParameter> size() {
        return parameters().get(SIZE);
    }

    @Override
    public AsType<ImportCandidateParameter> sort() {
        return parameters().get(SORT);
    }

    @Override
    protected String[] exclude() {
        return parameters().get(NODES_EXCLUDED).split(COMMA);
    }

    @Override
    protected String[] include() {
        return parameters().get(NODES_INCLUDED).split(COMMA);
    }

    @Override
    public URI openSearchUri() {
        return fromUri(infrastructureApiUri)
                .addChild(IMPORT_CANDIDATES_INDEX_NAME, SEARCH)
                .getUri();
    }

    @Override
    protected Map<String, String> facetPaths() {
        return FACET_IMPORT_CANDIDATE_PATHS;
    }

    @Override
    protected List<AggregationBuilder> builderAggregations() {
        return IMPORT_CANDIDATES_AGGREGATIONS;
    }

    @JacocoGenerated // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<ImportCandidateParameter, QueryBuilder>> builderCustomQueryStream(
            ImportCandidateParameter key) {
        return switch (key) {
            case CRISTIN_IDENTIFIER -> builderStreamAdditionalIdentifier(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> builderStreamAdditionalIdentifier(key, SCOPUS_AS_TYPE);
            case LICENSE, LICENSE_NOT -> licenseQuery(key);
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    private Stream<Entry<ImportCandidateParameter, QueryBuilder>> builderStreamAdditionalIdentifier(
            ImportCandidateParameter key, String source) {
        var value = parameters().get(key).as();
        var query =
                QueryBuilders.nestedQuery(
                        ADDITIONAL_IDENTIFIERS,
                        boolQuery()
                                .must(
                                        termQuery(
                                                jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD),
                                                value))
                                .must(
                                        termQuery(
                                                jsonPath(
                                                        ADDITIONAL_IDENTIFIERS,
                                                        SOURCE_NAME,
                                                        KEYWORD),
                                                source)),
                        ScoreMode.None);

        return Functions.queryToEntry(key, query);
    }

    public Stream<Map.Entry<ImportCandidateParameter, QueryBuilder>> licenseQuery(
            ImportCandidateParameter key) {
        var query = QueryBuilders.scriptQuery(selectByLicense(parameters().get(key).as()));
        return Functions.queryToEntry(key, query);
    }

    public static class ImportCandidateValidator
            extends ParameterValidator<ImportCandidateParameter, ImportCandidateSearchQuery> {

        ImportCandidateValidator() {
            super(new ImportCandidateSearchQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing()
                    .forEach(
                            key -> {
                                switch (key) {
                                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                                    case SORT -> setValue(key.name(), RELEVANCE_KEY_NAME);
                                    case AGGREGATION -> setValue(key.name(), NONE);
                                    default -> {
                                        /* do nothing */
                                    }
                                }
                            });
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (searchQuery.parameters().isPresent(PAGE)) {
                if (searchQuery.parameters().isPresent(FROM)) {
                    var page = searchQuery.parameters().get(PAGE).<Number>as();
                    var perPage = searchQuery.parameters().get(SIZE).<Number>as();
                    searchQuery
                            .parameters()
                            .set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                searchQuery.parameters().remove(PAGE);
            }
        }

        @Override
        protected Collection<String> validKeys() {
            return IMPORT_CANDIDATE_PARAMETER_SET.stream()
                    .map(ImportCandidateParameter::asLowerCase)
                    .toList();
        }

        @Override
        protected void validateSortKeyName(String name) {
            var nameSort = name.split(COLON_OR_SPACE);
            if (nameSort.length == NAME_AND_SORT_LENGTH) {
                SortOrder.fromString(nameSort[1]);
            } else if (nameSort.length > NAME_AND_SORT_LENGTH) {
                throw new IllegalArgumentException(TOO_MANY_ARGUMENTS + name);
            }
            if (ImportCandidateSort.fromSortKey(nameSort[0]) == ImportCandidateSort.INVALID) {
                throw new IllegalArgumentException(
                        INVALID_VALUE_WITH_SORT.formatted(
                                name, ImportCandidateSort.validSortKeys()));
            }
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = ImportCandidateParameter.keyFromString(key);
            var decodedValue =
                    qpKey.valueEncoding() != ValueEncoding.NONE
                            ? Functions.decodeUTF(value)
                            : value;
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION ->
                        searchQuery.parameters().set(qpKey, decodedValue);
                case NODES_SEARCHED ->
                        searchQuery.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case INVALID -> invalidKeys.add(key);
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return ImportCandidateParameter.keyFromString(keyName)
                    != ImportCandidateParameter.INVALID;
        }
    }
}
