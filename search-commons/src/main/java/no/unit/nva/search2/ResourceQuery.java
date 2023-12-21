package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.constant.Resource.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.enums.ResourceParameter.CONTRIBUTOR;
import static no.unit.nva.search2.enums.ResourceParameter.FIELDS;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static no.unit.nva.search2.enums.ResourceParameter.SORT_ORDER;
import static no.unit.nva.search2.enums.ResourceSort.fromSortKey;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ResourceParameter;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

public final class ResourceQuery extends Query<ResourceParameter> {

    ResourceQuery() {
        super();
    }

    public static ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
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
    protected URI getOpenSearchUri() {
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

        handleSearchAfter(builder);

        RESOURCES_AGGREGATIONS.forEach(builder::aggregation);

        builder.size(getValue(SIZE).as());
        builder.from(getValue(FROM).as());
        getSortStream().forEach(entry -> builder.sort(fromSortKey(entry.getKey()).getFieldName(), entry.getValue()));

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
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

    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {
        var promotedPublications = userSettingsClient.doSearch(this).promotedPublications();
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

}