package no.unit.nva.search2.common.enums;

public enum ParameterKind {
    INVALID,
    /**
     * ignored parameters are not processed by standard or custom handing
     */
    IGNORED,
    BOOLEAN,
    NUMBER,
    DATE,
    KEYWORD,
    FUZZY_KEYWORD,
    TEXT,
    FUZZY_TEXT,
    FREE_TEXT,
    SORT_KEY,
    CUSTOM
}
