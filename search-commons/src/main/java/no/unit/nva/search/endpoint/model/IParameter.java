package no.unit.nva.search.endpoint.model;

import static no.unit.nva.constants.ErrorMessages.INVALID_BOOLEAN;
import static no.unit.nva.constants.ErrorMessages.INVALID_DATE;
import static no.unit.nva.constants.ErrorMessages.INVALID_NUMBER;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.constants.Words.UNDERSCORE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_BOOLEAN;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NUMBER;

import no.unit.nva.search.common.enums.ValueEncoding;
import no.unit.nva.search.endpoint.Parameter;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Stig Norland
 */
public interface IParameter extends ILabel {

    /**
     * @return {@link ParameterType}
     */
    ParameterType type();

    ParameterModality modality();

    Operator fieldOperator();

    Float fieldBoost();

    String patternOfKey();

    String patternOfValue();

    String errorMessage();

    Parameter subQueryReference();

    ValueEncoding encoding();

    IParameter subQuery();

    Stream<String> searchFields(boolean... isKeyWord);

    static Predicate<IParameter> equalTo(String name) {
        return key -> name.matches(key.patternOfKey());
    }

    static String defaultErrorMessage(ParameterType type) {
        return switch (type) {
            case DATE -> INVALID_DATE;
            case BOOLEAN -> INVALID_BOOLEAN;
            case NUMBER -> INVALID_NUMBER;
            case SORT_KEY -> INVALID_VALUE_WITH_SORT;
            case INVALID -> "Status INVALID should not raise an exception, Exception";
            default -> INVALID_VALUE;
        };
    }

    static String defaultValuePattern(ParameterType type) {
        return switch (type) {
            case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            case INVALID -> PATTERN_IS_NONE_OR_ONE;
            default -> PATTERN_IS_NON_EMPTY;
        };
    }

    static String defaultKeyPattern(String name) {
        return PATTERN_IS_IGNORE_CASE + name.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
    }
}
