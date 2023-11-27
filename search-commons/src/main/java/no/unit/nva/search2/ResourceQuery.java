package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.ErrorMessages.UNEXPECTED_VALUE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ASC_OR_DESC;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SELECTED_GROUP;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URL_PARAM_INDICATOR;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.EXPECTED_TWO_PARTS;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.JANUARY_FIRST;
import static no.unit.nva.search2.constant.Words.ZERO;
import static no.unit.nva.search2.enums.ResourceParameter.CONTRIBUTOR_ID;
import static no.unit.nva.search2.enums.ResourceParameter.FIELDS;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.FUNDING;
import static no.unit.nva.search2.enums.ResourceParameter.PAGE;
import static no.unit.nva.search2.enums.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static no.unit.nva.search2.enums.ResourceParameter.SORT_ORDER;
import static no.unit.nva.search2.enums.ResourceParameter.keyFromString;
import static no.unit.nva.search2.enums.ResourceSort.INVALID;
import static no.unit.nva.search2.enums.ResourceSort.validSortKeys;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.common.QueryBuilderSourceWrapper;
import no.unit.nva.search2.common.QueryBuilderTools;
import no.unit.nva.search2.common.SwsResponse;
import no.unit.nva.search2.dto.PagedSearch;
import no.unit.nva.search2.dto.PagedSearchBuilder;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ResourceParameter;
import no.unit.nva.search2.enums.ResourceSort;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ResourceQuery extends Query<ResourceParameter> {



    private ResourceQuery() {
        super();
    }

    static Builder builder() {
        return new Builder();
    }

    public String doSearch(ResourceClient queryClient) {
        final var response = queryClient.doSearch(this);
        return MediaType.CSV_UTF_8.is(this.getMediaType())
            ? toCsvText(response)
            : toPagedResponse(response).toJsonString();
    }

    private String toCsvText(SwsResponse response) {
        return CsvTransformer.transform(response.getSearchHits());
    }

    PagedSearch toPagedResponse(SwsResponse response) {
        final var requestParameter = toNvaSearchApiRequestParameter();
        final var source = URI.create(getNvaSearchApiUri().toString().split(PATTERN_IS_URL_PARAM_INDICATOR)[0]);

        return
            new PagedSearchBuilder()
                .withTotalHits(response.getTotalSize())
                .withHits(response.getSearchHits())
                .withIds(source, requestParameter, getValue(FROM).as(), getValue(SIZE).as())
                .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                .withAggregations(response.getAggregationsStructured())
                .build();
    }

    public Stream<QueryBuilderSourceWrapper> createQueryBuilderStream(UserSettingsClient userSettingsClient) {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : boolQuery();

        if (isPresent(CONTRIBUTOR_ID)) {
            assert queryBuilder instanceof BoolQueryBuilder;
            addPromotedPublications(userSettingsClient, (BoolQueryBuilder) queryBuilder);
        }

        var builder = new SearchSourceBuilder().query(queryBuilder);

        var searchAfter = removeKey(SEARCH_AFTER);
        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }

        if (isFirstPage()) {
            RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
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
                        default -> throw new IllegalStateException(UNEXPECTED_VALUE + key.searchOperator());
                    }
                }
            });
        return bq;
    }

    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {
        var promotedPublications = userSettingsClient.doSearch(this).promotedPublications();
        if (hasPromotedPublications(promotedPublications)) {
            removeKey(SORT);  // remove sort to avoid messing up "sorting by score"
            for (int i = 0; i < promotedPublications.size(); i++) {
                bq.should(
                    QueryBuilders
                        .matchQuery(ID, promotedPublications.get(i))
                        .boost(3.14F + promotedPublications.size() - i)
                );
            }
        }
    }

    @NotNull
    private Stream<Tuple<String, SortOrder>> getSortStream() {
        return
            getOptional(SORT).stream()
                .flatMap(sort -> Arrays.stream(sort.split(COMMA)))
                .map(sort -> sort.split(COLON))
                .map(this::expandSortKeys);
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

    public static URI nextResultsBySortKey(
        SwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {

        requestParameter.remove(FROM.fieldName());
        var sortedP =
            response.getSort().stream()
                .map(Object::toString)
                .collect(Collectors.joining(COMMA));
        requestParameter.put(SEARCH_AFTER.fieldName(), sortedP);
        return UriWrapper.fromUri(gatewayUri)
            .addQueryParameters(requestParameter)
            .getUri();
    }

    @JacocoGenerated
    Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var fieldName = ResourceSort.fromSortKey(strings[0]).getFieldName();
        return new Tuple<>(fieldName, sortOrder);
    }


    public boolean hasPromotedPublications(List<String> promotedPublications) {
        return nonNull(promotedPublications) && !promotedPublications.isEmpty();
    }

    public boolean isFirstPage() {
        return ZERO.equals(getValue(FROM).toString());
    }

    @SuppressWarnings("PMD.GodClass")
    protected static class Builder extends
        QueryBuilder<ResourceParameter, ResourceQuery> {




        Builder() {
            super(new ResourceQuery());
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
                case CREATED_BEFORE, CREATED_SINCE,
                    MODIFIED_BEFORE, MODIFIED_SINCE,
                    PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setSearchFieldValue(qpKey, expandYearToDate(value));
                case CONTEXT_TYPE, CONTEXT_TYPE_NOT, CONTEXT_TYPE_SHOULD,
                    CONTRIBUTOR_ID, CONTRIBUTOR, CONTRIBUTOR_NOT, CONTRIBUTOR_SHOULD,
                    DOI, DOI_NOT, DOI_SHOULD,
                    FUNDING, FUNDING_SOURCE, FUNDING_SOURCE_NOT, FUNDING_SOURCE_SHOULD,
                    ID, ID_NOT, ID_SHOULD,
                    INSTANCE_TYPE, INSTANCE_TYPE_NOT, INSTANCE_TYPE_SHOULD,
                    INSTITUTION, INSTITUTION_NOT, INSTITUTION_SHOULD,
                    ISBN, ISBN_NOT, ISBN_SHOULD, ISSN, ISSN_NOT, ISSN_SHOULD,
                    ORCID, ORCID_NOT, ORCID_SHOULD,
                    PARENT_PUBLICATION, PARENT_PUBLICATION_SHOULD,
                    PROJECT, PROJECT_NOT, PROJECT_SHOULD,
                    PUBLICATION_YEAR, PUBLICATION_YEAR_SHOULD,
                    SEARCH_ALL,
                    TITLE, TITLE_NOT, TITLE_SHOULD,
                    TOP_LEVEL_ORGANIZATION,
                    UNIT, UNIT_NOT, UNIT_SHOULD,
                    USER, USER_NOT, USER_SHOULD -> query.setSearchFieldValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
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
            query.getOptional(FUNDING)
                .ifPresent(funding -> query.setSearchFieldValue(FUNDING, funding.replaceAll(COLON, COMMA)));
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
            var sortKey = ResourceSort.fromSortKey(sortField);

            if (sortKey == INVALID) {
                throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(sortField, validSortKeys()));
            }
            return sortKey.name().toLowerCase(Locale.getDefault()) + COLON + sortOrder;
        }

        protected String getSortOrder(String... sortKeyParts) {
            return (sortKeyParts.length == EXPECTED_TWO_PARTS)
                ? sortKeyParts[1].toLowerCase(Locale.getDefault())
                : DEFAULT_VALUE_SORT_ORDER;
        }

        private void addSortQuery(String value) {
            var validFieldValue =
                decodeUTF(value)
                    .replaceAll(PATTERN_IS_ASC_OR_DESC, PATTERN_IS_SELECTED_GROUP);
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), validFieldValue));
        }

        public String expandYearToDate(String value) {
            return value.length() == 4 ? value + JANUARY_FIRST : value;
        }

        protected String expandFields(String value) {
            return ALL.equals(value) || isNull(value)
                ? ALL
                : Arrays.stream(value.split(COMMA))
                .filter(this::keyIsValid)           // ignoring invalid keys
                .collect(Collectors.joining(COMMA));
        }

        private boolean keyIsValid(String key) {
            return keyFromString(key) != ResourceParameter.INVALID;
        }
    }
}