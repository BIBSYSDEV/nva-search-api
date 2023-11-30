package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.ResourcePaths.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.ZERO;
import static no.unit.nva.search2.enums.ResourceParameter.CONTRIBUTOR_ID;
import static no.unit.nva.search2.enums.ResourceParameter.FIELDS;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.FUNDING;
import static no.unit.nva.search2.enums.ResourceParameter.PAGE;
import static no.unit.nva.search2.enums.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static no.unit.nva.search2.enums.ResourceParameter.SORT_ORDER;
import static no.unit.nva.search2.enums.ResourceParameter.keyFromString;
import static no.unit.nva.search2.enums.ResourceSort.INVALID;
import static no.unit.nva.search2.enums.ResourceSort.fromSortKey;
import static no.unit.nva.search2.enums.ResourceSort.validSortKeys;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ResourceParameter;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.BoolQueryBuilder;
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

    public Stream<QueryContentWrapper> createQueryBuilderStream(UserSettingsClient userSettingsClient) {
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
        getSortStream(SORT).forEach(orderTuple -> builder.sort(orderTuple.v1(), orderTuple.v2()));

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
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

    private boolean hasPromotedPublications(List<String> promotedPublications) {
        return nonNull(promotedPublications) && !promotedPublications.isEmpty();
    }

    @Override
    protected Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var fieldName = fromSortKey(strings[0]).getFieldName();
        return new Tuple<>(fieldName, sortOrder);
    }

    @Override
    protected ResourceParameter getFieldsKey() {
        return FIELDS;
    }

    @NotNull
    @Override
    protected String[] fieldsToKeyNames(String field) {
        return ALL.equals(field) || isNull(field)
            ? ASTERISK.split(COMMA)     // NONE or ALL -> ['*']
            : Arrays.stream(field.split(COMMA))
                .map(ResourceParameter::keyFromString)
                .map(ParameterKey::searchFields)
                .flatMap(Collection::stream)
                .toArray(String[]::new);
    }

    @Override
    public boolean isFirstPage() {
        return ZERO.equals(getValue(FROM).toString());
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
    public String getSort() {
        return getValue(SORT).as();
    }

    @SuppressWarnings("PMD.GodClass")
    protected static class Builder extends QueryBuilder<ResourceParameter, ResourceQuery> {

        Builder() {
            super(new ResourceQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.fieldName(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.fieldName(), DEFAULT_RESOURCE_SORT + COLON + DEFAULT_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setPagingValue(qpKey, value);
                case FIELDS -> query.setPagingValue(qpKey, expandFields(value));
                case SORT -> mergeToPagingKey(SORT, extractDescAsc(value));
                case SORT_ORDER -> mergeToPagingKey(SORT, value);
                case CREATED_BEFORE, CREATED_SINCE,
                    MODIFIED_BEFORE, MODIFIED_SINCE,
                    PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setSearchingValue(qpKey, expandYearToDate(value));
                case CONTEXT_TYPE, CONTEXT_TYPE_NOT, CONTEXT_TYPE_SHOULD,
                    CONTRIBUTOR_ID, CONTRIBUTOR, CONTRIBUTOR_NOT,
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
                    USER, USER_NOT, USER_SHOULD -> query.setSearchingValue(qpKey, value);
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
                    query.setPagingValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.removeKey(PAGE);
            }
            query.getOptional(FUNDING)
                .ifPresent(funding -> query.setSearchingValue(FUNDING, funding.replaceAll(COLON, COMMA)));
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return keyFromString(keyName) != ResourceParameter.INVALID;
        }

        @Override
        protected void validateSortEntry(Entry<String, String> entry) {
            var sortOrder = nonNull(entry.getValue())
                ? entry.getValue().toLowerCase(Locale.getDefault())
                : DEFAULT_SORT_ORDER;
            if (!sortOrder.matches(SORT_ORDER.valuePattern())) {
                throw new IllegalArgumentException("Invalid sort order: " + sortOrder);
            }
            var sortKey = fromSortKey(entry.getKey());

            if (sortKey == INVALID) {
                throw new IllegalArgumentException(
                    INVALID_VALUE_WITH_SORT.formatted(entry.getKey(), validSortKeys()));
            }

            //            return sortKey.name().toLowerCase(Locale.getDefault()) + COLON + sortOrder;
        }
    }
}