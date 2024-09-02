package no.unit.nva.search.common.constant;

import static no.unit.nva.search.common.constant.Words.PREFIX;
import static no.unit.nva.search.common.constant.Words.QUOTE;
import static no.unit.nva.search.common.constant.Words.SUFFIX;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import nva.commons.core.JacocoGenerated;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Stig Norland
 */
public final class ErrorMessages {

    public static final String HANDLER_NOT_DEFINED = "handler NOT defined -> ";
    public static final String INVALID_VALUE = "Parameter [%s] has invalid value [%s]";
    public static final String INVALID_VALUE_WITH_SORT =
            "Sort has invalid field value [%s]. Valid values are: %s";
    public static final String INVALID_NUMBER =
            "Parameter '%s' has invalid value. Must be a number.";
    public static final String INVALID_DATE = "Parameter '%s' has invalid value. Must be a date.";
    public static final String INVALID_BOOLEAN =
            "Parameter '%s' has invalid value. Must be a boolean.";
    public static final String MISSING_PARAMETER = "Parameter(s) -> [%s] -> is/are required.";
    public static final String NOT_IMPLEMENTED_FOR = "Not implemented for ";

    public static final String OPERATOR_NOT_SUPPORTED = "Operator not supported";
    public static final String RELEVANCE_SEARCH_AFTER_ARE_MUTUAL_EXCLUSIVE =
            "Sorted by relevance & searchAfter are mutual exclusive.";
    public static final String TOO_MANY_ARGUMENTS = "too many arguments: ";

    public static final String TEMPLATE_INVALID_QUERY_PARAMETERS =
            "Invalid query parameter supplied %s.  Valid parameters: %s Also pass through to"
                + " OpenSearch:[page & per_page | offset & results, sort (& sortOrder), fields, "
                + "search_after]";

    /**
     * Formats and emits a message with valid parameter names.
     *
     * @param invalidKeys list of invalid parameter names
     * @param queryParameters list of valid parameter names
     * @return formatted string containing a list of valid parameters
     */
    public static String validQueryParameterNamesMessage(
            Set<String> invalidKeys, Collection<String> queryParameters) {
        return TEMPLATE_INVALID_QUERY_PARAMETERS.formatted(invalidKeys, queryParameters);
    }

    public static String requiredMissingMessage(Set<String> missingKeys) {
        return String.format(MISSING_PARAMETER, prettifyList(missingKeys));
    }

    private static String prettifyList(Set<String> queryParameters) {
        return queryParameters.size() > 1
                ? queryParameters.stream()
                        .map(parameterName -> QUOTE + parameterName + QUOTE)
                        .collect(Collectors.joining(", ", PREFIX, SUFFIX))
                : queryParameters.stream().collect(Collectors.joining(EMPTY_STRING, QUOTE, QUOTE));
    }

    @JacocoGenerated
    public ErrorMessages() {}
}
