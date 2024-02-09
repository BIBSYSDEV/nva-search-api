package no.unit.nva.search2.enums;

import java.util.Collection;
import java.util.function.Predicate;
import nva.commons.core.JacocoGenerated;

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
            case DATE, KEYWORD, FUZZY_TEXT, TEXT, FUZZY_KEYWORD, SORT_KEY -> ValueEncoding.DECODE;
        };
    }

    @JacocoGenerated
    static String getErrorMessage(ParamKind kind) {
        return switch (kind) {
            case BOOLEAN, KEYWORD, FUZZY_TEXT, TEXT, FUZZY_KEYWORD, CUSTOM -> INVALID_VALUE;
            case DATE -> INVALID_DATE;
            case NUMBER -> INVALID_NUMBER;
            //            case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case SORT_KEY -> INVALID_VALUE_WITH_SORT;
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
            case KEYWORD, CUSTOM, FUZZY_TEXT, TEXT, FUZZY_KEYWORD, SORT_KEY -> PATTERN_IS_NON_EMPTY;
            case INVALID -> PATTERN_IS_NONE_OR_ONE;
        };
    }

    static int compareAscending(Enum<?> key1, Enum<?> key2) {
        return key1.ordinal() - key2.ordinal();
    }


    enum ValueEncoding {
        NONE, DECODE, DECODE_SANITIZED
    }

    enum ParamKind {
        INVALID, BOOLEAN, DATE, NUMBER, KEYWORD, FUZZY_KEYWORD, TEXT, FUZZY_TEXT, SORT_KEY, CUSTOM
    }

    enum FieldOperator {
        MUST, MUST_NOT, SHOULD, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, BETWEEN
    }
}