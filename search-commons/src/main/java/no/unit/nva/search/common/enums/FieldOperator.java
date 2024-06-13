package no.unit.nva.search.common.enums;

import java.util.Locale;

/**
 * @author Stig Norland
 */
public enum FieldOperator {
    /**
     * ALL must match in document (Only sensible for collections).
     */
    ALL_ITEMS,
    /**
     * None can match in document (Inverted of MUST).
     */
    NO_ITEMS,
    /**
     * One or more must match (Only sensible for unique fields).
     */
    ONE_OR_MORE_ITEM,
    /**
     * Any cannot match (These should be excluded).
     */
    NOT_ONE_ITEM,
    /**
     * Greater than or equal to.
     */
    GREATER_THAN_OR_EQUAL_TO,
    /**
     * Less than.
     */
    LESS_THAN,
    /**
     * Between.
     */
    BETWEEN,
    /**
     * Not Applicable
     */
    NA;

    public String asLowerCase() {
        return this.name().toLowerCase(Locale.getDefault());
    }
}
