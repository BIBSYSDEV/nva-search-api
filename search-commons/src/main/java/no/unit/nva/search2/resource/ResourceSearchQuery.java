package no.unit.nva.search2.resource;

import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search2.common.constant.Functions.expandYearToDate;
import static no.unit.nva.search2.common.constant.Functions.trimSpace;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.PI;
import static no.unit.nva.search2.common.constant.Words.SCOPUS_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.SPACE;
import static no.unit.nva.search2.common.constant.Words.STATUS;
import static no.unit.nva.search2.resource.Constants.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search2.resource.Constants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.resource.Constants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search2.resource.Constants.facetResourcePaths;
import static no.unit.nva.search2.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search2.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search2.resource.ResourceParameter.CONTRIBUTOR;
import static no.unit.nva.search2.resource.ResourceParameter.FIELDS;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.PAGE;
import static no.unit.nva.search2.resource.ResourceParameter.RESOURCE_PARAMETER_SET;
import static no.unit.nva.search2.resource.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search2.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import static no.unit.nva.search2.resource.ResourceParameter.SORT;
import static no.unit.nva.search2.resource.ResourceParameter.SORT_ORDER;
import static no.unit.nva.search2.resource.ResourceParameter.TITLE;
import static no.unit.nva.search2.resource.ResourceSort.INVALID;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.ResourceCsvTransformer;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.SearchQuery;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.SortKey;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

@SuppressWarnings("PMD.GodClass")
public final class ResourceSearchQuery extends SearchQuery<ResourceParameter> {

    private UserSettingsClient userSettingsClient;
    private final ResourceStreamBuilders streamBuilders;
    private final ResourceFilter filterBuilder;
    private boolean useCsvFieldsAsSource;
    private final Map<String,String> additionalQueryParameters = new HashMap<>();

    private ResourceSearchQuery() {
        super();
        assignStatusImpossibleWhiteList();
        streamBuilders = new ResourceStreamBuilders(this.queryTools, parameters());
        filterBuilder = new ResourceFilter(this);
    }

    public static ResourceParameterValidator builder() {
        return new ResourceParameterValidator();
    }

    @Override
    protected ResourceParameter keyAggregation() {
        return AGGREGATION;
    }

    @Override
    protected ResourceParameter keyFields() {
        return FIELDS;
    }

    @Override
    protected ResourceParameter keySearchAfter() {
        return SEARCH_AFTER;
    }

