package no.unit.nva.search2.constant;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

import static no.unit.nva.search2.constant.ApplicationConstants.QUOTE;
import static no.unit.nva.search2.constant.ApplicationConstants.SUFFIX;
import static no.unit.nva.search2.constant.ApplicationConstants.PREFIX;
import static nva.commons.core.StringUtils.EMPTY_STRING;

@JacocoGenerated
public class ErrorMessages {
    public static final String INVALID_VALUE = "Parameter [%s] has invalid value [%s]";
    public static final String INVALID_VALUE_WITH_SORT =
        "Sort has invalid field value [%s]. Valid values are: %s";
    public static final String INVALID_NUMBER = "Parameter '%s' has invalid value. Must be a number.";
    public static final String INVALID_DATE = "Parameter '%s' has invalid value. Must be a date.";
    public static final String INVALID_SORT_ORDER =
        "Parameter '%s' has invalid value. Must be either 'asc' or 'desc'.";
    public static final String TEMPLATE_INVALID_QUERY_PARAMETERS =
        """
        Invalid query parameter supplied %s.
        Valid parameters: %s
        Also pass through to OpenSearch: [from & size]|[offset & results]|[page & per_page], [sort (& sortOrder)], fields, search_after
        """;
    public static final String MISSING_PARAMETER = "Parameter(s) -> [%s] -> is/are required.";

    /**
     * Formats and emits a message with valid parameter names.
     *
     * @param invalidKeys     list of invalid parameter names
     * @param queryParameters list of valid parameter names
     * @return formatted string containing a list of valid parameters
     */
    public static String validQueryParameterNamesMessage(Set<String> invalidKeys, Collection<String> queryParameters) {
        return TEMPLATE_INVALID_QUERY_PARAMETERS
                   .formatted(invalidKeys,queryParameters);
    }

    /**
     * Creates an error message containing which parameter that has invalid value and what the value is supposed to be.
     *
     * @param queryParameterName name of parameter with invalid value
     * @param inValidValue        what values are allowed for this parameter
     * @return formatted string containing a message with allowed values for this parameter
     */
    public static String invalidQueryParametersMessage(String queryParameterName, String inValidValue) {
        return String.format(INVALID_VALUE, queryParameterName, inValidValue);
    }

    public static String requiredMissingMessage(Set<String> missingKeys) {
        return String.format(MISSING_PARAMETER, prettifyList(missingKeys));
    }

    private static String prettifyList(Set<String> queryParameters) {
        return queryParameters.size() > 1
                   ? queryParameters.stream()
                         .map(parameterName -> QUOTE + parameterName + QUOTE)
                         .collect(Collectors.joining(", ", PREFIX, SUFFIX))
                   : queryParameters.stream()
                         .collect(Collectors.joining(EMPTY_STRING, QUOTE, QUOTE));
    }
}