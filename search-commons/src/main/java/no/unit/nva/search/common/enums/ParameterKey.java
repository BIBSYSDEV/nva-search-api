package no.unit.nva.search.common.enums;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.ErrorMessages.INVALID_BOOLEAN;
import static no.unit.nva.constants.ErrorMessages.INVALID_DATE;
import static no.unit.nva.constants.ErrorMessages.INVALID_NUMBER;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE;
import static no.unit.nva.constants.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_BOOLEAN;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.KEYWORD;
import static nva.commons.core.StringUtils.EMPTY_STRING;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import no.unit.nva.constants.Words;

/**
 * Interface for defining the keys used in the search service.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public interface ParameterKey<K extends Enum<K> & ParameterKey<K>> {
  static Predicate<ParameterKey<?>> equalTo(String name) {
    return key -> name.matches(key.fieldPattern());
  }

  static int compareAscending(Enum<?> key1, Enum<?> key2) {
    return key1.ordinal() - key2.ordinal();
  }

  static ValueEncoding getEncoding(ParameterKind kind) {
    return switch (kind) {
      case INVALID, FLAG, BOOLEAN, NUMBER -> ValueEncoding.NONE;
      default -> ValueEncoding.DECODE;
    };
  }

  static String getErrorMessage(ParameterKind kind) {
    return switch (kind) {
      case DATE -> INVALID_DATE;
      case EXISTS -> INVALID_BOOLEAN;
      case NUMBER -> INVALID_NUMBER;
      case SORT_KEY -> INVALID_VALUE_WITH_SORT;
      case INVALID -> "Status INVALID should not raise an exception, Exception";
      default -> INVALID_VALUE;
    };
  }

  static String getValuePattern(ParameterKind kind, String pattern) {
    return nonNull(pattern)
        ? pattern
        : switch (kind) {
          case EXISTS -> PATTERN_IS_BOOLEAN;
          case DATE -> PATTERN_IS_DATE;
          case NUMBER -> PATTERN_IS_NUMBER;
          case INVALID -> PATTERN_IS_NONE_OR_ONE;
          default -> PATTERN_IS_NON_EMPTY;
        };
  }

  static Function<String, String> trimKeyword(ParameterKind parameterKind, boolean... isKeyWord) {
    return field ->
        isNotKeyword(parameterKind, isKeyWord)
            ? field.trim().replace(DOT + Words.KEYWORD, EMPTY_STRING)
            : field.trim();
  }

  static boolean isNotKeyword(ParameterKind parameterKind, boolean... isKeyWord) {
    var result = !(parameterKind.equals(KEYWORD) || parameterKind.equals(CUSTOM));
    return isKeyWord.length == 1 ? !isKeyWord[0] : result;
  }

  String asCamelCase();

  String asLowerCase();

  Float fieldBoost();

  /**
   * @return {@link ParameterKind}
   */
  ParameterKind fieldType();

  String fieldPattern();

  String valuePattern();

  ValueEncoding valueEncoding();

  Stream<String> searchFields(boolean... isKeyWord);

  FieldOperator searchOperator();

  String errorMessage();

  K subQuery();
}
