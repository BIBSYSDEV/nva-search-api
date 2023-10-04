package no.unit.nva.search2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.model.ResourceParameterKey;
import no.unit.nva.search2.model.SortKeys;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.search.SearchHit;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search.models.SearchResponseDto.formatAggregations;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT_ORDER;
import static no.unit.nva.search2.model.ResourceParameterKey.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.model.ResourceParameterKey.keyFromString;
import static no.unit.nva.search2.model.SortKeys.INVALID;
import static no.unit.nva.search2.model.SortKeys.validSortKeys;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public final class ResourceAwsQuery extends OpenSearchQuery<ResourceParameterKey> {

    private ResourceAwsQuery() {
        super();
    }

    public PagedSearchResourceDto doSearch(OpenSearchAwsClient queryClient) throws ApiGatewayException {
        try (queryClient) {
            return Stream.of(queryClient.doSearch(this, APPLICATION_JSON.toString()))
                       .map(this::toResponse)
                       .findFirst().orElseThrow();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

    @NotNull
    private PagedSearchResourceDto toResponse(@NotNull SearchResponse response) {

        final var source = URI.create(gatewayUri.toString().split("\\?")[0]);
        final var requestParameter = toGateWayRequestParameter();
        final var offset = getValue(FROM).<Integer>as();
        final var size = getValue(SIZE).<Integer>as();

        var hits = Arrays.stream(response.getHits().getHits())
                       .map(hitsToJsonString())
                       .map(jsonToNode())
                       .toList();

        return PagedSearchResourceDto.Builder.builder()
                   .withTotalHits(response.getHits().getTotalHits().value)
                   .withHits(hits)
                   .withAggregations(extractAggregations(response))
                   .withIds(source,requestParameter, offset, size)
                   .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                   .build();
    }

    @NotNull
    private static Function<String, JsonNode> jsonToNode() {
        return content -> attempt(() -> objectMapperWithEmpty.readTree(content)).orElseThrow();
    }

    private static Function<SearchHit, String> hitsToJsonString() {
        return hit -> attempt(() -> XContentHelper.convertToJson(
            hit.getSourceRef(), true, false, XContentType.JSON)
        ).orElseThrow();
    }

    private static JsonNode extractAggregations(SearchResponse searchResponse) {
        var json = attempt(() -> objectMapperWithEmpty.readTree(searchResponse.toString())).orElseThrow();
        var aggregations = (ObjectNode) json.get("aggregations");
        return nonNull(aggregations) ? formatAggregations(aggregations) : null;
    }

    private URI nextResultsBySortKey(
        @NotNull SearchResponse response, Map<String, String> requestParameter, URI gatewayUri) {
        var lastIndex = response.getHits().getHits().length - 1;
        if (lastIndex < 0) {
            return null;
        }
        requestParameter.remove(FROM.key());
        var sortedP =
            Arrays.stream(response.getHits().getHits()[lastIndex].getSortValues())
                .map(Object::toString)
                .collect(Collectors.joining(","));
        requestParameter.put(SEARCH_AFTER.key(), sortedP);
        return UriWrapper.fromUri(gatewayUri)
                   .addQueryParameters(requestParameter)
                   .getUri();
    }

    @SuppressWarnings("PMD.GodClass")
    public static final class Builder
        extends OpenSearchQueryBuilder<ResourceParameterKey, ResourceAwsQuery> {

        private static final String ALL = "all";
        public static final Integer EXPECTED_TWO_PARTS = 2;

        private Builder() {
            super(new ResourceAwsQuery());
        }

        public static ResourceAwsQuery.Builder queryBuilder() {
            return new Builder();
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.key(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.key(), DEFAULT_VALUE_SORT + COLON + DEFAULT_VALUE_SORT_ORDER);
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

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (nonNull(query.getValue(PAGE))) {
                if (isNull(query.getValue(FROM))) {
                    var page = query.getValue(PAGE).<Number>as();
                    var perPage = query.getValue(SIZE).<Number>as();
                    query.setQueryValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.removeValue(PAGE);
            }
        }

        @Override
        protected void validateSort() throws BadRequestException {
            var sortKeys = query.getValue(SORT).<String>as().split(COMMA);
            var joiner = new StringJoiner(COMMA);
            for (String sortKey : sortKeys) {
                joiner.add(validateSortKey(sortKey));
            }
            var validSortKeys = joiner.toString();
            query.setQueryValue(SORT, validSortKeys);
        }

        private String validateSortKey(String keySort) throws BadRequestException {
            var sortKeyParts = keySort.split(COLON);
            if (sortKeyParts.length > EXPECTED_TWO_PARTS) {
                throw new BadRequestException(
                    ERROR_MESSAGE_INVALID_VALUE_WITH_SORT.formatted(keySort, validSortKeys())
                );
            }
            var sortOrder = (sortKeyParts.length == EXPECTED_TWO_PARTS)
                ? sortKeyParts[1]
                : DEFAULT_VALUE_SORT_ORDER;
            if (!sortOrder.matches(SORT_ORDER.pattern())) {
                throw new BadRequestException("Invalid sort order: " + sortOrder);
            }
            var sortField = sortKeyParts[0];
            var sortKey = SortKeys.keyFromString(sortField);
            if (sortKey == INVALID) {
                throw new BadRequestException(
                    ERROR_MESSAGE_INVALID_VALUE_WITH_SORT.formatted(sortField, validSortKeys())
                );
            }
            return sortKey.name() + COLON + sortOrder;
        }

        private void addSortOrderToSortQuery(String value) {
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), value));
        }

        private void setSortQuery(ResourceParameterKey qpKey, String value) {
            var validFieldValue = decodeUTF(value).replaceAll(" (asc|desc)", ":$1");
            query.setQueryValue(qpKey, mergeParameters(query.getValue(qpKey).as(), validFieldValue));
        }

        private String expandFields(String value) {
            return ALL.equals(value)
                       ? String.join("|", VALID_LUCENE_PARAMETER_KEYS.stream().map(ResourceParameterKey::key).toList())
                       : value;
        }
    }
}