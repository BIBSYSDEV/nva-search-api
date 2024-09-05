package no.unit.nva.search.endpoint.model;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import static java.util.Objects.nonNull;

public enum Operator implements ILabel {
    INVALID(),
    /**
     * Ignored parameters are not processed by standard or custom handling. Normally used together
     * with other parameters in custom handlers or paging.
     */
    EQUAL(),
    NOT_EQUAL("not"),
    COUNT(),
    EXISTS(),
    HAS_PARTS(),
    PART_OF(),
    BETWEEN(),
    GREATER_THAN("gt"),
    GREATER_THAN_OR_EQUAL("gte"),
    LESS_THAN("lt"),
    LESS_THAN_OR_EQUAL("lte"),
    ;

    private final String prefix;

    Operator() {
        this(EMPTY_STRING);
    }

    Operator(String prefix) {
        this.prefix = prefix;
    }

    public String pattern() {
        return prefix;
    }

    @Override
    public String asCamelCase() {
        return ILabel.asCamelCase(this.name());
    }

    @Override
    public String asLowerCase() {
        return ILabel.asLowerCase(this.name());
    }

    public static Operator fromString(String operator) {
        if (nonNull(operator)) {
            for (Operator value : values()) {
                if (value.name().equalsIgnoreCase(operator)) {
                    return value;
                }
            }
        }
        return INVALID;
    }
}
