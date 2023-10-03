package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.model.ResourceParameterKey.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.model.ResourceParameterKey.keyFromString;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.model.ResourceParameterKey;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;

public final class ResourceSwsQuery extends OpenSearchQuery<ResourceParameterKey> {

    private ResourceSwsQuery() {
        super();
    }

    public PagedSearchResourceDto doSearch(OpenSearchSwsClient queryClient) throws ApiGatewayException {
        return
            Stream.of(queryClient.doSearch(this, APPLICATION_JSON.toString()))
                .map(this::toResponse)
                .findFirst().orElseThrow();
    }

    @NotNull
    private PagedSearchResourceDto toResponse(@NotNull OpenSearchSwsResponse response) {

        final var requestParameter = toGateWayRequestParameter();
        final var source = URI.create(gatewayUri.toString().split("/?")[0]);

        return PagedSearchResourceDto.Builder.builder()
                   .withTotalHits(response.getTotalSize())
                   .withHits(response.getSearchHits())
                   .withAggregations(response.getAggregationsStructured())
                   .withIds(source, requestParameter, getValue(FROM).as(), getValue(SIZE).as())
                   .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                   .build();
    }

    private URI nextResultsBySortKey(
        @NotNull OpenSearchSwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {
        requestParameter.remove(FROM.key());
        var sortedP =
            response.getSort().stream().map(Object::toString).collect(Collectors.joining(","));
        requestParameter.put(SEARCH_AFTER.key(), sortedP);
        return UriWrapper.fromUri(gatewayUri)
                   .addQueryParameters(requestParameter)
                   .getUri();
    }

    public static final class Builder
        extends OpenSearchQueryBuilder<ResourceParameterKey, ResourceSwsQuery> {

        public static final String ALL = "all";

        private Builder() {
            super(new ResourceSwsQuery());
        }

        public static Builder queryBuilder() {
            return new Builder();
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.key(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.key(), DEFAULT_VALUE_SORT + ":" + DEFAULT_VALUE_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            switch (qpKey) {
                case SEARCH_AFTER,
                         FROM,
                         SIZE,
                         PAGE -> query.setQueryValue(qpKey, value);
                case FIELDS -> query.setQueryValue(qpKey, expandFields(value));
                case SORT -> setSortQuery(qpKey, value);
                case SORT_ORDER -> addSortOrderToSortQuery(value);
                case CATEGORY, CONTRIBUTOR,
                         CREATED_BEFORE, CREATED_SINCE,
                         DOI, FUNDING, FUNDING_SOURCE, ID,
                         INSTITUTION, ISSN,
                         MODIFIED_BEFORE, MODIFIED_SINCE,
                         PROJECT_CODE, PUBLISHED_BEFORE,
                         PUBLISHED_SINCE, SEARCH_ALL, TITLE,
                         UNIT, USER, YEAR_REPORTED -> query.setLucineValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }

        private void addSortOrderToSortQuery(String value) {
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), value));
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (isNull(query.getValue(FROM))) {
                var page = query.getValue(PAGE).<Integer>as();
                var perPage = query.getValue(SIZE).<Integer>as();
                query.setQueryValue(FROM, String.valueOf(page * perPage));
            }
            query.removeValue(PAGE);
            // TODO check if field is set and has value 'all' then populate with all fields
        }

        private String expandFields(String value) {
            return ALL.equals(value)
                       ? String.join("|", VALID_LUCENE_PARAMETER_KEYS.stream().map(ResourceParameterKey::key).toList())
                       : value;
        }

        private void setSortQuery(ResourceParameterKey qpKey, String value) {
            var validFieldValue = decodeUTF(value).replaceAll(" (asc|desc)", ":$1");
            query.setQueryValue(qpKey, mergeParameters(query.getValue(qpKey).as(), validFieldValue));
        }
    }
}