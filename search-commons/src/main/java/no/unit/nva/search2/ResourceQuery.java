package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.QueryTools.valueToBoolean;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Resource.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.constant.Resource.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.enums.ResourceParameter.CONTRIBUTOR;
import static no.unit.nva.search2.enums.ResourceParameter.FIELDS;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.PAGE;
import static no.unit.nva.search2.enums.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static no.unit.nva.search2.enums.ResourceParameter.SORT_ORDER;
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
import no.unit.nva.search2.dto.UserSettings;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ValueEncoding;
import no.unit.nva.search2.enums.PublicationStatus;
import no.unit.nva.search2.enums.ResourceParameter;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class ResourceQuery extends Query<ResourceParameter> {

    private org.opensearch.index.query.QueryBuilder filter;

    private ResourceQuery() {
        super();
    }

    public static ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
    }

    public org.opensearch.index.query.QueryBuilder getFilter() {
        return filter;
    }

    public void setFilter(org.opensearch.index.query.QueryBuilder filter) {
        this.filter = filter;
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
    public AsType getSort() {
        return getValue(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(Words.RESOURCES, Words.SEARCH)
                .getUri();
    }

    @Override
    protected boolean isPagingValue(ResourceParameter key) {
        return key.ordinal() >= FIELDS.ordinal() && key.ordinal() <= SORT_ORDER.ordinal();
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream(UserSettingsClient userSettingsClient) {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : boolQuery();

        if (isLookingForOneContributor()) {
            assert queryBuilder instanceof BoolQueryBuilder;
            addPromotedPublications(userSettingsClient, (BoolQueryBuilder) queryBuilder);
        }


        var builder = new SearchSourceBuilder().query(queryBuilder);

        builder.postFilter(getFilter());


        handleSearchAfter(builder);
        builder.size(getValue(SIZE).as());
        builder.from(getValue(FROM).as());
        builder.trackTotalHits(true);
        getSortStream()
            .forEach(entry -> builder.sort(getFieldName(entry), entry.getValue()));
        RESOURCES_AGGREGATIONS
            .forEach(builder::aggregation);

        logger.debug(builder.toString());

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    private static String getFieldName(Entry<String, SortOrder> entry) {
        return fromSortKey(entry.getKey()).getFieldName();
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var searchAfter = removeKey(SEARCH_AFTER);
        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }
    }

    private boolean isLookingForOneContributor() {
        return hasOneValue(CONTRIBUTOR);
    }

    @JacocoGenerated
    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {
        var promotedPublications =
            attempt(() -> userSettingsClient.doSearch(this))
                .or(() -> new UserSettings(List.of()))
                .get().promotedPublications();
        if (hasContent(promotedPublications)) {
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


    @SuppressWarnings("PMD.GodClass")
    public static class ResourceQueryBuilder extends QueryBuilder<ResourceParameter, ResourceQuery> {

        ResourceQueryBuilder() {
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
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setKeyValue(qpKey, decodedValue);
                case FIELDS -> query.setKeyValue(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case CREATED_BEFORE, CREATED_SINCE,
                    MODIFIED_BEFORE, MODIFIED_SINCE,
                    PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setKeyValue(qpKey, expandYearToDate(decodedValue));
                case HAS_FILE -> query.setKeyValue(qpKey, valueToBoolean(decodedValue).toString());
                case LANG -> { /* ignore and continue */ }
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

        public ResourceQueryBuilder withStatus(PublicationStatus... status) {
            //TODO make filters....
            //query.setFilter();
            return this;
        }

    }
}