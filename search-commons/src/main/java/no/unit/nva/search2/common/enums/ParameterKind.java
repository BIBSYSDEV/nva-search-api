package no.unit.nva.search2.common.enums;

public enum ParameterKind {
    INVALID,
    /**
     * Ignored parameters are not processed by standard or custom handing.
     * Normally used in conjecture with other parameters in custom handlers or paging.
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
