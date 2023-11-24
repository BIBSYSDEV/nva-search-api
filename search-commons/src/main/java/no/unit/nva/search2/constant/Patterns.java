package no.unit.nva.search2.constant;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class Patterns {
    
    public static final String PATTERN_IS_BOOLEAN = "true|false|1|0";

    public static final String PATTERN_IS_IGNORE_CASE = "(?i)";

    public static final String PATTERN_IS_BOOLEAN = PATTERN_IS_IGNORE_CASE + "(true|false|1|0)";

    /**
     * Pattern for matching a date string.
     * yyyy | yyyy-MM-dd | yyyy-MM-ddTHH:mm:ssZ | yyyy-MM-ddTHH:mm:ss.SSSZ
     */
    public static final String PATTERN_IS_DATE = "\\d{4}(-\\d{2}(-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z?)?)?)?";
    public static final String PATTERN_IS_ADD_SLASH = "\\\\$1";
    public static final String PATTERN_IS_ASC_OR_DESC =  PATTERN_IS_IGNORE_CASE + " (asc|desc)";
    public static final String PATTERN_IS_NONE_OR_ONE = ".?";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";
    public static final String PATTERN_IS_SELECTED_GROUP = ":$1";
    public static final String PATTERN_IS_URI = "https?://[^\\s/$.?#].[^\\s]*";
    public static final String PATTERN_IS_URL_PARAM_INDICATOR = "\\?";

    /**
     * Pattern for matching a funding string.
     * funding source and project_id together separated by ':'
     */
    public static final String PATTERN_IS_FUNDING =  "[\\w]+[:\\s]{1}\\d+";

    /**
     * Pattern for matching group of opensearch special characters.
     * - + & | ! ( ) { } [ ] ^ " ~ * ? : \ /
     *  with the intention of escaping them.
     */
    public static final String PATTERN_IS_SPECIAL_CHARACTERS = "([-+&|!\\(\\){}\\[\\]^\\\"\\~*?:\\/])";

}