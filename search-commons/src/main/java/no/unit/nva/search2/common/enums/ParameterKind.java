package no.unit.nva.search2.common.enums;

import static no.unit.nva.search2.common.constant.Words.CHAR_UNDERSCORE;
import org.apache.commons.text.CaseUtils;

public enum ParameterKind {
    INVALID,
    /**
     * Ignored parameters are not processed by standard or custom handing.
     * Normally used in conjection with other parameters in custom handlers or paging.
     */
    IGNORED,
    BOOLEAN,
    NUMBER,
    DATE,
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
