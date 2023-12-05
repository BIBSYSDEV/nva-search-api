package no.unit.nva.search2.enums;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_DATE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_NUMBER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_BOOLEAN;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import java.util.Collection;
import java.util.function.Predicate;
import nva.commons.core.JacocoGenerated;

public interface ParameterKey {

    String fieldName();

    Float fieldBoost();

    ParamKind fieldType();

    String fieldPattern();

    String valuePattern();

    ValueEncoding valueEncoding();

    Collection<String> searchFields();

    FieldOperator searchOperator();

    String errorMessage();

    static Predicate<ParameterKey> equalTo(String name) {
        return key -> name.matches(key.fieldPattern());
    }

    @JacocoGenerated
    static ValueEncoding getEncoding(ParamKind kind) {
        return switch (kind) {
            case INVALID, NUMBER, BOOLEAN, CUSTOM -> ValueEncoding.NONE;
            case DATE, KEYWORD, TEXT, SORT_KEY -> ValueEncoding.DECODE;
        };
    }

    @JacocoGenerated
    static String getErrorMessage(ParamKind kind) {
        return switch (kind) {
            case BOOLEAN -> INVALID_VALUE;
            case DATE -> INVALID_DATE;
            case NUMBER -> INVALID_NUMBER;
            // case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case SORT_KEY -> INVALID_VALUE_WITH_SORT;
            case KEYWORD, TEXT, CUSTOM -> INVALID_VALUE;
            case INVALID -> "Status INVALID should not raise an exception, Exception";
        };
    }

    @JacocoGenerated
    static String getValuePattern(ParamKind kind, String pattern) {
        return nonNull(pattern) ? pattern : switch (kind) {
            case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            // case RANGE -> PATTERN_IS_RANGE;
            case KEYWORD, CUSTOM, TEXT, SORT_KEY -> PATTERN_IS_NON_EMPTY;
            case INVALID -> PATTERN_IS_NONE_OR_ONE;
        };
    }

    static int compareAscending(Enum<?> key1, Enum<?> key2) {
        return key1.ordinal() - key2.ordinal();
    }


    enum ValueEncoding {
        NONE, DECODE
    }

    enum ParamKind {
        BOOLEAN, DATE, NUMBER, KEYWORD, TEXT, SORT_KEY, CUSTOM, INVALID
    }

    enum FieldOperator {
        MUST, MUST_NOT, SHOULD, GREATER_THAN_OR_EQUAL_TO, LESS_THAN
    }
}