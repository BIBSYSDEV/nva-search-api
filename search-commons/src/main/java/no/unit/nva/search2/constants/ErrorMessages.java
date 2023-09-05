package no.unit.nva.search2.constants;

import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class ErrorMessages {

    public static final String ALPHANUMERIC_CHARACTERS_DASH_COMMA_PERIOD_AND_WHITESPACE =
        "May only contain alphanumeric characters, dash, comma, period, colon, semicolon and whitespace";
    public static final String ERROR_MESSAGE_INVALID_VALUE = "Parameter '%s' has invalid value. ";

    public static final String ERROR_MESSAGE_INVALID_NUMBER = "Parameter '%s' has invalid value. Must be a number.";
    public static final String ERROR_MESSAGE_INVALID_DATE = "Parameter '%s' has invalid value. Must be a date.";
    public static final String ERROR_MESSAGE_PAGE_OUT_OF_SCOPE =
        "Page requested is out of scope. Query contains %s results";
    public static final String ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS =
        "Invalid query parameter supplied %s. Valid parameters: %s";
    public static final String ERROR_MESSAGE_INVALID_FIELD_VALUE = "Invalid value for field '%s'";


    /**
     * Formats and emits a message with valid parameter names.
     *
     * @param invalidKeys     list of invalid parameter names
     * @param queryParameters list of valid parameter names
     * @return formatted string containing a list of valid parameters
     */
    public static String validQueryParameterNamesMessage(Set<String> invalidKeys, Set<String> queryParameters) {
        return String.format(ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS, invalidKeys,
                             prettifyList(queryParameters));
    }

    /**
     * Creates a error message containing which parameter that has invalid value and what the value is supposed to be.
     *
     * @param queryParameterName name of parameter with invalid value
     * @param validValues        what values are allowed for this parameter
     * @return formatted string containing a message with allowed values for this parameter
     */
    public static String invalidQueryParametersMessage(String queryParameterName, String validValues) {
        return String.format(ERROR_MESSAGE_INVALID_VALUE + validValues, queryParameterName);
    }


    public static String requiredMissingMessage(Set<String> missingKeys) {
        return String.format(ERROR_MESSAGE_INVALID_FIELD_VALUE, prettifyList(missingKeys));
    }

    private static String prettifyList(Set<String> queryParameters) {
        return queryParameters.size() > 1
                   ? queryParameters.stream().sorted()
                         .map(parameterName -> "'" + parameterName + "'")
                         .collect(Collectors.joining(", ", "[", "]"))
                   : queryParameters.stream()
                         .collect(Collectors.joining("", "'", "'"));
    }



}