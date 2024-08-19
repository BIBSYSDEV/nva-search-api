package no.unit.nva.search.parentchild;

import no.unit.nva.search.common.AsType;
import no.unit.nva.search.common.ParameterValidator;
import no.unit.nva.search.common.SearchQuery;
import no.unit.nva.search.common.constant.Words;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.common.enums.ValueEncoding;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.resource.ResourceStreamBuilders;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static no.unit.nva.search.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search.common.constant.ErrorMessages.TOO_MANY_ARGUMENTS;
import static no.unit.nva.search.common.constant.Functions.decodeUTF;
import static no.unit.nva.search.common.constant.Functions.trimSpace;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search.common.constant.Words.COMMA;
import static no.unit.nva.search.common.constant.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.search.common.constant.Words.HTTPS;
import static no.unit.nva.search.common.constant.Words.NAME_AND_SORT_LENGTH;
import static no.unit.nva.search.common.constant.Words.NONE;
import static no.unit.nva.search.common.constant.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search.common.constant.Words.SCOPUS_AS_TYPE;
import static no.unit.nva.search.resource.Constants.CRISTIN_ORGANIZATION_PATH;
import static no.unit.nva.search.resource.Constants.CRISTIN_PERSON_PATH;
import static no.unit.nva.search.resource.Constants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.resource.Constants.facetResourcePaths;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.NODES_EXCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.NODES_INCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.NODES_SEARCHED;
import static no.unit.nva.search.resource.ResourceParameter.PAGE;
import static no.unit.nva.search.resource.ResourceParameter.RESOURCE_PARAMETER_SET;
import static no.unit.nva.search.resource.ResourceParameter.SEARCH_AFTER;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;
import static no.unit.nva.search.resource.ResourceSort.INVALID;
import static nva.commons.core.paths.UriWrapper.fromUri;

/**
 * @author Stig Norland
 * @author Kir Truhacev
 * @author Sondre Vestad
 */
@SuppressWarnings("PMD.GodClass")
public final class ResourceParentChildSearchQuery extends SearchQuery<ResourceParameter> {

    private final ResourceStreamBuilders streamBuilders;
    private final Map<String, String> additionalQueryParameters = new HashMap<>();

    private ResourceParentChildSearchQuery() {
        super();
        streamBuilders = new ResourceStreamBuilders(parameters());
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
        return NODES_SEARCHED;
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
    protected AsType<ResourceParameter> from() {
        return parameters().get(FROM);
    }

    @Override
    protected AsType<ResourceParameter> size() {
        return parameters().get(SIZE);
    }

    @Override
    public AsType<ResourceParameter> sort() {
        return parameters().get(SORT);
    }

    @Override
    protected String[] exclude() {
        return parameters().get(NODES_EXCLUDED).split(COMMA);
    }

    @Override
    protected String[] include() {
        return parameters().get(NODES_INCLUDED).split(COMMA);
    }

    @Override
    public URI openSearchUri() {
        return
            fromUri(infrastructureApiUri)
                .addChild(Words.RESOURCES_PC, Words.SEARCH)
                .addQueryParameters(queryParameters())
                .getUri();
    }

    private Map<String, String> queryParameters() {
        return additionalQueryParameters;
    }

    @Override
    protected Map<String, String> facetPaths() {
        return facetResourcePaths;
    }

    @Override
    protected List<AggregationBuilder> builderAggregations() {
        return RESOURCES_AGGREGATIONS;
    }


    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<ResourceParameter, QueryBuilder>> builderCustomQueryStream(ResourceParameter key) {
        return switch (key) {
            case FUNDING -> streamBuilders.fundingQuery(key);
            case CRISTIN_IDENTIFIER -> streamBuilders.additionalIdentifierQuery(key, CRISTIN_AS_TYPE);
            case SCOPUS_IDENTIFIER -> streamBuilders.additionalIdentifierQuery(key, SCOPUS_AS_TYPE);
            case TOP_LEVEL_ORGANIZATION, UNIT, UNIT_NOT -> streamBuilders.subUnitIncludedQuery(key);
            case SEARCH_ALL ->
                streamBuilders.searchAllWithBoostsQuery(fieldsToKeyNames(parameters().get(NODES_SEARCHED)));
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }


    public static class ResourceParameterValidator extends ParameterValidator<ResourceParameter, ResourceParentChildSearchQuery> {


        ResourceParameterValidator() {
            super(new ResourceParentChildSearchQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.name(), RELEVANCE_KEY_NAME);
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
                case UNIT, UNIT_NOT, TOP_LEVEL_ORGANIZATION -> mergeToKey(qpKey, identifierToCristinId(decodedValue));
                case CONTRIBUTOR, CONTRIBUTOR_NOT -> mergeToKey(qpKey, identifierToCristinPersonId(decodedValue));
                case SEARCH_AFTER, FROM, SIZE, PAGE, AGGREGATION -> searchQuery.parameters().set(qpKey, decodedValue);
                case NODES_SEARCHED -> searchQuery.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
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

        @Override
        protected boolean isKeyValid(String keyName) {
            return ResourceParameter.keyFromString(keyName) != ResourceParameter.INVALID;
        }

    }
}