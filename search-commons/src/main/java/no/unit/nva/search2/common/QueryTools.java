package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NO_FILES;
import static no.unit.nva.search2.common.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search2.common.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.common.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTORS_PART_OFS;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.common.constant.Words.FUNDINGS;
import static no.unit.nva.search2.common.constant.Words.HAS_PUBLIC_FILE;
import static no.unit.nva.search2.common.constant.Words.HAS_PUBLIC_FILE_KEY_NAME;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.ONE;
import static no.unit.nva.search2.common.constant.Words.PUBLISHED_FILE;
import static no.unit.nva.search2.common.constant.Words.SOURCE;
import static no.unit.nva.search2.common.constant.Words.SOURCE_NAME;
import static no.unit.nva.search2.common.constant.Words.TYPE;
import static no.unit.nva.search2.common.constant.Words.VALUE;
import static no.unit.nva.search2.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search2.common.enums.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.common.enums.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.common.enums.ParameterKind.FUZZY_KEYWORD;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.StringUtils.isEmpty;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ParameterKind;
import no.unit.nva.search2.resource.ResourceParameter;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.sort.SortOrder;

public final class QueryTools<K extends Enum<K> & ParameterKey> {

    /**
     * '1', 'true' 'True' -> true any other value -> False.
     *
     * @param value a string that is expected to be 1/true or 0/false
     * @return Boolean because we need the text 'true' or 'false'
     */
    public static Boolean valueToBoolean(String keyName, String value) {
        if (keyName.matches(PATTERN_IS_NO_FILES)) {
            return Boolean.FALSE;
        }
        if (ONE.equals(value) || PUBLISHED_FILE.equals(value) || HAS_PUBLIC_FILE.equals(value) || isEmpty(value)) {
            return Boolean.TRUE;
        }
        return Boolean.parseBoolean(value);
    }

    public static boolean hasContent(String value) {
        return nonNull(value) && !value.isEmpty();
    }

//    @JacocoGenerated    // used by PromotedPublication, which is not tested here.
    public static boolean hasContent(Collection<?> value) {
        return nonNull(value) && !value.isEmpty();
    }

    public static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public String getFirstSearchField(K key) {
        return getSearchFields(key)[0];
    }

    public String[] getSearchFields(K key) {
        return key.searchFields().stream()
            .map(String::trim)
            .map(trimmed -> isNotKeyword(key)
                ? trimmed.replace(DOT + KEYWORD, EMPTY_STRING)
                : trimmed)
            .toArray(String[]::new);
    }

    public static Entry<String, SortOrder> objectToSortEntry(Object sortString) {
        return stringsToSortEntry(sortString.toString().split(COLON_OR_SPACE));
    }

