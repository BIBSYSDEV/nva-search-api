package no.unit.nva.search.endpoint;

import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.endpoint.model.ParameterModality.ALL_OF;
import static no.unit.nva.search.endpoint.model.ParameterType.FREE_TEXT;
import static no.unit.nva.search.endpoint.model.ParameterType.IGNORED;
import static no.unit.nva.search.endpoint.model.ParameterType.NUMBER;
import static no.unit.nva.search.endpoint.model.ParameterType.SORT_KEY;

import static java.util.Objects.nonNull;

import no.unit.nva.search.common.enums.ValueEncoding;
import no.unit.nva.search.endpoint.model.ILabel;
import no.unit.nva.search.endpoint.model.IParameter;
import no.unit.nva.search.endpoint.model.Operator;
import no.unit.nva.search.endpoint.model.ParameterModality;
import no.unit.nva.search.endpoint.model.ParameterType;

import java.util.stream.Stream;

public enum DefaultParameter implements IParameter {
    SEARCH_ALL(FREE_TEXT, ALL_OF, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    NODES_SEARCHED(IGNORED, PATTERN_IS_FIELDS_SEARCHED),
    NODES_INCLUDED(IGNORED),
    NODES_EXCLUDED(IGNORED),
    // Pagination parameters
    AGGREGATION(IGNORED),
    PAGE(NUMBER),
    FROM(NUMBER, PATTERN_IS_FROM_KEY),
    SIZE(NUMBER, PATTERN_IS_SIZE_KEY),
    SEARCH_AFTER(IGNORED),
    SORT(SORT_KEY, PATTERN_IS_SORT_KEY),
    SORT_ORDER(IGNORED, ALL_OF, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null);

    private final String patternOfValue;
    private final String patternOfKey;
    private final ParameterModality modality;
    private final ParameterType type;
    private final String errorMessage;

    DefaultParameter(
            ParameterType type,
            ParameterModality modality,
            String patternOfKey,
            String patternOfValue,
            String errorMessage) {
        this.type = type;
        this.modality = modality;
        this.patternOfKey = patternOfKey;
        this.patternOfValue = patternOfValue;
        this.errorMessage = errorMessage;
    }

    DefaultParameter(ParameterType type) {
        this(type, null, null, null, null);
    }

    DefaultParameter(ParameterType parameterType, String patternIsFieldsSearched) {
        this(parameterType, null, patternIsFieldsSearched, null, null);
    }

    @Override
    public ParameterType type() {
        return type;
    }

    @Override
    public ParameterModality modality() {
        return modality;
    }

    @Override
    public Operator fieldOperator() {
        return Operator.INVALID;
    }

    @Override
    public Float fieldBoost() {
        return 0f;
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
    public Parameter subQueryReference() {
        return null;
    }

    @Override
    public IParameter subQuery() {
        return null;
    }

    @Override
    public Stream<String> searchFields(boolean... isKeyWord) {
        return Stream.empty();
    }

    @Override
    public String asCamelCase() {
        return ILabel.asCamelCase(this.name());
    }

    @Override
    public String asLowerCase() {
        return ILabel.asLowerCase(this.name());
    }
}
