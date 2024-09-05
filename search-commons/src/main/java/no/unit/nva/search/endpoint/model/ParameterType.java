package no.unit.nva.search.endpoint.model;

import no.unit.nva.search.common.enums.ValueEncoding;

public enum ParameterType implements ILabel {
    INVALID,
    /**
     * Ignored parameters are not processed by standard or custom handling. Normally used together
     * with other parameters in custom handlers or paging.
     */
    IGNORED,
    CUSTOM,
    BOOLEAN,
    NUMBER,
    DATE,
    KEYWORD,
    FUZZY_KEYWORD,
    TEXT,
    FREE_TEXT,
    ACROSS_FIELDS,
    SORT_KEY;

    @Override
    public String asCamelCase() {
        return ILabel.asCamelCase(this.name());
    }

    @Override
    public String asLowerCase() {
        return ILabel.asLowerCase(this.name());
    }

    public ValueEncoding encoding() {
        return getEncoding(this);
    }

    static ValueEncoding getEncoding(ParameterType type) {
        return switch (type) {
            case INVALID, IGNORED, BOOLEAN, NUMBER -> ValueEncoding.NONE;
            default -> ValueEncoding.DECODE;
        };
    }
}
