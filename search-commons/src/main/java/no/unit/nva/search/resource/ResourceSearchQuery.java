package no.unit.nva.search.resource;

import static no.unit.nva.constants.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.constants.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.constants.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.constants.Words.HTTPS;
import static no.unit.nva.constants.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.constants.Words.NONE;
import static no.unit.nva.constants.Words.PI;
import static no.unit.nva.constants.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.constants.Words.SCOPUS_AS_TYPE;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.search.common.constant.Functions.trimSpace;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search.resource.Constants.CRISTIN_ORGANIZATION_PATH;
import static no.unit.nva.search.resource.Constants.CRISTIN_PERSON_PATH;
import static no.unit.nva.search.resource.Constants.EXCLUDED_FIELDS;
import static no.unit.nva.search.resource.Constants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search.resource.Constants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.resource.Constants.facetResourcePaths;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.CONTRIBUTOR;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.NODES_EXCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.NODES_INCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.NODES_SEARCHED;
import static no.unit.nva.search.resource.ResourceParameter.PAGE;
import static no.unit.nva.search.resource.ResourceParameter.RESOURCE_PARAMETER_SET;
import static no.unit.nva.search.resource.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;
import static no.unit.nva.search.resource.ResourceParameter.UNIDENTIFIED_NORWEGIAN;
import static no.unit.nva.search.resource.ResourceSort.INVALID;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;

import static org.opensearch.index.query.QueryBuilders.matchQuery;

import static java.lang.String.format;

import no.unit.nva.constants.Words;
import no.unit.nva.search.common.AsType;
import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.ParameterValidator;
import no.unit.nva.search.common.Query;
import no.unit.nva.search.common.SearchQuery;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.common.records.HttpResponseFormatter;

import nva.commons.core.JacocoGenerated;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ResourceSearchQuery is a query for searching resources.
 *
 * @author Stig Norland
 * @author Kir Truhacev
 * @author Sondre Vestad
 */
@SuppressWarnings("PMD.GodClass")
public final class ResourceSearchQuery extends SearchQuery<ResourceParameter> {

    private final ResourceStreamBuilders streamBuilders;
    private final ResourceAccessFilter filterBuilder;
    private final Map<String, String> additionalQueryParameters = new HashMap<>();
    private UserSettingsClient userSettingsClient;

