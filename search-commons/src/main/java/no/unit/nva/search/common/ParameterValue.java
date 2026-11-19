package no.unit.nva.search.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.COMMA;
import static nva.commons.core.StringUtils.EMPTY_STRING;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A query parameter value, read as the type the caller needs.
 *
 * <p>A missing value reads as an empty string, stream or array, never as null.
 *
 * @author Stig Norland
 */
public class ParameterValue {

  private final String value;

  public ParameterValue(String value) {
    this.value = value;
  }

  private static Stream<String> splitParts(String delimiter, String value) {
    return Arrays.stream(value.split(delimiter)).filter(part -> !part.isBlank());
  }

  public String asString() {
    return nonNull(value) ? value : EMPTY_STRING;
  }

  /**
   * The value as an integer.
   *
   * @throws NumberFormatException if there is no value or it is not an integer
   */
  public int asInteger() {
    return Integer.parseInt(value);
  }

  public boolean isEmpty() {
    return isNull(value) || value.isEmpty();
  }

  public boolean isPresent() {
    return !isEmpty();
  }

  /**
   * Whether one of the comma-separated values equals the expected value, ignoring case and
   * surrounding whitespace.
   */
  public boolean containsValue(String expectedValue) {
    return asSplitStream(COMMA).map(String::strip).anyMatch(expectedValue::equalsIgnoreCase);
  }

  /**
   * Split.
   *
   * @param delimiter regex to split on
   * @return The non-blank parts of the value, or an empty array if there is no value.
   */
  public String[] split(String delimiter) {
    return asSplitStream(delimiter).toArray(String[]::new);
  }

  /**
   * AsSplitStream.
   *
   * @param delimiter regex to split on
   * @return The value as an optional Stream, split by delimiter.
   */
  public Stream<String> asSplitStream(String delimiter) {
    return asStream().flatMap(value -> splitParts(delimiter, value));
  }

  /**
   * AsStream.
   *
   * @return The value as an optional Stream.
   */
  public Stream<String> asStream() {
    return Optional.ofNullable(value).stream();
  }

  /**
   * AsBoolean.
   *
   * @return False if value is null or FALSE, otherwise True
   */
  public boolean asBoolean() {
    return Boolean.parseBoolean(value);
  }

  @Override
  public String toString() {
    return asString();
  }
}
