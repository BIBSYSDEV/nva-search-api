package no.unit.nva.search2.constant;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class Patterns {

    public static final String PATTERN_IS_IGNORE_CASE = "(?i)";
    public static final String PATTERN_IS_BOOLEAN = PATTERN_IS_IGNORE_CASE + "(true|false|1|0)";
    public static final String PATTERN_IS_WORD_ENDING_WITH_HASHTAG = "[A-za-z0-9]*#";
    public static final String PATTERN_IS_ASC_DESC_VALUE = "(?i)asc|desc";
    public static final String PATTERN_IS_ASC_OR_DESC_GROUP = "(?i) (asc|desc)";
    public static final String PATTERN_IS_CATEGORY_KEYS = "(?i)instance.?type|category";
    public static final String PATTERN_IS_CATEGORY_NOT_KEYS = "(?i)instance.?type.?not|category.?not";
    public static final String PATTERN_IS_CATEGORY_SHOULD_KEYS = "(?i)instance.?type.?should|category.?should";

    /**
     * Pattern for matching a date string. yyyy | yyyy-MM-dd | yyyy-MM-ddTHH:mm:ssZ | yyyy-MM-ddTHH:mm:ss.SSSZ
     */
    public static final String PATTERN_IS_DATE = "\\d{4}(-\\d{2}(-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z?)?)?)?";
    public static final String PATTERN_IS_DOC_COUNT_ERROR_UPPER_BOUND = "(?i)doc.?count.?error.?upper.?bound";
    public static final String PATTERN_IS_FROM_KEY = "(?i)offset|from";
    public static final String PATTERN_IS_NONE_OR_ONE = ".?";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";
    public static final String PATTERN_IS_PUBLICATION_YEAR_KEYS = "(?i)year.?reported|publication.?year";
    public static final String PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS = "(?i)year.?reported.?should|publication"
                                                                         + ".?year.?should";
    public static final String PATTERN_IS_SEARCH_ALL_KEY = "(?i)search.?all|query";
    public static final String PATTERN_IS_SELECTED_GROUP = ":$1";
    public static final String PATTERN_IS_SIZE_KEY = "(?i)per.?page|results|limit|size";
    public static final String PATTERN_IS_SORT_KEY = "(?i)order.?by|sort";
    public static final String PATTERN_IS_SORT_ORDER_KEY = "(?i)sort.?order|order";
    public static final String PATTERN_IS_SUM_OTHER_DOC_COUNT = PATTERN_IS_IGNORE_CASE + "sum.?other.?doc.?count";
    public static final String PATTERN_IS_URI = "https?://[^\\s/$.?#].[^\\s]*";
    public static final String PATTERN_IS_URL_PARAM_INDICATOR = "\\?";


    /**
     * Pattern for matching a funding string.
     * funding source and project_id together separated by ':'
     */
    public static final String PATTERN_IS_FUNDING =  "[\\w]+[:\\s]{1}\\d+";

}