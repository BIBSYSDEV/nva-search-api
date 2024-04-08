package no.unit.nva.search2.importcandidate;

import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;

import static no.unit.nva.search2.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.SCOPUS_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.SEARCH;
import static no.unit.nva.search2.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search2.common.constant.Words.VALUE;
import static no.unit.nva.search2.importcandidate.Constants.DEFAULT_IMPORT_CANDIDATE_SORT;
import static no.unit.nva.search2.importcandidate.Constants.FACET_IMPORT_CANDIDATE_PATHS;
import static no.unit.nva.search2.importcandidate.Constants.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search2.importcandidate.Constants.IMPORT_CANDIDATES_INDEX_NAME;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.AGGREGATION;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.FIELDS;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.IMPORT_CANDIDATE_PARAMETER_SET;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.PAGE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SEARCH_AFTER;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SORT;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SORT_ORDER;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryTools;
import no.unit.nva.search2.common.enums.SortKey;
import no.unit.nva.search2.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ImportCandidateQuery extends Query<ImportCandidateParameter> {

    ImportCandidateQuery() {
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
        return FIELDS;
    }

    @Override
    protected ImportCandidateParameter keySearchAfter() {
        return SEARCH_AFTER;
    }

    @Override
    protected ImportCandidateParameter keySortOrder() {
        return SORT_ORDER;
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
    protected AsType<ImportCandidateParameter> getFrom() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<ImportCandidateParameter> getSize() {
        return parameters().get(SIZE);
    }

    @Override
    public AsType<ImportCandidateParameter> getSort() {
        return parameters().get(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
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

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<ImportCandidateParameter, QueryBuilder>> builderStreamCustomQuery(
        ImportCandidateParameter key) {
        return switch (key) {
            case CRISTIN_IDENTIFIER -> builderStreamAdditionalIdentifier(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> builderStreamAdditionalIdentifier(key, SCOPUS_AS_TYPE);
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }


    private Stream<Entry<ImportCandidateParameter, QueryBuilder>> builderStreamAdditionalIdentifier(
        ImportCandidateParameter key, String source) {
        var value = parameters().get(key).as();
        var query = QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), value))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

        return queryTools.queryToEntry(key, query);
    }


    @SuppressWarnings("PMD.GodClass")
    public static class ImportCandidateValidator
        extends ParameterValidator<ImportCandidateParameter, ImportCandidateQuery> {

        ImportCandidateValidator() {
            super(new ImportCandidateQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.name(), DEFAULT_IMPORT_CANDIDATE_SORT + COLON + DEFAULT_SORT_ORDER);
                    case AGGREGATION -> setValue(key.name(), ALL);
                    default -> { /* do nothing */
                    }
                }
            });
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (query.parameters().isPresent(PAGE)) {
                if (query.parameters().isPresent(FROM)) {
                    var page = query.parameters().get(PAGE).<Number>as();
                    var perPage = query.parameters().get(SIZE).<Number>as();
                    query.parameters().set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.parameters().remove(PAGE);
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
                    INVALID_VALUE_WITH_SORT.formatted(name, ImportCandidateSort.validSortKeys())
                );
            }
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = ImportCandidateParameter.keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? QueryTools.decodeUTF(value)
                : value;
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.parameters().set(qpKey, decodedValue);
                case FIELDS -> query.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case AGGREGATION -> query.parameters().set(qpKey, ignoreInvalidAggregations(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case INVALID -> invalidKeys.add(key);
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return ImportCandidateParameter.keyFromString(keyName) != ImportCandidateParameter.INVALID;
        }

        @Override
        protected boolean isAggregationValid(String aggregationName) {
            return
                ALL.equalsIgnoreCase(aggregationName)
                    || NONE.equalsIgnoreCase(aggregationName)
                    || IMPORT_CANDIDATES_AGGREGATIONS.stream()
                        .anyMatch(builder -> builder.getName().equalsIgnoreCase(aggregationName));
        }
    }
}