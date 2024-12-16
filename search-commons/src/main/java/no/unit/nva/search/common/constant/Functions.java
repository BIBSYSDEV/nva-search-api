package no.unit.nva.search.common.constant;

import static no.unit.nva.constants.Defaults.ENVIRONMENT;
import static no.unit.nva.constants.Words.BOKMAAL_CODE;
import static no.unit.nva.constants.Words.COLON;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.ENGLISH_CODE;
import static no.unit.nva.constants.Words.KEYWORD;
import static no.unit.nva.constants.Words.LABELS;
import static no.unit.nva.constants.Words.NYNORSK_CODE;
import static no.unit.nva.constants.Words.PIPE;
import static no.unit.nva.constants.Words.SAMI_CODE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_OR_DESC_GROUP;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SELECTED_GROUP;

import static nva.commons.core.StringUtils.SPACE;

import static java.util.Objects.nonNull;

import no.unit.nva.constants.Defaults;
import no.unit.nva.search.common.enums.ParameterKey;

import nva.commons.core.JacocoGenerated;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for building OpenSearch queries that search across multiple fields.
 *
 * @author Stig Norland
 */
public final class Functions {

    private static final String SEARCH_INFRASTRUCTURE_AUTH_URI = "SEARCH_INFRASTRUCTURE_AUTH_URI";
    private static final String SEARCH_INFRASTRUCTURE_API_URI = "SEARCH_INFRASTRUCTURE_API_URI";
    private static final String API_HOST = "API_HOST";

    @JacocoGenerated
    public Functions() {}

    public static String jsonPath(String... args) {
        return String.join(DOT, args);
    }

    @JacocoGenerated
    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_AUTH_URI);
    }

    private static Stream<String> languageCodes() {
        return Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE);
    }

    public static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static String readApiHost() {
        return ENVIRONMENT.readEnv(API_HOST);
    }

    public static NestedAggregationBuilder labels(String jsonPath) {
        var nestedAggregation = nestedBranchBuilder(LABELS, jsonPath, LABELS);

        languageCodes()
                .map(code -> branchBuilder(code, jsonPath, LABELS, code, KEYWORD))
                .forEach(nestedAggregation::subAggregation);

        return nestedAggregation;
    }

    public static TermsAggregationBuilder branchBuilder(String name, String... pathElements) {
        return AggregationBuilders.terms(name)
                .field(jsonPath(pathElements))
                .size(Defaults.DEFAULT_AGGREGATION_SIZE);
    }

    public static NestedAggregationBuilder nestedBranchBuilder(
            String name, String... pathElements) {
        return new NestedAggregationBuilder(name, jsonPath(pathElements));
    }

    public static FilterAggregationBuilder filterBranchBuilder(
            String name, String filter, String... paths) {
        return AggregationBuilders.filter(name, QueryBuilders.termQuery(jsonPath(paths), filter));
    }

    public static FilterAggregationBuilder filterBranchBuilder(
            String name, QueryBuilder queryBuilder) {
        return AggregationBuilders.filter(name, queryBuilder);
    }

    public static String mergeWithColonOrComma(String oldValue, String newValue) {
        if (nonNull(oldValue)) {
            var delimiter = newValue.matches(PATTERN_IS_ASC_DESC_VALUE) ? COLON : COMMA;
            return String.join(delimiter, oldValue, newValue);
        } else {
            return newValue;
        }
    }

    public static String trimSpace(String value) {
        return value.replaceAll(PATTERN_IS_ASC_OR_DESC_GROUP, PATTERN_IS_SELECTED_GROUP);
    }

    public static String toEnumStrings(Function<String, Enum<?>> fromString, String decodedValue) {
        return Arrays.stream(decodedValue.split(COMMA + PIPE + SPACE))
                .map(fromString)
                .map(Enum::toString)
                .collect(Collectors.joining(COMMA));
    }

    public static String multipleFields(String... values) {
        return String.join(PIPE, values);
    }

    @JacocoGenerated
    public static boolean hasContent(String value) {
        return nonNull(value) && !value.isEmpty();
    }

    @JacocoGenerated
    public static boolean hasContent(Collection<?> value) {
        return nonNull(value) && !value.isEmpty();
    }

    public static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static <K extends Enum<K> & ParameterKey<K>>
            Stream<Map.Entry<K, QueryBuilder>> queryToEntry(K key, QueryBuilder qb) {
        final var entry =
                new Map.Entry<K, QueryBuilder>() {
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
}