    private ResourceSearchQuery() {
        super();
        assignStatusImpossibleWhiteList();
        setAlwaysExcludedFields(List.of(EXCLUDED_FIELDS));
        streamBuilders = new ResourceStreamBuilders(parameters());
        filterBuilder = new ResourceAccessFilter(this);
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the ResourceQuery from any forgetful developer (me)
     *
     * <p>i.e.In order to return any results, withRequiredStatus must be set
     */
    private void assignStatusImpossibleWhiteList() {
        filters()
                .set(
                        new TermsQueryBuilder(STATUS_KEYWORD, UUID.randomUUID().toString())
                                .queryName(STATUS));
    }

    public static ResourceParameterValidator builder() {
        return new ResourceParameterValidator();
    }

    @Override
    protected ResourceParameter keyFields() {
        return NODES_SEARCHED;
    }

    @Override
    public AsType<ResourceParameter> sort() {
        return parameters().get(SORT);
    }

    @Override
    protected String[] exclude() {
        return Stream.concat(
                        parameters().get(NODES_EXCLUDED).asSplitStream(COMMA),
                        getExcludedFields().stream())
                .distinct()
                .toArray(String[]::new);
    }

    @Override
    protected String[] include() {
        return parameters().get(NODES_INCLUDED).split(COMMA);
    }

    @Override
    protected ResourceParameter keyAggregation() {
        return AGGREGATION;
    }

    @Override
    protected ResourceParameter keySearchAfter() {
        return SEARCH_AFTER;
    }

    @Override
    protected ResourceParameter toKey(String keyName) {
        return ResourceParameter.keyFromString(keyName);
    }

    @Override
    protected SortKey toSortKey(String sortName) {
        return ResourceSort.fromSortKey(sortName);
    }

    @Override
    protected List<AggregationBuilder> builderAggregations() {
        return RESOURCES_AGGREGATIONS;
    }

    @JacocoGenerated // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<ResourceParameter, QueryBuilder>> builderCustomQueryStream(
            ResourceParameter key) {
        return switch (key) {
            case FUNDING -> streamBuilders.fundingQuery(key);
            case CRISTIN_IDENTIFIER ->
                    streamBuilders.additionalIdentifierQuery(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> streamBuilders.additionalIdentifierQuery(key, SCOPUS_AS_TYPE);
            case TOP_LEVEL_ORGANIZATION, UNIT, UNIT_NOT -> streamBuilders.subUnitIncludedQuery(key);
            case UNIDENTIFIED_NORWEGIAN -> streamBuilders.unIdentifiedNorwegians(key);
            case UNIDENTIFIED_CONTRIBUTOR_INSTITUTION ->
                    streamBuilders.unIdentifiedContributorOrInstitution(key);
            case SEARCH_ALL ->
                    streamBuilders.searchAllWithBoostsQuery(
                            mapOfPathAndBoost(parameters().get(NODES_SEARCHED)));
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    @Override
    public <R, Q extends Query<ResourceParameter>>
            HttpResponseFormatter<ResourceParameter> doSearch(OpenSearchClient<R, Q> queryClient) {
        if (parameters().isPresent(UNIDENTIFIED_NORWEGIAN)) {
            return super.doSearch(queryClient);
        }
        return super.doSearch(queryClient);
    }

    @Override
    protected AsType<ResourceParameter> from() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<ResourceParameter> size() {
        return parameters().get(SIZE);
    }

    @Override
    protected Map<String, String> facetPaths() {
        return facetResourcePaths;
    }

    @Override
    protected BoolQueryBuilder builderMainQuery() {
        var queryBuilder = super.builderMainQuery();
        if (isLookingForOneContributor()) {
            addPromotedPublications(this.userSettingsClient, queryBuilder);
        }
        return queryBuilder;
    }

    private boolean isLookingForOneContributor() {
        return parameters().get(CONTRIBUTOR).asSplitStream(COMMA).count() == 1;
    }

    private void addPromotedPublications(
            UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {

        var result = attempt(() -> userSettingsClient.doSearch(this));
        if (result.isSuccess()) {
            var promotedPublications = result.get().promotedPublications();
            for (int i = 0; i < promotedPublications.size(); i++) {
                var qb =
                        matchQuery(IDENTIFIER_KEYWORD, promotedPublications.get(i))
                                // 4.14 down to 3.14 (PI)
                                .boost(PI + 1F - ((float) i / promotedPublications.size()));
                bq.should(qb);
            }
        }
    }

    @Override
    public URI openSearchUri() {
        return fromUri(infrastructureApiUri)
                .addChild(Words.RESOURCES, Words.SEARCH)
                .addQueryParameters(queryParameters())
                .getUri();
    }

    private Map<String, String> queryParameters() {
        return additionalQueryParameters;
    }

    public ResourceAccessFilter withFilter() {
        return filterBuilder;
    }

    public ResourceSearchQuery withScrollTime(String time) {
        this.additionalQueryParameters.put("scroll", time);
        return this;
    }

    public ResourceSearchQuery withUserSettings(UserSettingsClient userSettingsClient) {
        this.userSettingsClient = userSettingsClient;
        return this;
    }

    /**
     * ResourceParameterValidator is a class that validates parameters for a ResourceSearchQuery.
     *
     * @author Stig Norland
     */
    public static class ResourceParameterValidator
            extends ParameterValidator<ResourceParameter, ResourceSearchQuery> {

        ResourceParameterValidator() {
            super(new ResourceSearchQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing()
                    .forEach(
                            key -> {
                                switch (key) {
                                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                                    case SORT -> setValue(key.name(), RELEVANCE_KEY_NAME);
                                    case AGGREGATION -> setValue(key.name(), NONE);
                                    default -> {
                                        /* ignore and continue */
                                    }
                                }
                            });
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (searchQuery.parameters().isPresent(PAGE)) {
                if (searchQuery.parameters().isPresent(FROM)) {
                    var page = searchQuery.parameters().get(PAGE).<Number>as();
                    var perPage = searchQuery.parameters().get(SIZE).<Number>as();
                    searchQuery
                            .parameters()
                            .set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                searchQuery.parameters().remove(PAGE);
            }
        }

        @Override
        protected Collection<String> validKeys() {
            return RESOURCE_PARAMETER_SET.stream().map(ResourceParameter::asLowerCase).toList();
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return ResourceParameter.keyFromString(keyName) != ResourceParameter.INVALID;
        }

        @Override
        protected void validateSortKeyName(String name) {
            var nameSort = name.split(COLON_OR_SPACE);
            if (nameSort.length == NAME_AND_SORT_LENGTH) {
                SortOrder.fromString(nameSort[1]);
            }
            if (nameSort.length > NAME_AND_SORT_LENGTH) {
                throw new IllegalArgumentException(TOO_MANY_ARGUMENTS + name);
            }

            if (ResourceSort.fromSortKey(nameSort[0]) == INVALID) {
                throw new IllegalArgumentException(
                        INVALID_VALUE_WITH_SORT.formatted(name, ResourceSort.validSortKeys()));
            }
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = ResourceParameter.keyFromString(key);
            var decodedValue = getDecodedValue(qpKey, value);
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case UNIT, UNIT_NOT, TOP_LEVEL_ORGANIZATION ->
                        mergeToKey(qpKey, identifierToCristinId(decodedValue));
                case CONTRIBUTOR, CONTRIBUTOR_NOT ->
                        mergeToKey(qpKey, identifierToCristinPersonId(decodedValue));
                case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION ->
                        searchQuery.parameters().set(qpKey, decodedValue);
                case NODES_SEARCHED ->
                        searchQuery.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        private String identifierToCristinId(String decodedValue) {
            return Arrays.stream(decodedValue.split(COMMA))
                    .map(value -> identifierToUri(value, CRISTIN_ORGANIZATION_PATH))
                    .collect(Collectors.joining(COMMA));
        }

        private String identifierToCristinPersonId(String decodedValue) {
            return Arrays.stream(decodedValue.split(COMMA))
                    .map(value -> identifierToUri(value, CRISTIN_PERSON_PATH))
                    .collect(Collectors.joining(COMMA));
        }

        private String currentHost() {
            return HTTPS + searchQuery.getNvaSearchApiUri().getHost();
        }

        private String identifierToUri(String decodedValue, String uriPath) {
            return isUriId(decodedValue)
                    ? decodedValue
                    : format("%s%s%s", currentHost(), uriPath, decodedValue);
        }

        private boolean isUriId(String decodedValue) {
            return decodedValue.startsWith(HTTPS);
        }
    }
}
