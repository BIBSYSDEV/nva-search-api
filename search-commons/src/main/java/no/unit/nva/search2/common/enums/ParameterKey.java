package no.unit.nva.search2.common.enums;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_DATE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_NUMBER;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_BOOLEAN;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NUMBER;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ParameterKey {

    String asCamelCase();

    Float fieldBoost();

    ParameterKind fieldType();

    String fieldPattern();

    String valuePattern();

    ValueEncoding valueEncoding();

    Stream<String> searchFields();

    FieldOperator searchOperator();

    String errorMessage();

    static Predicate<ParameterKey> equalTo(String name) {
        return key -> name.matches(key.fieldPattern());
    }

    static int compareAscending(Enum<?> key1, Enum<?> key2) {
        return key1.ordinal() - key2.ordinal();
    }

    static ValueEncoding getEncoding(ParameterKind kind) {
        return switch (kind) {
            case INVALID, IGNORED, BOOLEAN, NUMBER -> ValueEncoding.NONE;
            default -> ValueEncoding.DECODE;
        };
    }

    static String getErrorMessage(ParameterKind kind) {
        return switch (kind) {
            case DATE -> INVALID_DATE;
            case NUMBER -> INVALID_NUMBER;
            case SORT_KEY -> INVALID_VALUE_WITH_SORT;
            case INVALID -> "Status INVALID should not raise an exception, Exception";
            default -> INVALID_VALUE;
        };
    }

    static String getValuePattern(ParameterKind kind, String pattern) {
        return nonNull(pattern) ? pattern : switch (kind) {
            case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            case INVALID -> PATTERN_IS_NONE_OR_ONE;
            default -> PATTERN_IS_NON_EMPTY;
        };
    }
}