package no.unit.nva.search.endpoint;

import static no.unit.nva.search.common.constant.Words.DOT;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.KEYWORD;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import static java.util.Objects.nonNull;

import no.unit.nva.search.common.constant.Words;
import no.unit.nva.search.common.enums.ValueEncoding;
import no.unit.nva.search.endpoint.model.ILabel;
import no.unit.nva.search.endpoint.model.IParameter;
import no.unit.nva.search.endpoint.model.Operator;
import no.unit.nva.search.endpoint.model.ParameterModality;
import no.unit.nva.search.endpoint.model.ParameterType;

import java.util.function.Function;
import java.util.stream.Stream;

public record Parameter(
        String name,
        ParameterType type,
        ParameterModality modality,
        Operator fieldOperator,
        Float fieldBoost,
        String patternOfKey,
        String patternOfValue,
        String errorMessage,
        Parameter subQueryReference,
        String... fieldsToSearch)
        implements IParameter {

    public Parameter(
            String name,
            ParameterType type,
            ParameterModality modality,
            Operator fieldOperator,
            String... fieldsToSearch) {

        this(name, type, modality, fieldOperator, 0f, null, null, null, null, fieldsToSearch);
    }

    @Override
    public String asCamelCase() {
        return ILabel.asCamelCase(this.name);
    }

    @Override
    public String asLowerCase() {
        return ILabel.asLowerCase(this.name());
    }

    @Override
    public ValueEncoding encoding() {
        return switch (type) {
            case INVALID, IGNORED, BOOLEAN, NUMBER -> ValueEncoding.NONE;
            default -> ValueEncoding.DECODE;
        };
    }

    @Override
    public String errorMessage() {
        return nonNull(errorMessage) ? errorMessage : IParameter.defaultErrorMessage(type());
    }

    @Override
    public String patternOfKey() {
        return nonNull(patternOfKey) ? patternOfKey : IParameter.defaultKeyPattern(name());
    }

    @Override
    public String patternOfValue() {
        return nonNull(patternOfValue) ? patternOfValue : IParameter.defaultValuePattern(type());
    }

    @Override
    public Parameter subQuery() {
        return subQueryReference;
    }

    @Override
    public Stream<String> searchFields(boolean... isKeyWord) {
        return Stream.of(fieldsToSearch);
    }

    static Function<String, String> trimKeyword(ParameterType type, boolean... isKeyWord) {
        return field ->
                isNotKeyword(type, isKeyWord)
                        ? field.trim().replace(DOT + Words.KEYWORD, EMPTY_STRING)
                        : field.trim();
    }

    static boolean isNotKeyword(ParameterType type, boolean... isKeyWord) {
        var result = !(type.equals(KEYWORD) || type.equals(CUSTOM));
        return isKeyWord.length == 1 ? !isKeyWord[0] : result;
    }
}
