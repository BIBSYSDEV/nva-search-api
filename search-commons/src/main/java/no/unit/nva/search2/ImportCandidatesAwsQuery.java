package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH;
import static no.unit.nva.search2.constant.ApplicationConstants.ZERO;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.FIELDS;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.FROM;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.PAGE;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.SEARCH_AFTER;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.SEARCH_ALL;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.SIZE;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.SORT;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.SORT_ORDER;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.model.ParameterKeyImportCandidate.keyFromString;
import static no.unit.nva.search2.model.ResourceSortKeys.INVALID;
import static no.unit.nva.search2.model.ResourceSortKeys.validSortKeys;
import static nva.commons.core.paths.UriWrapper.fromUri;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.model.ParameterKey;
import no.unit.nva.search2.model.ParameterKeyImportCandidate;
import no.unit.nva.search2.model.QueryBuilderSourceWrapper;
import no.unit.nva.search2.model.ResourceSortKeys;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ImportCandidatesAwsQuery extends OpenSearchQuery<ParameterKeyImportCandidate> {
    
    private ImportCandidatesAwsQuery() {
        super();
    }
    
    static Builder builder() {
        return new Builder();
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(IMPORT_CANDIDATES_INDEX, SEARCH)
                .getUri();
    }

    public String doSearch(ImportCandidatesAwsClient queryClient) {
        final var response = queryClient.doSearch(this);
        return MediaType.CSV_UTF_8.is(this.getMediaType())
            ? toCsvText(response)
            : toPagedResponse(response).toJsonString();
    }
    
    private String toCsvText(OpenSearchSwsResponse response) {
        return CsvTransformer.transform(response.getSearchHits());
    }
    
    PagedSearchResourceDto toPagedResponse(OpenSearchSwsResponse response) {
        final var requestParameter = toNvaSearchApiRequestParameter();
        final var source = URI.create(getNvaSearchApiUri().toString().split("\\?")[0]);
        
        return
            PagedSearchResourceDto.Builder.builder()
                .withTotalHits(response.getTotalSize())
                .withHits(response.getSearchHits())
                .withAggregations(response.getAggregationsStructured())
                .withIds(source, requestParameter, getValue(FROM).as(), getValue(SIZE).as())
                .withNextResultsBySortKey(QueryBuilderTools.nextResultsBySortKey(response, requestParameter, source))
                .build();
    }
    
    public Stream<QueryBuilderSourceWrapper> createQueryBuilderStream() {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : boolQuery();
        
        var builder = new SearchSourceBuilder().query(queryBuilder);
        
        var searchAfter = removeKey(SEARCH_AFTER);
        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }
        
        if (isFirstPage()) {
            IMPORT_CANDIDATES_AGGREGATIONS.forEach(builder::aggregation);
        }
        
        builder.size(getValue(SIZE).as());
        builder.from(getValue(FROM).as());
        getSortStream().forEach(orderTuple -> builder.sort(orderTuple.v1(), orderTuple.v2()));
        
        return Stream.of(new QueryBuilderSourceWrapper(builder, this.getOpenSearchUri()));
    }
    
    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @return a BoolQueryBuilder
     */
    @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault"})
    private BoolQueryBuilder boolQuery() {
        var bq = QueryBuilders.boolQuery();
        getOpenSearchParameters()
            .forEach((key, value) -> {
                if (key.equals(SEARCH_ALL)) {
                    bq.must(multiMatchQuery());
                } else if (key.fieldType().equals(ParameterKey.ParamKind.KEYWORD)) {
                    QueryBuilderTools.addKeywordQuery(key, value, bq);
                } else {
                    switch (key.searchOperator()) {
                        case MUST -> bq.must(QueryBuilderTools.buildQuery(key, value));
                        case MUST_NOT -> bq.mustNot(QueryBuilderTools.buildQuery(key, value));
                        case SHOULD -> bq.should(QueryBuilderTools.buildQuery(key, value));
                        case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> bq.must(QueryBuilderTools.rangeQuery(key, value));
                    }
                }
            });
        return bq;
    }
    
    @NotNull
    private Stream<Tuple<String, SortOrder>> getSortStream() {
        return
            getOptional(SORT).stream()
                .flatMap(sort -> Arrays.stream(sort.split(COMMA)))
                .map(sort -> sort.split(COLON))
                .map(QueryBuilderTools::expandSortKeys);
    }
    
    /**
     * Creates a multi match query, all words needs to be present, within a document.
     *
     * @return a MultiMatchQueryBuilder
     */
    private MultiMatchQueryBuilder multiMatchQuery() {
        var fields = QueryBuilderTools.extractFields(getValue(FIELDS).toString());
        var value = getValue(SEARCH_ALL).toString();
        return QueryBuilders
            .multiMatchQuery(value, fields)
            .type(Type.CROSS_FIELDS)
            .operator(Operator.AND);
    }
    
    boolean isFirstPage() {
        return ZERO.equals(getValue(FROM).toString());
    }
    
    @SuppressWarnings("PMD.GodClass")
    protected static class Builder
        extends OpenSearchQueryBuilder<ParameterKeyImportCandidate, ImportCandidatesAwsQuery> {
        
        private static final String ALL = "all";
        public static final Integer EXPECTED_TWO_PARTS = 2;
        
        Builder() {
            super(new ImportCandidatesAwsQuery());
        }
        
        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.fieldName(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.fieldName(), DEFAULT_VALUE_SORT + COLON + DEFAULT_VALUE_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }
        
        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setQueryValue(qpKey, value);
                case FIELDS -> query.setQueryValue(qpKey, expandFields(value));
                case SORT -> addSortQuery(value);
                case SORT_ORDER -> addSortOrderQuery(value);
                case PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setSearchFieldValue(qpKey, expandDate(value));
                case CATEGORY, CATEGORY_NOT, CATEGORY_SHOULD,
                    COLLABORATION_TYPE,
                    DOI, DOI_NOT, DOI_SHOULD,
                    ID, ID_NOT, ID_SHOULD,
                    OWNER, OWNER_NOT, OWNER_SHOULD,
                    PUBLISHER,
                    SEARCH_ALL,
                    TITLE, TITLE_NOT, TITLE_SHOULD -> query.setSearchFieldValue(qpKey, value);
                default -> invalidKeys.add(key);
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
                    query.setQueryValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.removeKey(PAGE);
            }
        }
        
        @Override
        protected Collection<String> validKeys() {
            return VALID_LUCENE_PARAMETER_KEYS.stream()
                .map(ParameterKey::fieldName)
                .toList();
        }
        
        @Override
        protected void validateSort() throws BadRequestException {
            if (!query.isPresent(SORT)) {
                return;
            }
            try {
                var sortKeys = query.getValue(SORT).<String>as().split(COMMA);
                var validSortKeys =
                    Arrays.stream(sortKeys)
                        .map(this::validateSortKey)
                        .collect(Collectors.joining(COMMA));
                
                query.setQueryValue(SORT, validSortKeys);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }
        }
        
        private void addSortOrderQuery(String value) {
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), value));
        }
        
        private String validateSortKey(String keySort) {
            var sortKeyParts = keySort.split(COLON);
            if (sortKeyParts.length > EXPECTED_TWO_PARTS) {
                throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(keySort, validSortKeys()));
            }
            
            var sortOrder = getSortOrder(sortKeyParts);
            
            if (!sortOrder.matches(SORT_ORDER.valuePattern())) {
                throw new IllegalArgumentException("Invalid sort order: " + sortOrder);
            }
            
            var sortField = sortKeyParts[0];
            var sortKey = ResourceSortKeys.fromSortKey(sortField);
            
            if (sortKey == INVALID) {
                throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(sortField, validSortKeys()));
            }
            return sortKey.name().toLowerCase(Locale.getDefault()) + COLON + sortOrder;
        }
        
        private String getSortOrder(String... sortKeyParts) {
            return (sortKeyParts.length == EXPECTED_TWO_PARTS)
                ? sortKeyParts[1].toLowerCase(Locale.getDefault())
                : DEFAULT_VALUE_SORT_ORDER;
        }
        
        private void addSortQuery(String value) {
            var validFieldValue =
                decodeUTF(value)
                    .replaceAll(PATTERN_IS_IGNORE_CASE + " (asc|desc)", ":$1");
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), validFieldValue));
        }
        
        private String expandDate(String value) {
            return value.length() == 4 ? value + "-01-01" : value;
        }
        
        private String expandFields(String value) {
            return ALL.equals(value) || isNull(value)
                ? ALL
                : Arrays.stream(value.split(COMMA))
                    .filter(this::keyIsValid)
                    .collect(Collectors.joining(COMMA));
        }
        
        private boolean keyIsValid(String key) {
            return keyFromString(key) != ParameterKeyImportCandidate.INVALID;
        }
    }
}