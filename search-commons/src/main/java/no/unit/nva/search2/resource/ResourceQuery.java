package no.unit.nva.search2.resource;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.FUNDINGS;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_TRUE;
import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.PI;
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.common.constant.Words.PUBLISHER;
import static no.unit.nva.search2.common.constant.Words.SCOPUS_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.SOURCE;
import static no.unit.nva.search2.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search2.common.constant.Words.SPACE;
import static no.unit.nva.search2.common.constant.Words.STATUS;
import static no.unit.nva.search2.common.constant.Words.VALUE;
import static no.unit.nva.search2.resource.Constants.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search2.resource.Constants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.resource.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search2.resource.Constants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search2.resource.Constants.facetResourcePaths;
import static no.unit.nva.search2.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search2.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search2.resource.ResourceParameter.CONTRIBUTOR;
import static no.unit.nva.search2.resource.ResourceParameter.EXCLUDE_SUBUNITS;
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
import static no.unit.nva.search2.resource.ResourceSort.fromSortKey;
import static no.unit.nva.search2.resource.ResourceSort.sortToJson;
import static no.unit.nva.search2.resource.ResourceSort.validSortKeys;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.PublicationStatus;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import no.unit.nva.search2.common.records.UserSettings;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.common.xcontent.DeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortOrder;

@SuppressWarnings("PMD.GodClass")
public final class ResourceQuery extends Query<ResourceParameter> {

    public static ResourceParameterValidator builder() {
        return new ResourceParameterValidator();
    }

    private ResourceQuery() {
        super();
        assignStatusImpossibleWhiteList();
    }

