package no.unit.nva.search.common;

import static no.unit.nva.constants.Words.COMMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AsTypeTest {

  private static final String EXPECTED_STATUS = "New";

  @ParameterizedTest
  @ValueSource(strings = {"New", "new", "Pending,New", "Pending, New "})
  void shouldContainValueWhenOneOfTheValuesMatches(String value) {
    var parameter = new AsType(value);

    assertThat(parameter.containsValue(EXPECTED_STATUS)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Newer", "NotNew", "Pending", "Pending,Closed", ""})
  void shouldNotContainValueWhenNoValueMatchesExactly(String value) {
    var parameter = new AsType(value);

    assertThat(parameter.containsValue(EXPECTED_STATUS)).isFalse();
  }

  @Test
  void shouldNotContainValueWhenThereIsNoValue() {
    var parameter = new AsType(null);

    assertThat(parameter.containsValue(EXPECTED_STATUS)).isFalse();
  }

  @Test
  void shouldDropBlankPartsWhenSplitting() {
    var parameter = new AsType("Pending,, ,New");

    assertThat(parameter.split(COMMA)).containsExactly("Pending", "New");
  }

  @Test
  void shouldSplitToEmptyArrayWhenThereIsNoValue() {
    var parameter = new AsType(null);

    assertThat(parameter.split(COMMA)).isEmpty();
  }

  @Test
  void shouldReadMissingValueAsEmptyString() {
    var parameter = new AsType(null);

    assertThat(parameter.asString()).isEmpty();
  }

  @Test
  void shouldReadValueAsInteger() {
    var parameter = new AsType("42");

    assertThat(parameter.asInteger()).isEqualTo(42);
  }

  @Test
  void shouldFailToReadMissingValueAsInteger() {
    var parameter = new AsType(null);

    assertThatThrownBy(parameter::asInteger).isInstanceOf(NumberFormatException.class);
  }
}
