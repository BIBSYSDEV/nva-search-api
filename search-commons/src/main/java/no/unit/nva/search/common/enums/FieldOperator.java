package no.unit.nva.search.common.enums;

import java.util.Locale;

/**
 * Enum for defining the operators used in the search service.
 *
 * @author Stig Norland
 */
public enum FieldOperator {
    /** ALL must match in document (Only sensible for collections). */
    ALL_OF,
    /** None can match in document (Inverted of MUST). */
    NOT_ALL_OF,
    /** One or more must match (Only sensible for unique fields). */
    ANY_OF,
    /** Any cannot match (These should be excluded). */
    NOT_ANY_OF,
    /** Greater than or equal to. */
    GREATER_THAN_OR_EQUAL_TO,
    /** Less than. */
    LESS_THAN,
    /** Between. */
    BETWEEN,
    /** Not Applicable */
    NA;

    public String asLowerCase() {
        return this.name().toLowerCase(Locale.getDefault());
    }
}
