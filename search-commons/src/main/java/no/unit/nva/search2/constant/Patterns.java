package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class Patterns {

    /**
     * Pattern for matching a date string.
     * yyyy-MM-dd | yyyy-MM-ddTHH:mm:ssZ | yyyy-MM-ddTHH:mm:ss.SSSZ
     */
    public static final String PATTERN_IS_DATE = "(\\d){4}-(\\d){2}-(\\d){2}[T]*[(\\d){2}:(\\d){2}:(\\d){2,6}Z]*";

    /**
     * Pattern for matching a date string.
     *  yyyy | yyyy-MM-dd
     */
    public static final String PATTERN_IS_DATE_STRING = "\\b\\d{4}(?:-\\d{2}-\\d{2})?\\b";
    public static final String PATTERN_IS_ADD_SLASH = "\\\\$1";
    public static final String PATTERN_IS_IGNORE_CASE = "(?i)";
    public static final String PATTERN_IS_NONE_OR_ONE = ".?";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";

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
    public static final String PATTERN_IS_SPECIAL_CHARACTERS = "([-+&|!\\(\\){}\\[\\]^\"~*?:\\\\/])";

}