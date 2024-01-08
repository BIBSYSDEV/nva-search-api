package no.unit.nva.search2.constant;

import nva.commons.core.JacocoGenerated;

public final class Patterns {

    @JacocoGenerated
    public Patterns() {
    }

    public static final String PATTERN_IS_IGNORE_CASE = "(?i)";
    public static final String PATTERN_IS_BOOLEAN = PATTERN_IS_IGNORE_CASE + "(true|false|1|0)";
    public static final String PATTERN_IS_WORD_ENDING_WITH_HASHTAG = "[A-za-z0-9]*#";
    public static final String PATTERN_IS_ASC_DESC_VALUE = "(?i)asc|desc";
    public static final String PATTERN_IS_ASC_OR_DESC_GROUP = "(?i) (asc|desc)";
    public static final String PATTERN_IS_CATEGORY_KEYS = "(?i)(type|instance_?type|category)$";
    public static final String PATTERN_IS_CATEGORY_NOT_KEYS =
        "(?i)(type_?not|instance_?type_?not|category_?not)$";
    public static final String PATTERN_IS_CATEGORY_SHOULD_KEYS =
        "(?i)(type_?should|instance_?type_?should|category_?should)$";

    /**
     * Pattern for matching a date string. yyyy | yyyy-MM-dd | yyyy-MM-ddTHH:mm:ssZ | yyyy-MM-ddTHH:mm:ss.SSSZ
     */
    public static final String PATTERN_IS_DATE = "\\d{4}(-\\d{2}(-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z?)?)?)?";
    public static final String PATTERN_IS_FROM_KEY = "(?i)offset|from";
    public static final String PATTERN_IS_NONE_OR_ONE = ".?";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";
    public static final String PATTERN_IS_PUBLICATION_YEAR_KEYS =
        "(?i)year_?reported|publication_?year";
    public static final String PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS =
        "(?i)year_?reported_?should|publication_?year_?should";
    public static final String PATTERN_IS_SEARCH_ALL_KEY = "(?i)search_?all|query";
    public static final String PATTERN_IS_SELECTED_GROUP = ":$1";
    public static final String PATTERN_IS_SIZE_KEY = "(?i)per_?page|results|limit|size";
    public static final String PATTERN_IS_SORT_KEY = "(?i)order_?by|sort";
    public static final String PATTERN_IS_SORT_ORDER_KEY = "(?i)sort_?order|order";
    public static final String PATTERN_IS_URI = "https?://[^\\s/$.?#].[^\\s]*";
    public static final String PATTERN_IS_URL_PARAM_INDICATOR = "\\?";

    /**
     * Pattern for matching a funding string. funding source and project_id together separated by ':'
     */
    public static final String PATTERN_IS_FUNDING = "[\\w]+[:\\s]{1}\\d+";
}