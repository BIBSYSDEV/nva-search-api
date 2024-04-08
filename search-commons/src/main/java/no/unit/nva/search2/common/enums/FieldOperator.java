package no.unit.nva.search2.common.enums;

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
     * GREATER_THAN_OR_EQUAL_TO
     */
    GREATER_THAN_OR_EQUAL_TO,
    /**
     * LESS_THAN
     */
    LESS_THAN,
    /**
     * BETWEEN
     */
    BETWEEN
}