    @Override
    protected ResourceParameter keySortOrder() {
        return SORT_ORDER;
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
    protected AsType<ResourceParameter> getFrom() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<ResourceParameter> getSize() {
        return parameters().get(SIZE);
    }

    @Override
    public AsType<ResourceParameter> getSort() {
        return parameters().get(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(Words.RESOURCES, Words.SEARCH)
                .addQueryParameters(getQueryParameters())
                .getUri();
    }

    private Map<String, String> getQueryParameters() {
        return additionalQueryParameters;
    }

    @Override
    protected String toCsvText(SwsResponse response) {
        return ResourceCsvTransformer.transform(response.getSearchHits());
    }

    @Override
    protected void setFetchSource(SearchSourceBuilder builder) {
        if (this.useCsvFieldsAsSource) {
            builder.fetchSource(ResourceCsvTransformer.getJsonFields().toArray(String[]::new), null);
        } else {
            builder.fetchSource(true);
        }
    }

    @Override
    protected Map<String, String> facetPaths() {
        return facetResourcePaths;
    }

    @Override
    protected List<AggregationBuilder> builderAggregations() {
        return RESOURCES_AGGREGATIONS;
    }

    @Override
    protected BoolQueryBuilder builderMainQuery() {
        var queryBuilder = super.builderMainQuery();
        if (isLookingForOneContributor()) {
            addPromotedPublications(this.userSettingsClient, queryBuilder);
        }
        return queryBuilder;
    }

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<ResourceParameter, QueryBuilder>> builderStreamCustomQuery(ResourceParameter key) {
        return switch (key) {
            case FUNDING -> streamBuilders.fundingQuery(key);
            case CRISTIN_IDENTIFIER -> streamBuilders.additionalIdentifierQuery(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> streamBuilders.additionalIdentifierQuery(key, SCOPUS_AS_TYPE);
            case TOP_LEVEL_ORGANIZATION, UNIT -> streamBuilders.subUnitIncludedQuery(key);
            case SEARCH_ALL -> builderStreamSearchAllWithBoostsQuery();
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    public ResourceFilter withFilter() {
        return filterBuilder;
    }

    public ResourceSearchQuery withOnlyCsvFields() {
        this.useCsvFieldsAsSource = true;
        return this;
    }

    public ResourceSearchQuery withFixedRange(int from, int size) {
        this.parameters().set(FROM, String.valueOf(from));
        this.parameters().set(SIZE, String.valueOf(size));
        return this;
    }

    public ResourceSearchQuery withoutAggregation() {
        this.parameters().set(AGGREGATION, NONE);
        return this;
    }

    public ResourceSearchQuery withScrollTime(String time) {
        this.additionalQueryParameters.put("scroll", time);
        return this;
    }

    public ResourceSearchQuery withUserSettings(UserSettingsClient userSettingsClient) {
        this.userSettingsClient = userSettingsClient;
        return this;
    }

    private Stream<Map.Entry<ResourceParameter, QueryBuilder>> builderStreamSearchAllWithBoostsQuery() {
        var fields = fieldsToKeyNames(parameters().get(FIELDS));
        var sevenValues = parameters().get(SEARCH_ALL).asSplitStream(SPACE)
            .limit(7)
            .collect(Collectors.joining(SPACE));
        var fifteenValues = parameters().get(SEARCH_ALL).asSplitStream(SPACE)
            .limit(15)
            .collect(Collectors.joining(SPACE));

        var query = boolQuery()
            .queryName(SEARCH_ALL.asCamelCase())
            .must(QueryBuilders.multiMatchQuery(sevenValues)
                .fields(fields)
                .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                .operator(Operator.AND));

        if (fields.containsKey(ENTITY_DESCRIPTION_MAIN_TITLE) || fields.containsKey(ASTERISK)) {
            query.should(
                matchPhrasePrefixQuery(ENTITY_DESCRIPTION_MAIN_TITLE, fifteenValues).boost(TITLE.fieldBoost())
            );
        }
        if (fields.containsKey(ENTITY_ABSTRACT) || fields.containsKey(ASTERISK)) {
            query.should(matchPhraseQuery(ENTITY_ABSTRACT, fifteenValues).boost(ABSTRACT.fieldBoost()));
        }
        return queryTools.queryToEntry(SEARCH_ALL, query);
    }

    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {

        var result = attempt(() -> userSettingsClient.doSearch(this));
        if (result.isSuccess()) {
            parameters().remove(SORT);  // remove sort to avoid messing up "sorting by score"
            var promotedPublications = result.get().promotedPublications();
            for (int i = 0; i < promotedPublications.size(); i++) {
                var qb = matchQuery(IDENTIFIER_KEYWORD, promotedPublications.get(i))
                    .boost(PI + 1F - ((float) i / promotedPublications.size()));  // 4.14 down to 3.14 (PI)
                bq.should(qb);
            }
        }
    }

    private boolean isLookingForOneContributor() {
        return parameters().get(CONTRIBUTOR)
            .asSplitStream(COMMA)
            .count() == 1;
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the ResourceQuery from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withRequiredStatus must be set </p>
     */
    private void assignStatusImpossibleWhiteList() {
        filters.set(new TermsQueryBuilder(STATUS_KEYWORD, UUID.randomUUID().toString()).queryName(STATUS));
    }

    public static class ResourceParameterValidator extends ParameterValidator<ResourceParameter, ResourceSearchQuery> {

        ResourceParameterValidator() {
            super(new ResourceSearchQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.name(), DEFAULT_RESOURCE_SORT + COLON + DEFAULT_SORT_ORDER);
                    case AGGREGATION -> setValue(key.name(), NONE);
                    default -> { /* ignore and continue */ }
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
                    searchQuery.parameters().set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                searchQuery.parameters().remove(PAGE);
            }
        }

        @Override
        protected Collection<String> validKeys() {
            return RESOURCE_PARAMETER_SET.stream()
                .map(ResourceParameter::asLowerCase)
                .toList();
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
                    INVALID_VALUE_WITH_SORT.formatted(name, ResourceSort.validSortKeys())
                );
            }
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = ResourceParameter.keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION -> searchQuery.parameters().set(qpKey, decodedValue);
                case FIELDS -> searchQuery.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case PUBLICATION_LANGUAGE, PUBLICATION_LANGUAGE_NOT,
                     PUBLICATION_LANGUAGE_SHOULD -> searchQuery.parameters().set(qpKey, expandLanguage(decodedValue));
                case CREATED_BEFORE, CREATED_SINCE,
                     MODIFIED_BEFORE, MODIFIED_SINCE,
                     PUBLISHED_BEFORE, PUBLISHED_SINCE -> searchQuery.parameters().set(qpKey, expandYearToDate(decodedValue));
                case LANG -> { /* ignore and continue */ }
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return ResourceParameter.keyFromString(keyName) != ResourceParameter.INVALID;
        }

        private String expandLanguage(String decodedValue) {
            var startIndex = decodedValue.length() - 3;
            return Constants.LEXVO_ORG_ID_ISO_639_3 + decodedValue.substring(startIndex);
        }
    }
}