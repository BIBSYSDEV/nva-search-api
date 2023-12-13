package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.valueToBoolean;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Resource.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.enums.ResourceParameter.CONTRIBUTOR_ID;
import static no.unit.nva.search2.enums.ResourceParameter.FIELDS;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.PAGE;
import static no.unit.nva.search2.enums.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static no.unit.nva.search2.enums.ResourceParameter.keyFromString;
import static no.unit.nva.search2.enums.ResourceSort.INVALID;
import static no.unit.nva.search2.enums.ResourceSort.fromSortKey;
import static no.unit.nva.search2.enums.ResourceSort.validSortKeys;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ValueEncoding;
import no.unit.nva.search2.enums.ResourceParameter;
import nva.commons.core.JacocoGenerated;
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

    @Override
    protected Integer getFrom() {
        return getValue(FROM).as();
    }

    @Override
    protected Integer getSize() {
        return getValue(SIZE).as();
    }

    @Override
    protected ResourceParameter getFieldsKey() {
        return FIELDS;
    }

    @Override
    protected String[] fieldsToKeyNames(String field) {
        return ALL.equals(field) || isNull(field)
            ? ASTERISK.split(COMMA)     // NONE or ALL -> ['*']
            : Arrays.stream(field.split(COMMA))
                .map(ResourceParameter::keyFromString)
                .map(ParameterKey::searchFields)
                .flatMap(Collection::stream)
                .map(fieldPath -> fieldPath.replace(DOT + KEYWORD, EMPTY_STRING))
                .toArray(String[]::new);
    }

    @Override
    public String getSort() {
        return getValue(SORT).as();
    }

    @Override
    protected URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(Words.RESOURCES, Words.SEARCH)
                .getUri();
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

        RESOURCES_AGGREGATIONS.forEach(builder::aggregation);

        builder.size(getValue(SIZE).as());
        builder.from(getValue(FROM).as());
        getSortStream().forEach(entry -> builder.sort(fromSortKey(entry.getKey()).getFieldName(), entry.getValue()));

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

    @SuppressWarnings("PMD.GodClass")
    protected static class Builder extends QueryBuilder<ResourceParameter, ResourceQuery> {

        Builder() {
            super(new ResourceQuery());
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return keyFromString(keyName) != ResourceParameter.INVALID;
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
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setPagingValue(qpKey, decodedValue);
                case FIELDS -> query.setPagingValue(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToPagingKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToPagingKey(SORT, decodedValue);
                case CREATED_BEFORE, CREATED_SINCE,
                    MODIFIED_BEFORE, MODIFIED_SINCE,
                    PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setSearchingValue(qpKey, expandYearToDate(decodedValue));
                case HAS_FILE -> query.setSearchingValue(qpKey, valueToBoolean(decodedValue).toString());
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
                    USER, USER_NOT, USER_SHOULD,
                    USER_AFFILIATION, USER_AFFILIATION_NOT, USER_AFFILIATION_SHOULD -> mergeToKey(qpKey, decodedValue);
                case LANG -> { /* ignore and continue */ }
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
        }

        @Override
        protected void validateSortEntry(Entry<String, SortOrder> entry) {
            if (fromSortKey(entry.getKey()) == INVALID) {
                throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(entry.getKey(), validSortKeys()));
            }
            attempt(entry::getValue)
                .orElseThrow(e -> new IllegalArgumentException(e.getException().getMessage()));
        }
    }
}