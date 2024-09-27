package no.unit.nva.search.common.enums;

import static no.unit.nva.constants.Words.CHAR_UNDERSCORE;

import org.apache.commons.text.CaseUtils;

/**
 * Enum for defining the types of parameters that can be used in the search service.
 *
 * @author Stig Norland
 */
public enum ParameterKind {
    INVALID,
    /**
     * Ignored parameters are not processed by standard or custom handling. Normally used together
     * with other parameters in custom handlers or paging.
     */
    FLAG,
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
