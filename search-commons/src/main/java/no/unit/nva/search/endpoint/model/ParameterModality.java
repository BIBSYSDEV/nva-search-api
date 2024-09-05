package no.unit.nva.search.endpoint.model;

public enum ParameterModality implements ILabel {
    ALL_OF,
    ANY_OF;

    @Override
    public String asCamelCase() {
        return ILabel.asCamelCase(this.name());
    }

    @Override
    public String asLowerCase() {
        return ILabel.asLowerCase(this.name());
    }
}
