package no.unit.nva.search2.importcandidate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
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
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.PAGE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SEARCH_AFTER;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SORT;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SORT_ORDER;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.keyFromString;
import static no.unit.nva.search2.importcandidate.ImportCandidateSort.INVALID;
import static no.unit.nva.search2.importcandidate.ImportCandidateSort.fromSortKey;
import static no.unit.nva.search2.importcandidate.ImportCandidateSort.validSortKeys;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ImportCandidateQuery extends Query<ImportCandidateParameter> {

    public static Builder builder() {
        return new Builder();
    }

    ImportCandidateQuery() {
        super();
    }

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<ImportCandidateParameter, QueryBuilder>> customQueryBuilders(
        ImportCandidateParameter key) {
        return switch (key) {
            case CRISTIN_IDENTIFIER -> additionalIdentifierQuery(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> additionalIdentifierQuery(key, SCOPUS_AS_TYPE);
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }


    @Override
    protected Integer getFrom() {
        return getValue(FROM).as();
    }

    @Override
    protected Integer getSize() {
        return getValue(SIZE).as();
    }

    @Override
    protected ImportCandidateParameter getFieldsKey() {
        return FIELDS;
    }

    @Override
    protected String[] fieldsToKeyNames(String field) {
        return ALL.equals(field) || isNull(field)
            ? ASTERISK.split(COMMA)     // NONE or ALL -> ['*']
            : Arrays.stream(field.split(COMMA))
                .map(ImportCandidateParameter::keyFromString)
                .flatMap(ParameterKey::searchFields)
                .toArray(String[]::new);
    }

    @Override
    public AsType<ImportCandidateParameter> getSort() {
        return getValue(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(IMPORT_CANDIDATES_INDEX_NAME, SEARCH)
                .getUri();
    }

    @Override
    protected boolean isPagingValue(ImportCandidateParameter key) {
        return key.ordinal() >= FIELDS.ordinal() && key.ordinal() <= SORT_ORDER.ordinal();
    }

    @Override
    protected Map<String, String> aggregationsDefinition() {
        return FACET_IMPORT_CANDIDATE_PATHS;
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream() {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : mainQuery();

        var builder = defaultSearchSourceBuilder(queryBuilder);

        handleSearchAfter(builder);

        builder.aggregation(getAggregationsWithFilter());

        getSortStream().forEach(entry -> builder.sort(fromSortKey(entry.getKey()).jsonPath(), entry.getValue()));

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = removeKey(SEARCH_AFTER).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
    }

    private FilterAggregationBuilder getAggregationsWithFilter() {
        var aggrFilter = AggregationBuilders.filter(Words.POST_FILTER, getFilters());
        IMPORT_CANDIDATES_AGGREGATIONS
            .stream().filter(this::isRequestedAggregation)
            .forEach(aggrFilter::subAggregation);
        return aggrFilter;
    }

    private boolean isRequestedAggregation(AggregationBuilder aggregationBuilder) {
        return Optional.ofNullable(aggregationBuilder)
            .map(AggregationBuilder::getName)
            .map(this::isDefined)
            .orElse(false);
    }

    private boolean isDefined(String keyName) {
        return getValue(AGGREGATION)
            .asSplitStream(COMMA)
            .anyMatch(name -> name.equalsIgnoreCase(ALL) || name.equalsIgnoreCase(keyName));
    }

    public Stream<Entry<ImportCandidateParameter, QueryBuilder>> additionalIdentifierQuery(
        ImportCandidateParameter key, String source) {
        var value = getValue(key).as();
        var query = QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), value))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

        return queryTools.queryToEntry(key, query);
    }


    @SuppressWarnings("PMD.GodClass")
    public static class Builder extends ParameterValidator<ImportCandidateParameter, ImportCandidateQuery> {

        Builder() {
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

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setKeyValue(qpKey, decodedValue);
                case FIELDS -> query.setKeyValue(qpKey, ignoreInvalidFields(decodedValue));
                case AGGREGATION -> query.setKeyValue(qpKey, ignoreInvalidAggregations(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case INVALID -> invalidKeys.add(key);
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (query.isPresent(PAGE)) {
                if (query.isPresent(FROM)) {
                    var page = query.getValue(PAGE).<Number>as();
                    var perPage = query.getValue(SIZE).<Number>as();
                    query.setKeyValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.removeKey(PAGE);
            }
        }

        @Override
        protected void validateSortEntry(Entry<String, SortOrder> entry) {
            if (fromSortKey(entry.getKey()) == INVALID) {
                throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(entry.getKey(), validSortKeys()));
            }
            attempt(entry::getValue)
                .orElseThrow(e -> new IllegalArgumentException(e.getException().getMessage()));
        }

        @Override
        protected Collection<String> validKeys() {
            return VALID_LUCENE_PARAMETER_KEYS.stream()
                .map(ParameterKey::asCamelCase)
                .toList();
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return keyFromString(keyName) != ImportCandidateParameter.INVALID;
        }

        @Override
        protected boolean isAggregationValid(String aggregationName) {
            return
                ALL.equalsIgnoreCase(aggregationName) ||
                NONE.equalsIgnoreCase(aggregationName) ||
                IMPORT_CANDIDATES_AGGREGATIONS.stream()
                    .anyMatch(builder -> builder.getName().equalsIgnoreCase(aggregationName));
        }
    }
}