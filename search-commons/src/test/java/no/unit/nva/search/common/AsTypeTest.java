package no.unit.nva.search.common;

import static no.unit.nva.search.ticket.TicketParameter.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AsTypeTest {

  private static final String EXPECTED_STATUS = "New";

  @ParameterizedTest
  @ValueSource(strings = {"New", "new", "Pending,New", "Pending, New "})
  void shouldContainValueWhenOneOfTheValuesMatches(String value) {
    var parameter = new AsType<>(value, STATUS);

    assertThat(parameter.containsValue(EXPECTED_STATUS)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Newer", "NotNew", "Pending", "Pending,Closed", ""})
  void shouldNotContainValueWhenNoValueMatchesExactly(String value) {
    var parameter = new AsType<>(value, STATUS);

    assertThat(parameter.containsValue(EXPECTED_STATUS)).isFalse();
  }

  @Test
  void shouldNotContainValueWhenThereIsNoValue() {
    var parameter = new AsType<>(null, STATUS);

    assertThat(parameter.containsValue(EXPECTED_STATUS)).isFalse();
  }
}
