package no.unit.nva.search2;

import java.util.Arrays;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static no.unit.nva.search2.Constants.PATTERN_IS_NON_EMPTY;

public enum ResourceKeys implements IParameterKey {
    INVALID(null),
    CATEGORY("category", PATTERN_IS_NON_EMPTY),
    CONTRIBUTOR("contributor",PATTERN_IS_NON_EMPTY),
    CREATED_BEFORE("created_before",PATTERN_IS_NON_EMPTY),
    CREATED_SINCE(Constants.CREATED_SINCE),
    DOI(Constants.DOI),
    FUNDING(Constants.FUNDING),
    FUNDING_SOURCE(Constants.FUNDING_SOURCE),
    ID(Constants.ID),
    INSTITUTION(Constants.INSTITUTION),
    ISSN(Constants.ISSN),
    MODIFIED_BEFORE(Constants.MODIFIED_BEFORE),
    MODIFIED_SINCE(Constants.MODIFIED_SINCE),
    PROJECT_CODE(Constants.PROJECT_CODE),
    PUBLISHED_BEFORE(Constants.PUBLISHED_BEFORE),
    PUBLISHED_SINCE(Constants.PUBLISHED_SINCE),
    TITLE(Constants.TITLE),
    UNIT(Constants.UNIT),
    USER(Constants.USER),
    YEAR_REPORTED(Constants.YEAR_REPORTED),
    LANG(Constants.LANG),
    PAGE(Constants.PAGE, QueryParameterConstant.PATTERN_IS_NUMBER,
        QueryParameterConstant.ERROR_MESSAGE_INVALID_NUMBER,KeyEncoding.NONE),
    PER_PAGE(Constants.PER_PAGE),
    SORT(Constants.SORT),
    FIELDS(Constants.FIELDS);
    public static final int IGNORE_PATH_PARAMETER_INDEX = 0;

    public static final Set<ResourceKeys> VALID_QUERY_PARAMETERS =
        Arrays.stream(ResourceKeys.values())
            .filter(ResourceKeys::ignorePathKeys)
            .collect(Collectors.toSet());

    public static final Set<String> VALID_QUERY_PARAMETER_KEYS =
        VALID_QUERY_PARAMETERS.stream()
            .sorted()
            .map(ResourceKeys::getKey)
            .collect(Collectors.toSet());

    public static final Set<String> VALID_QUERY_PARAMETER_NVA_KEYS =
        VALID_QUERY_PARAMETERS.stream()
            .sorted()
            .map(ResourceKeys::getKey)
            .collect(Collectors.toSet());

    private final String pattern;
    private final String key;
    private final KeyEncoding encode;
    private final String errorMessage;

    ResourceKeys(String key) {
        this(key, PATTERN_IS_NON_EMPTY, null, KeyEncoding.NONE);
    }

    ResourceKeys(String key, String pattern) {
        this(key,  pattern, null, KeyEncoding.NONE);
    }

    ResourceKeys(String key, String pattern, String errorMessage,
                 KeyEncoding encode) {
        this.key = key;
        this.pattern = pattern;
        this.encode = encode;
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public KeyEncoding encoding() {
        return encode;
    }

    @Override
    public String toString() {
        return
            new StringJoiner(":", "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(name())
                .toString();
    }

    public static ResourceKeys keyFromString(String paramName, String value) {
        var result = Arrays.stream(ResourceKeys.values())
            .filter(ResourceKeys::ignorePathKeys)
            .filter(IParameterKey.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : result.stream()
            .filter(IParameterKey.hasValidValue(value))
            .findFirst()
            .orElse(INVALID);
    }


    private static boolean ignorePathKeys(ResourceKeys f) {
        return f.ordinal() > IGNORE_PATH_PARAMETER_INDEX;
    }

    public static class QueryParameterConstant {
        static final String ERROR_MESSAGE_INVALID_NUMBER = "";
        static final String PATTERN_IS_NUMBER = "";
        static final String ERROR_MESSAGE_INVALID_VALUE ="";
        static final String ALPHANUMERIC_CHARACTERS_DASH_COMMA_PERIOD_AND_WHITESPACE = "";
        static final String ERROR_MESSAGE_INVALID_CHARACTERS =
            ERROR_MESSAGE_INVALID_VALUE + ALPHANUMERIC_CHARACTERS_DASH_COMMA_PERIOD_AND_WHITESPACE;
    }
}