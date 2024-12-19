package no.unit.nva.search.model.builder;

import static no.unit.nva.search.model.enums.FieldOperator.ANY_OF;

import no.unit.nva.search.model.enums.FieldOperator;
import no.unit.nva.search.model.enums.ParameterKey;
import no.unit.nva.search.model.enums.ParameterKind;
import no.unit.nva.search.model.enums.ValueEncoding;

import java.util.stream.Stream;

public enum FakeParameter implements ParameterKey<FakeParameter> {
    CONTRIBUTORS_OF_CHILD,
    CONTRIBUTORS;

    @Override
    public String asCamelCase() {
        return "";
    }

    @Override
    public String asLowerCase() {
        return "";
    }

    @Override
    public Float fieldBoost() {
        return 0f;
    }

    @Override
    public ParameterKind fieldType() {
        return ParameterKind.TEXT;
    }

    @Override
    public String fieldPattern() {
        return "";
    }

    @Override
    public String valuePattern() {
        return "";
    }

    @Override
    public ValueEncoding valueEncoding() {
        return null;
    }

    @Override
    public Stream<String> searchFields(boolean... isKeyWord) {
        return Stream.empty();
    }

    @Override
    public FieldOperator searchOperator() {
        return ANY_OF;
    }

    @Override
    public String errorMessage() {
        return "";
    }

    @Override
    public FakeParameter subQuery() {
        return CONTRIBUTORS;
    }
}
