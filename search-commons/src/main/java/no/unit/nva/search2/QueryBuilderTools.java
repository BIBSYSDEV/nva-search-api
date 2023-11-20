package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.ALL;
import static no.unit.nva.search2.constant.ApplicationConstants.ASTERISK;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.ZERO;
import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.model.ParameterKeyResources.FROM;
import static no.unit.nva.search2.model.ParameterKeyResources.SEARCH_AFTER;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.ParameterKey;
import no.unit.nva.search2.model.ParameterKeyResources;
import no.unit.nva.search2.model.SortKeyResources;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.sort.SortOrder;

public final class QueryBuilderTools {
    
    private static final Integer SINGLE_FIELD = 1;

    static void addKeywordQuery(ParameterKeyResources key, String value, BoolQueryBuilder bq) {
        final var searchFields = key.searchFields().toArray(String[]::new);
        final var values = Arrays.stream(value.split(COMMA))
            .map(String::trim)
            //            .map(ParameterKey::escapeSearchString)
            .toArray(String[]::new);
        final var multipleFields = hasMultipleFields(searchFields);
        
        Arrays.stream(searchFields).forEach(searchField -> {
            final var termsQuery = QueryBuilders.termsQuery(searchField, values).boost(key.fieldBoost());
            switch (key.searchOperator()) {
                case MUST -> {
                    if (multipleFields) {
                        bq.should(termsQuery);
                    } else {
                        bq.must(termsQuery);
                    }
                }
                case MUST_NOT -> bq.mustNot(termsQuery);
                case SHOULD -> bq.should(termsQuery);
                default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            }
        });
    }

    static QueryBuilder buildQuery(ParameterKeyResources key, String value) {
        final var values = value.replace(COMMA, " ");
        final var searchFields =
            key.searchFields().stream()
                .map(String::trim)
                .map(trimmed -> !key.fieldType().equals(ParameterKey.ParamKind.KEYWORD)
                    ? trimmed.replace(".keyword", "")
                    : trimmed)
                .toArray(String[]::new);
        if (hasMultipleFields(searchFields)) {
            return QueryBuilders
                .multiMatchQuery(values, searchFields)
                .type(Type.BEST_FIELDS)
                .operator(operatorByKey(key));
        }
        var searchField = searchFields[0];
        return QueryBuilders
            .matchQuery(searchField, values)
            .boost(key.fieldBoost())
            .operator(operatorByKey(key));
    }

    static RangeQueryBuilder rangeQuery(ParameterKeyResources key, String value) {
        final var searchField = key.searchFields().toArray()[0].toString();
        
        return switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        };
    }

    static Operator operatorByKey(ParameterKeyResources key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case SHOULD, MUST_NOT -> Operator.OR;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        };
    }
    
    @JacocoGenerated
    static Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var fieldName = SortKeyResources.fromSortKey(strings[0]).getFieldName();
        return new Tuple<>(fieldName, sortOrder);
    }
    
    @NotNull
    static String[] extractFields(String field) {
        return ALL.equals(field) || isNull(field)
            ? ASTERISK.split(COMMA)
            : Arrays.stream(field.split(COMMA))
                .map(ParameterKeyResources::keyFromString)
                .map(ParameterKeyResources::searchFields)
                .flatMap(Collection::stream)
                .toArray(String[]::new);
    }
    
    static boolean isFirstPage(ResourceAwsQuery query) {
        return ZERO.equals(query.getValue(FROM).toString());
    }
    
    static boolean hasMultipleFields(String... swsKey) {
        return swsKey.length > SINGLE_FIELD;
    }
    
    static boolean hasPromotedPublications(List<String> promotedPublications) {
        return nonNull(promotedPublications) && !promotedPublications.isEmpty();
    }
    
    static URI nextResultsBySortKey(
        OpenSearchSwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {
        
        requestParameter.remove(FROM.fieldName());
        var sortedP =
            response.getSort().stream()
                .map(Object::toString)
                .collect(Collectors.joining(COMMA));
        requestParameter.put(SEARCH_AFTER.fieldName(), sortedP);
        return UriWrapper.fromUri(gatewayUri)
            .addQueryParameters(requestParameter)
            .getUri();
    }
}
