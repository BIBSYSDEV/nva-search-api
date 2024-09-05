package no.unit.nva.search.endpoint.model;

public enum ParameterCommand implements ILabel {
    INVALID,
    /**
     * Ignored parameters are not processed by standard or custom handling. Normally used together
     * with other parameters in custom handlers or paging.
     */
    ALL_OF,
    ANY_OF,
    BETWEEN,
    EQUAL,
    NOT_EQUAL,
    EXISTS,
    HAS_PARTS,
    PART_OF,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL;

    public String asCamelCase() {
        return ILabel.asCamelCase(this.name());
    }

    @Override
    public String asLowerCase() {
        return ILabel.asLowerCase(this.name());
    }
}
