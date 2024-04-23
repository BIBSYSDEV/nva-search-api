package no.unit.nva.search2.resource;

import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search2.common.constant.Words.FUNDINGS;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_TRUE;
import static no.unit.nva.search2.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.PI;
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
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.PublicationStatus;
import no.unit.nva.search2.common.enums.SortKey;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.UserSettings;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

@SuppressWarnings("PMD.GodClass")
public final class ResourceQuery extends Query<ResourceParameter> {

    private UserSettingsClient userSettingsClient;

    private ResourceQuery() {
        super();
        assignStatusImpossibleWhiteList();
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the ResourceQuery from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withRequiredStatus must be set </p>
     * <p>See {@link #withRequiredStatus(PublicationStatus...)} for the correct way to filter by status</p>
     */
    private void assignStatusImpossibleWhiteList() {
        filters.set(new TermsQueryBuilder(STATUS_KEYWORD, UUID.randomUUID().toString()).queryName(STATUS));
    }

    public static ResourceParameterValidator builder() {
        return new ResourceParameterValidator();
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
        this.filters.add(filter);
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
        this.filters.add(filter);
        return this;
    }

    public ResourceQuery withUserSettings(UserSettingsClient userSettingsClient) {
        this.userSettingsClient = userSettingsClient;
        return this;
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
                .getUri();
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
            case FUNDING -> builderStreamFundingQuery(key);
            case CRISTIN_IDENTIFIER -> builderStreamAdditionalIdentifierQuery(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> builderStreamAdditionalIdentifierQuery(key, SCOPUS_AS_TYPE);
            case TOP_LEVEL_ORGANIZATION, UNIT -> builderStreamSubUnitIncludedQuery(key);
            case SEARCH_ALL -> builderStreamSearchAllWithBoostsQuery();
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    private Stream<Entry<ResourceParameter, QueryBuilder>> builderStreamAdditionalIdentifierQuery(
        ResourceParameter key, String source) {
        var query = QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            boolQuery()
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), parameters().get(key).as()))
                .must(termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None);

        return queryTools.queryToEntry(key, query);
    }

    private Stream<Entry<ResourceParameter, QueryBuilder>> builderStreamFundingQuery(ResourceParameter key) {
        var values = parameters().get(key).split(COLON);
        var query = QueryBuilders.nestedQuery(
            FUNDINGS,
            boolQuery()
                .must(termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                .must(termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
            ScoreMode.None);
        return queryTools.queryToEntry(key, query);
    }

    private Stream<Entry<ResourceParameter, QueryBuilder>> builderStreamSearchAllWithBoostsQuery() {
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

    private Stream<Entry<ResourceParameter, QueryBuilder>> builderStreamSubUnitIncludedQuery(ResourceParameter key) {
        var query =
            parameters().get(EXCLUDE_SUBUNITS).asBoolean()
                ? termQuery(extractTermPath(key), parameters().get(key).as())
                : termQuery(extractMatchPath(key), parameters().get(key).as());

        return queryTools.queryToEntry(key, query);
    }

    private String extractTermPath(ResourceParameter key) {
        return key.searchFields(KEYWORD_TRUE).findFirst().orElseThrow();
    }

    private String extractMatchPath(ResourceParameter key) {
        return key.searchFields(KEYWORD_TRUE).skip(1).findFirst().orElseThrow();
    }

    private boolean isLookingForOneContributor() {
        return parameters().get(CONTRIBUTOR)
            .asSplitStream(COMMA)
            .count() == 1;
    }

    private void addPromotedPublications(UserSettingsClient userSettingsClient, BoolQueryBuilder bq) {
        var promotedPublications =
            attempt(() -> userSettingsClient.doSearch(this))
                .or(() -> new UserSettings(List.of()))
                .get().promotedPublications();
        if (hasContent(promotedPublications)) {
            parameters().remove(SORT);  // remove sort to avoid messing up "sorting by score"
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

    public static class ResourceParameterValidator extends ParameterValidator<ResourceParameter, ResourceQuery> {

        ResourceParameterValidator() {
            super(new ResourceQuery());
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
            if (query.parameters().isPresent(PAGE)) {
                if (query.parameters().isPresent(FROM)) {
                    var page = query.parameters().get(PAGE).<Number>as();
                    var perPage = query.parameters().get(SIZE).<Number>as();
                    query.parameters().set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.parameters().remove(PAGE);
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
                case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION -> query.parameters().set(qpKey, decodedValue);
                case FIELDS -> query.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case PUBLICATION_LANGUAGE, PUBLICATION_LANGUAGE_NOT,
                     PUBLICATION_LANGUAGE_SHOULD -> query.parameters().set(qpKey, expandLanguage(decodedValue));
                case CREATED_BEFORE, CREATED_SINCE,
                     MODIFIED_BEFORE, MODIFIED_SINCE,
                     PUBLISHED_BEFORE, PUBLISHED_SINCE -> query.parameters().set(qpKey, expandYearToDate(decodedValue));
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