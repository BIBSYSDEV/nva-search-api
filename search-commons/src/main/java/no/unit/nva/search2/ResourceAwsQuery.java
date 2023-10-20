package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT_ORDER;
import static no.unit.nva.search2.model.ResourceParameterKey.keyFromString;
import static no.unit.nva.search2.model.ResourceSortKeys.INVALID;
import static no.unit.nva.search2.model.ResourceSortKeys.validSortKeys;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.model.ResourceParameterKey;
import no.unit.nva.search2.model.ResourceSortKeys;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import com.google.common.net.MediaType;
import org.jetbrains.annotations.NotNull;

public final class ResourceAwsQuery extends OpenSearchQuery<ResourceParameterKey> {

    private ResourceAwsQuery() {
        super();
    }

    static Builder builder() {
        return new Builder();
    }

    public String doSearch(ResourceAwsClient queryClient) {
        return
            MediaType.CSV_UTF_8.is(this.getMediaType())
                ? fetchAsCsvText(queryClient)
                : fetchAsPagedResponse(queryClient).toJsonString();
    }

    private String fetchAsCsvText(ResourceAwsClient client) {
        final var response = client.doSearch(this);
        return CsvTransformer.transform(response.getSearchHits());
    }

    PagedSearchResourceDto fetchAsPagedResponse(ResourceAwsClient client) {
        final var response = client.doSearch(this);
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
        requestParameter.remove(FROM.key());
        var sortedP =
            response.getSort().stream().map(Object::toString).collect(Collectors.joining(COMMA));
        requestParameter.put(SEARCH_AFTER.key(), sortedP);
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
                case SORT -> addSortQuery(value);
                case SORT_ORDER -> addSortOrderQuery(value);
                case PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setLucineValue(qpKey, expandDate(value));
                case CATEGORY, CONTRIBUTOR,
                    CREATED_BEFORE, CREATED_SINCE,
                    DOI, FUNDING, FUNDING_SOURCE, ID,
                    INSTITUTION, ISSN,
                    MODIFIED_BEFORE, MODIFIED_SINCE,
                    PROJECT_CODE, SEARCH_ALL, TITLE,
                    UNIT, USER, YEAR_REPORTED -> query.setLucineValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }

        private String expandDate(String value) {
            return value.length() == 4 ? value + "-01-01" : value;
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
                query.removeValue(PAGE);
            }
        }

        @Override
        protected void validateSort() throws BadRequestException {
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

        private static boolean notInvalid(String key) {
            return keyFromString(key) != ResourceParameterKey.INVALID;
        }

        private String validateSortKey(String keySort) {
            var sortKeyParts = keySort.split(COLON);
            if (sortKeyParts.length > EXPECTED_TWO_PARTS) {
                throw new IllegalArgumentException(INVALID_VALUE_WITH_SORT.formatted(keySort, validSortKeys()));
            }

            var sortOrder = getSortOrder(sortKeyParts);

            if (!sortOrder.matches(SORT_ORDER.pattern())) {
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

        private void addSortOrderQuery(String value) {
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), value));
        }

        private void addSortQuery(String value) {
            var validFieldValue =
                decodeUTF(value)
                    .replaceAll(PATTERN_IS_IGNORE_CASE + " (asc|desc)", ":$1");
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).as(), validFieldValue));
        }

        private String expandFields(String value) {
            return ALL.equals(value) || isNull(value)
                ? ALL
                : Arrays.stream(value.split(COMMA))
                .filter(Builder::notInvalid)
                .collect(Collectors.joining(COMMA));
        }
    }
}