    public static Entry<String, SortOrder> stringsToSortEntry(String... strings) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return strings[0];
            }

            @Override
            public SortOrder getValue() {
                final var orderString = attempt(() -> strings[1])
                    .orElse((f) -> DEFAULT_SORT_ORDER);
                return SortOrder.fromString(orderString);
            }

            @Override
            @JacocoGenerated
            public SortOrder setValue(SortOrder value) {
                return null;
            }
        };
    }

    public Stream<Map.Entry<K, QueryBuilder>> queryToEntry(K key, QueryBuilder qb) {
        final var entry = new Map.Entry<K, QueryBuilder>() {
            @Override
            public K getKey() {
                return key;
            }

            @Override
            public QueryBuilder getValue() {
                return qb;
            }

            @Override
            @JacocoGenerated
            public QueryBuilder setValue(QueryBuilder value) {
                return null;
            }
        };
        return Stream.of(entry);
    }

    public Stream<Entry<K, QueryBuilder>> boolQuery(K key, String value) {
        return queryToEntry(
            key, QueryBuilders.termQuery(getFirstSearchField(key), Boolean.valueOf(value))
        );
    }

    public Stream<Entry<K, QueryBuilder>> fundingQuery(K key, String value) {
        final var values = value.split(COLON);
        return queryToEntry(
            key,
            QueryBuilders.nestedQuery(
                FUNDINGS,
                QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery(jsonPath(FUNDINGS, IDENTIFIER, KEYWORD), values[1]))
                    .must(QueryBuilders.termQuery(jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD), values[0])),
                ScoreMode.None));
    }

    public Stream<Entry<K, QueryBuilder>> additionalIdentifierQuery(K key, String value, String source) {
        return queryToEntry(key, QueryBuilders.nestedQuery(
            ADDITIONAL_IDENTIFIERS,
            QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, VALUE, KEYWORD), value))
                .must(QueryBuilders.termQuery(jsonPath(ADDITIONAL_IDENTIFIERS, SOURCE_NAME, KEYWORD), source)),
            ScoreMode.None));
    }

    public Stream<Entry<K, QueryBuilder>> createSubunitsQuery(Map<K, String> searchParameters, K key) {
        var viewingScope = getViewingScope(searchParameters);
        if (viewingScopeIsProvided(viewingScope)) {
            var shouldExcludeSubunits = getExcludeSubunitsKey(searchParameters);
            return queryToEntry(key, createSubunitQuery(shouldExcludeSubunits, viewingScope));
        } else {
            return null;
        }
    }

    private static QueryBuilder createSubunitQuery(Boolean shouldExcludeSubunits, List<String> viewingScope) {
        return shouldExcludeSubunits ? excludeSubunitsQuery(viewingScope) : includeSubunitsQuery(viewingScope);
    }

    private static boolean viewingScopeIsProvided(List<String> viewingScope) {
        return !viewingScope.isEmpty();
    }

    private List<String> getViewingScope(Map<K, String> searchParameterKeys) {
        var viewingScopeKeys = getViewingScopeParameters(searchParameterKeys);
        var viewingScopeParams = viewingScopeKeys.stream().map(searchParameterKeys::get).collect(Collectors.joining(","));
        return !viewingScopeKeys.isEmpty()
                   ? extractViewingScope(viewingScopeParams)
                   : List.of();
    }

    private List<K> getViewingScopeParameters(Map<K, String> searchParameterKeys) {
        return searchParameterKeys.keySet().stream()
                   .filter(this::isOrganization)
                   .toList();
    }

    public boolean isOrganization(K key) {
        return ResourceParameter.TOP_LEVEL_ORGANIZATION.name().equals(key.name())
               || ResourceParameter.UNIT.name().equals(key.name());
    }

    private static List<String> extractViewingScope(String viewingScopeParameter) {
        return Arrays.stream(viewingScopeParameter.split(COMMA))
                   .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                   .toList();
    }

    private Boolean getExcludeSubunitsKey(Map<K, String> searchParameterKeys) {
        var excludeSubunitValue = searchParameterKeys.keySet().stream()
                                    .filter(key -> ResourceParameter.EXCLUDE_SUBUNITS.name().equals(key.name()))
                                    .findFirst()
                                    .map(searchParameterKeys::get)
                                    .orElse(null);
        return Boolean.parseBoolean(excludeSubunitValue);
    }

    private static QueryBuilder includeSubunitsQuery(List<String> viewingScope) {
        var query = QueryBuilders.boolQuery();
        query.should(QueryBuilders.termsQuery(jsonPath(CONTRIBUTORS_PART_OFS, KEYWORD), viewingScope));
        query.should(QueryBuilders.termsQuery(jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD),
                                              viewingScope));
        return query;
    }

    private static QueryBuilder excludeSubunitsQuery(List<String> viewingScope) {
        return QueryBuilders.termsQuery(jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD),
                                        viewingScope);
    }

    public boolean isPublicFile(K key) {
        return HAS_PUBLIC_FILE_KEY_NAME.equals(key.name());
    }

    public Stream<Entry<K, QueryBuilder>> publishedFileQuery(K key, String value) {
        return Boolean.TRUE.equals(Boolean.valueOf(value))
                            ? queryToEntry(key, QueryBuilders.boolQuery().must(containsPublishedFileQuery()))
                            : queryToEntry(key, QueryBuilders.boolQuery().mustNot(containsPublishedFileQuery()));
    }

    private static MatchQueryBuilder containsPublishedFileQuery() {
        return QueryBuilders.matchQuery(jsonPath(ASSOCIATED_ARTIFACTS, TYPE, KEYWORD),
                                        PUBLISHED_FILE);
    }

    private boolean isNotKeyword(K key) {
        return !ParameterKind.KEYWORD.equals(key.fieldType());
    }

    public boolean isBooleanKey(K key) {
        return ParameterKind.BOOLEAN.equals(key.fieldType());
    }

    public boolean isNumberKey(K key) {
        return key.searchOperator() == GREATER_THAN_OR_EQUAL_TO
               || key.searchOperator() == LESS_THAN
               || key.searchOperator() == BETWEEN;
    }

    public boolean isFundingKey(K key) {
        return Words.FUNDING.equals(key.name());
    }

    public boolean isCristinIdentifierKey(K key) {
        return Words.CRISTIN_IDENTIFIER.equals(key.name());
    }

    public boolean isScopusIdentifierKey(K key) {
        return Words.SCOPUS_IDENTIFIER.equals(key.name());
    }

    public boolean isFuzzyKeywordKey(K key) {
        return FUZZY_KEYWORD.equals(key.fieldType());
    }

    public boolean isKeywordKey(K key) {
        return ParameterKind.KEYWORD.equals(key.fieldType());
    }

    public boolean isSearchAllKey(K key) {
        return Words.SEARCH_ALL_KEY_NAME.equals(key.name());
    }

    public boolean isTextKey(K key) {
        return ParameterKind.TEXT.equals(key.fieldType())
               || ParameterKind.FUZZY_TEXT.equals(key.fieldType());
    }

}
