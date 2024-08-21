package no.unit.nva.search.common.enums;

import static no.unit.nva.search.common.constant.Words.CHAR_UNDERSCORE;
import org.apache.commons.text.CaseUtils;

/**
 * @author Stig Norland
 */
public enum ParameterKind {
    INVALID,
    /**
     * Ignored parameters are not processed by standard or custom handling.
     * Normally used together with other parameters in custom handlers or paging.
     */
    IGNORED,
    BOOLEAN,
    NUMBER,
    DATE,
    EXISTS,
    HAS_PARTS,
    PART_OF,
    KEYWORD,
    FUZZY_KEYWORD,
    TEXT,
    FREE_TEXT,
    ACROSS_FIELDS,
    SORT_KEY,
    CUSTOM;

    public String asCamelCase() {
        return CaseUtils.toCamelCase(this.name(), false, CHAR_UNDERSCORE);
    }
}