    @Override
    protected Stream<Entry<ResourceParameter, QueryBuilder>> customQueryBuilders(ResourceParameter key) {
        return switch (key) {
            case FUNDING -> fundingQuery(key);
            case CRISTIN_IDENTIFIER -> additionalIdentifierQuery(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> additionalIdentifierQuery(key, SCOPUS_AS_TYPE);
            case TOP_LEVEL_ORGANIZATION, UNIT -> subUnitIncluded(key);
            case SEARCH_ALL -> multiMatchQueryStream();
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    @Override
    protected ResourceParameter keyFromString(String keyName) {
        return ResourceParameter.keyFromString(keyName);
    }

    private Stream<Entry<ResourceParameter, QueryBuilder>> subUnitIncluded(ResourceParameter key) {
        var query =
            getValue(EXCLUDE_SUBUNITS).asBoolean()
                ? termQuery(getTermPath(key), getValue(key).as())
                : termQuery(getMatchPath(key), getValue(key).as());

        return queryTools.queryToEntry(key, query);
    }

    private String getTermPath(ResourceParameter key) {
        return key.searchFields(KEYWORD_TRUE).findFirst().orElseThrow();
    }

    private String getMatchPath(ResourceParameter key) {
        return key.searchFields(KEYWORD_TRUE).skip(1).findFirst().orElseThrow();
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
    public AsType<ResourceParameter> getSort() {
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

    @Override
    protected Map<String, String> aggregationsDefinition() {
        return facetResourcePaths;
    }

    /**
     * Filter on Required Status.
     *
     * <p>Only STATUES specified here will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     * <p>See {@link PublicationStatus} for available values.</p>
     *
     * @param publicationStatus the required statues
     * @return ResourceQuery (builder pattern)
     */
    public ResourceQuery withRequiredStatus(PublicationStatus... publicationStatus) {
        final var values = Arrays.stream(publicationStatus)
            .map(PublicationStatus::toString)
            .toArray(String[]::new);
        final var filter = new TermsQueryBuilder(STATUS_KEYWORD, values)
            .queryName(STATUS);
        this.addFilter(filter);
        return this;
    }

    /**
     * Filter on organization.
     * <P>Only documents belonging to organization specified are searchable (for the user)
     * </p>
     *
     * @param organization uri of publisher
     * @return ResourceQuery (builder pattern)
     */
    public ResourceQuery withOrganization(URI organization) {
        final var filter = new TermQueryBuilder(PUBLISHER_ID_KEYWORD, organization.toString())
            .queryName(PUBLISHER);
        this.addFilter(filter);
        return this;
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream(UserSettingsClient userSettingsClient) {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : makeBoolQuery(userSettingsClient);

        var builder = defaultSearchSourceBuilder(queryBuilder);

        handleSearchAfter(builder);

        getSortBuilder().forEach(builder::sort);

        if (getMediaType().is(JSON_UTF_8)) {
            builder.aggregation(getAggregationsWithFilter());
        }
        logger.debug(builder.toString());

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    private BoolQueryBuilder makeBoolQuery(UserSettingsClient userSettingsClient) {
        var queryBuilder = mainQuery();
        if (isLookingForOneContributor()) {
            addPromotedPublications(userSettingsClient, queryBuilder);
        }
        return queryBuilder;
    }

    private Stream<Entry<ResourceParameter, QueryBuilder>> multiMatchQueryStream() {
        var fields = fieldsToKeyNames(getValue(FIELDS));
        var sevenValues = getValue(SEARCH_ALL).asSplitStream(SPACE)
            .limit(7)
            .collect(Collectors.joining(SPACE));
        var fifteenValues = getValue(SEARCH_ALL).asSplitStream(SPACE)
            .limit(15)
            .collect(Collectors.joining(SPACE));

        var query = boolQuery()
            .queryName(SEARCH_ALL.asCamelCase())
            .must(QueryBuilders.multiMatchQuery(sevenValues)
                      .fields(fields)
                      .type(Type.CROSS_FIELDS)
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

    private FilterAggregationBuilder getAggregationsWithFilter() {
        var aggregationFilter = AggregationBuilders.filter(POST_FILTER, getFilters());
        RESOURCES_AGGREGATIONS
            .stream().filter(this::isRequestedAggregation)
            .forEach(aggregationFilter::subAggregation);
        return aggregationFilter;
    }

    private boolean isRequestedAggregation(AggregationBuilder aggregationBuilder) {
        return Optional.ofNullable(aggregationBuilder)
            .map(AggregationBuilder::getName)
            .map(this::isDefined)
            .orElse(false);
    }

    private boolean isDefined(String keyName) {
        return getValue(AGGREGATION)
            .asSplitStream(COMMA)
            .anyMatch(name -> name.equalsIgnoreCase(ALL) || name.equalsIgnoreCase(keyName));
    }

    private String getSortFieldName(Entry<String, SortOrder> entry) {
        return fromSortKey(entry.getKey()).jsonPath();
    }

    protected List<SortBuilder<?>> getSortBuilder() {
        try (XContentParser parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY,
                                                                            DeprecationHandler.IGNORE_DEPRECATIONS,
                                                                            sortToJson(getSort().as()))) {
            parser.nextToken();  // Flytter til første token for å starte parsing
            return FieldSortBuilder.fromXContent(parser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = removeKey(SEARCH_AFTER).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
    }

    private boolean isLookingForOneContributor() {
        return hasOneValue(CONTRIBUTOR);
    }

    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {
        var promotedPublications =
            attempt(() -> userSettingsClient.doSearch(this))
                .or(() -> new UserSettings(List.of()))
                .get().promotedPublications();
        if (hasContent(promotedPublications)) {
            removeKey(SORT);  // remove sort to avoid messing up "sorting by score"
            for (int i = 0; i < promotedPublications.size(); i++) {
                var sortableIdentifier = fromUri(promotedPublications.get(i)).getLastPathElement();
                var qb = matchQuery(IDENTIFIER_KEYWORD, sortableIdentifier)
                    .boost(PI + 1F - ((float) i / promotedPublications.size()));  // 4.14 down to 3.14 (PI)
                bq.should(qb);
            }
            logger.info(
                bq.should().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "))
            );
        }
    }


    public Stream<Entry<ResourceParameter, QueryBuilder>> fundingQuery(ResourceParameter key) {
        final var values = getValue(key).split(COLON);
        var query = QueryBuilders.nestedQuery(
            FUNDINGS,
            boolQuery()
                .must(termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                .must(termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
            ScoreMode.None);

        return queryTools.queryToEntry(key, query);
    }

    public Stream<Entry<ResourceParameter, QueryBuilder>> additionalIdentifierQuery(
        ResourceParameter key, String source) {
        var value = getValue(key).toString();
        var query = QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), value))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

        return queryTools.queryToEntry(key, query);
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the ResourceQuery from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withRequiredStatus must be set </p>
     * <p>See {@link #withRequiredStatus(PublicationStatus...)} for the correct way to filter by status</p>
     */
    private void assignStatusImpossibleWhiteList() {
        setFilters(new TermsQueryBuilder(STATUS_KEYWORD, UUID.randomUUID().toString()).queryName(STATUS));
    }

    public static class ResourceParameterValidator extends ParameterValidator<ResourceParameter, ResourceQuery> {

        ResourceParameterValidator() {
            super(new ResourceQuery());
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return ResourceParameter.keyFromString(keyName) != ResourceParameter.INVALID;
        }

        @Override
        protected boolean isAggregationValid(String aggregationName) {
            return
                ALL.equalsIgnoreCase(aggregationName) ||
                NONE.equalsIgnoreCase(aggregationName) ||
                RESOURCES_AGGREGATIONS.stream()
                    .anyMatch(builder -> builder.getName().equalsIgnoreCase(aggregationName));
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.name(), DEFAULT_RESOURCE_SORT + COLON + DEFAULT_SORT_ORDER);
                    case AGGREGATION -> setValue(key.name(), ALL);
                    default -> { /* ignore and continue */ }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = ResourceParameter.keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setKeyValue(qpKey, decodedValue);
                case FIELDS -> query.setKeyValue(qpKey, ignoreInvalidFields(decodedValue));
                case AGGREGATION -> query.setKeyValue(qpKey, ignoreInvalidAggregations(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case PUBLICATION_LANGUAGE, PUBLICATION_LANGUAGE_NOT,
                    PUBLICATION_LANGUAGE_SHOULD -> query.setKeyValue(qpKey, expandLanguage(decodedValue));
                case CREATED_BEFORE, CREATED_SINCE,
                    MODIFIED_BEFORE, MODIFIED_SINCE,
                    PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.setKeyValue(qpKey, expandYearToDate(decodedValue));
                case LANG -> { /* ignore and continue */ }
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        private String expandLanguage(String decodedValue) {
            var startIndex = decodedValue.length() - 3;
            return Constants.LEXVO_ORG_ID_ISO_639_3 + decodedValue.substring(startIndex);
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
                throw new IllegalArgumentException(
                    INVALID_VALUE_WITH_SORT.formatted(entry.getKey(), validSortKeys())
                );
            }
            attempt(entry::getValue)
                .orElseThrow(e -> new IllegalArgumentException(e.getException().getMessage()));
        }

        @Override
        protected Collection<String> validKeys() {
            return RESOURCE_PARAMETER_SET.stream()
                .map(Enum::name)
                .toList();
        }
    }
}