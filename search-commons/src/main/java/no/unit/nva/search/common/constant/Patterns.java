package no.unit.nva.search.common.constant;

import static no.unit.nva.constants.Words.COLON;
import static no.unit.nva.constants.Words.PIPE;
import static no.unit.nva.constants.Words.SPACE;

import nva.commons.core.JacocoGenerated;

/**
 * Class for defining regular expressions used in the search service.
 *
 * @author Stig Norland
 */
public final class Patterns {

    public static final String COLON_OR_SPACE = COLON + PIPE + SPACE;
    public static final String PATTERN_IS_IGNORE_CASE = "(?i)";
    public static final String PATTERN_IS_WORD_ENDING_WITH_HASHTAG = "[A-za-z0-9]*#";
    public static final String PATTERN_IS_ASC_DESC_VALUE = "(?i)asc|desc";
    public static final String PATTERN_IS_ASC_OR_DESC_GROUP = "(?i) (asc|desc)";
    public static final String PATTERN_IS_CATEGORY_KEYS =
            "(?i)(type|instance_?type|category)(_?should)?$";
    public static final String PATTERN_IS_CATEGORY_NOT_KEYS =
            "(?i)(type_?not|instance_?type_?not|category_?not)$";

    public static final String PATTERN_IS_BOOLEAN = "(?i)(true|false)";

    /**
     * Pattern for matching a date string. yyyy | yyyy-MM-dd | yyyy-MM-ddTHH:mm:ssZ |
     * yyyy-MM-ddTHH:mm:ss.SSSZ
     */
    public static final String PATTERN_IS_DATE =
            "\\d{4}(-\\d{2}(-\\d{2}(T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z?)?)?)?";

    public static final String PATTERN_IS_FROM_KEY = "(?i)offset|from";
    public static final String PATTERN_IS_FIELDS_SEARCHED = "(?i)fields|nodes_?searched";

    public static final String PATTERN_IS_ORGANIZATION = "(?i)(organization_?id|viewing_?scope)$";
    public static final String PATTERN_IS_FUNDING_IDENTIFIER =
            "(?i)(funding_?identifier|grant_?id)$";
    public static final String PATTERN_IS_FUNDING_IDENTIFIER_NOT =
            "(?i)(funding_?identifier_?not|grant_?id_?not)$";
    public static final String PATTERN_IS_FUNDING_IDENTIFIER_SHOULD =
            "(?i)(funding_?identifier_?should|grant_?id_?should)$";
    public static final String PATTERN_IS_NONE_OR_ONE = ".?";
    public static final String PATTERN_IS_NON_EMPTY = ".+";
    public static final String PATTERN_IS_NUMBER = "[0-9]\\d*";

    public static final String PATTERN_IS_SEARCH_ALL_KEY = "(?i)search_?all|query";
    public static final String PATTERN_IS_SELECTED_GROUP = ":$1";
    public static final String PATTERN_IS_SIZE_KEY = "(?i)per_?page|results|limit|size";
    public static final String PATTERN_IS_SORT_KEY = "(?i)order_?by|sort";
    public static final String PATTERN_IS_SORT_ORDER_KEY = "(?i)sort_?order|order";
    public static final String PATTERN_IS_URI =
            "https?://[^\\s/$.?#].[^\\s]*"; // Pattern for matching a URI
    public static final String PATTERN_IS_PIPE = "\\|";

    /**
     * Pattern for matching a funding string. funding source and project_id together separated by
     * ':'
     */
    public static final String PATTERN_IS_FUNDING = "[\\w]+[:\\s]{1}.+";

    public static final String PATTERN_IS_NONE_PRINTABLE_CHARACTERS = "[^ -~]+";

    @JacocoGenerated
    public Patterns() {}
}
