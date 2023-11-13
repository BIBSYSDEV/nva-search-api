package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.ApplicationConstants.ALL;
import static no.unit.nva.search2.constant.ApplicationConstants.ASTERISK;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.ZERO;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.model.ResourceParameterKey.CONTRIBUTOR_ID;
import static no.unit.nva.search2.model.ResourceParameterKey.FIELDS;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.FUNDING;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_ALL;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT_ORDER;
import static no.unit.nva.search2.model.ResourceParameterKey.keyFromString;
import static no.unit.nva.search2.model.ResourceSortKeys.INVALID;
import static no.unit.nva.search2.model.ResourceSortKeys.validSortKeys;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.model.ParameterKey;
import no.unit.nva.search2.model.QueryBuilderSourceWrapper;
import no.unit.nva.search2.model.ResourceParameterKey;
import no.unit.nva.search2.model.ResourceSortKeys;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ResourceAwsQuery extends OpenSearchQuery<ResourceParameterKey> {

    private static final Integer SINGLE_FIELD = 1;

    private ResourceAwsQuery() {
        super();
    }

    static Builder builder() {
        return new Builder();
    }

    public String doSearch(ResourceAwsClient queryClient) {
        final var response = queryClient.doSearch(this);
        return
            MediaType.CSV_UTF_8.is(this.getMediaType())
                ? toCsvText(response)
                : toPagedResponse(response).toJsonString();
    }

    public Stream<QueryBuilderSourceWrapper> createQueryBuilderStream(UserSettingsClient userSettingsClient) {
        var queryBuilder = this.hasNoSearchValue()
                               ? QueryBuilders.matchAllQuery()
                               : boolQuery(userSettingsClient);

        var builder = new SearchSourceBuilder().query(queryBuilder);
        var searchAfter = removeKey(SEARCH_AFTER);

        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }

        if (isFirstPage(this)) {
            RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        }

        builder.size(this.getValue(SIZE).as());
        builder.from(this.getValue(FROM).as());
        getSortStream(this)
            .forEach(orderTuple -> builder.sort(orderTuple.v1(), orderTuple.v2()));

        return Stream.of(new QueryBuilderSourceWrapper(builder, this.getOpenSearchUri()));
    }

    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @param userSettingsClient UserSettingsClient
     * @return a BoolQueryBuilder
     */
    @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault"})
    private BoolQueryBuilder boolQuery(UserSettingsClient userSettingsClient) {
        var bq = QueryBuilders.boolQuery();
        getOpenSearchParameters()
            .forEach((key, value) -> {
                if (key.equals(SEARCH_ALL)) {
                    bq.must(multiMatchQuery());
                } else if (key.fieldType().equals(ParameterKey.ParamKind.KEYWORD)) {
                    addKeywordQuery(key, value, bq);
                } else {
                    switch (key.searchOperator()) {
                        case MUST -> bq.must(buildQuery(key, value));
                        case MUST_NOT -> bq.mustNot(buildQuery(key, value));
                        case SHOULD -> bq.should(buildQuery(key, value));
                        case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> bq.must(rangeQuery(key, value));
                    }
                }
                if (key.equals(CONTRIBUTOR_ID)) {
                    addPromotedPublications(userSettingsClient, bq);
                }
            });
        return bq;
    }

    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {
        var promotedPublications = userSettingsClient.doSearch(this).promotedPublications();
        if (hasPromotedPublications(promotedPublications)) {
            removeKey(SORT);  // remove sort to avoid messing up "sorting by score"
            for (int i = 0; i < promotedPublications.size(); i++) {
                bq.should(QueryBuilders
                              .matchQuery("id", promotedPublications.get(i))
                              .boost(3.14F + promotedPublications.size() - i));
            }
        }
    }

    /**
     * Creates a multi match query, all words needs to be present, within a document.
     *
     * @return a MultiMatchQueryBuilder
     */
    private MultiMatchQueryBuilder multiMatchQuery() {
        var fields = extractFields(getValue(FIELDS).toString());
        var value = getValue(SEARCH_ALL).toString();
        return QueryBuilders
                   .multiMatchQuery(value, fields)
                   .type(Type.CROSS_FIELDS)
                   .operator(Operator.AND);
    }

    private void addKeywordQuery(ResourceParameterKey key, String value, BoolQueryBuilder bq) {
        final var searchFields = key.searchFields().toArray(String[]::new);
        final var values = Arrays.stream(value.split(COMMA))
                               .map(String::trim)
                               //            .map(ParameterKey::escapeSearchString)
                               .toArray(String[]::new);
        final var multipleFields = hasMultipleFields(searchFields);

        Arrays.stream(searchFields).forEach(searchField -> {
            final var termsQuery = QueryBuilders.termsQuery(searchField, values).boost(key.fieldBoost());
            switch (key.searchOperator()) {
                case MUST -> {
                    if (multipleFields) {
                        bq.should(termsQuery);
                    } else {
                        bq.must(termsQuery);
                    }
                }
                case MUST_NOT -> bq.mustNot(termsQuery);
                case SHOULD -> bq.should(termsQuery);
                default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            }
        });
    }

    private QueryBuilder buildQuery(ResourceParameterKey key, String value) {
        final var searchFields =
            key.searchFields().stream()
                .map(String::trim)
                .map(trimmed -> !key.fieldType().equals(ParameterKey.ParamKind.KEYWORD)
                                    ? trimmed.replace(".keyword", "")
                                    : trimmed)
                .toArray(String[]::new);
        if (hasMultipleFields(searchFields)) {
            return QueryBuilders
                       .multiMatchQuery(value, searchFields)
                       .operator(operatorByKey(key));
        }
        var searchField = searchFields[0];
        return QueryBuilders
                   .matchQuery(searchField, value)
                   .boost(key.fieldBoost())
                   .operator(operatorByKey(key));
    }

    private RangeQueryBuilder rangeQuery(ResourceParameterKey key, String value) {
        final var searchField = key.searchFields().toArray()[0].toString();

        return switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        };
    }

    private Operator operatorByKey(ResourceParameterKey key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case MUST_NOT, SHOULD -> Operator.AND;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        };
    }

    @NotNull
    private String[] extractFields(String field) {
        return ALL.equals(field) || Objects.isNull(field)
                   ? ASTERISK.split(COMMA)
                   : Arrays.stream(field.split(COMMA))
                         .map(ResourceParameterKey::keyFromString)
                         .map(ResourceParameterKey::searchFields)
                         .flatMap(Collection::stream)
                         .toArray(String[]::new);
    }

    @JacocoGenerated
    private Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var fieldName = ResourceSortKeys.fromSortKey(strings[0]).getFieldName();
        return new Tuple<>(fieldName, sortOrder);
    }

    @NotNull
    private Stream<Tuple<String, SortOrder>> getSortStream(ResourceAwsQuery query) {
        return
            query.getOptional(SORT).stream()
                .flatMap(sort -> Arrays.stream(sort.split(COMMA)))
                .map(sort -> sort.split(COLON))
                .map(this::expandSortKeys);
    }

    private boolean isFirstPage(ResourceAwsQuery query) {
        return ZERO.equals(query.getValue(FROM).toString());
    }

    private boolean hasMultipleFields(String... swsKey) {
        return swsKey.length > SINGLE_FIELD;
    }

    private boolean hasPromotedPublications(List<String> promotedPublications) {
        return nonNull(promotedPublications) && !promotedPublications.isEmpty();
    }

    String toCsvText(OpenSearchSwsResponse response) {
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
                .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                .build();
    }

    private URI nextResultsBySortKey(
        @NotNull OpenSearchSwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {
        requestParameter.remove(FROM.fieldName());
        var sortedP =
            response.getSort().stream().map(Object::toString).collect(Collectors.joining(COMMA));
        requestParameter.put(SEARCH_AFTER.fieldName(), sortedP);
        return UriWrapper.fromUri(gatewayUri)
                   .addQueryParameters(requestParameter)
                   .getUri();
    }

    @SuppressWarnings("PMD.GodClass")
    protected static class Builder extends OpenSearchQueryBuilder<ResourceParameterKey, ResourceAwsQuery> {

        private static final String ALL = "all";
        public static final Integer EXPECTED_TWO_PARTS = 2;

        Builder() {
            super(new ResourceAwsQuery());
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
                         PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setSearchFieldValue(qpKey, expandDate(value));
                case CATEGORY, CATEGORY_NOT, CATEGORY_SHOULD,
                         CONTRIBUTOR_ID, CONTRIBUTOR, CONTRIBUTOR_NOT, CONTRIBUTOR_SHOULD,
                         DOI, DOI_NOT, DOI_SHOULD,
                         FUNDING, FUNDING_SOURCE, FUNDING_SOURCE_NOT, FUNDING_SOURCE_SHOULD,
                         ID, ID_NOT, ID_SHOULD,
                         INSTITUTION, INSTITUTION_NOT, INSTITUTION_SHOULD,
                         ISBN, ISBN_NOT, ISBN_SHOULD, ISSN, ISSN_NOT, ISSN_SHOULD,
                         ORCID, ORCID_NOT, ORCID_SHOULD,
                         PROJECT, PROJECT_NOT, PROJECT_SHOULD,
                         PUBLICATION_YEAR, PUBLICATION_YEAR_SHOULD,
                         SEARCH_ALL,
                         TITLE, TITLE_NOT, TITLE_SHOULD,
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
            return keyFromString(key) != ResourceParameterKey.INVALID;
        }
    }
}