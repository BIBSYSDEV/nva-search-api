package no.unit.nva.constants;

import static no.unit.nva.constants.IndexMappingsAndSettings.IMPORT_CANDIDATE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IndexMappingsAndSettingsTest {
  @ParameterizedTest
  @MethodSource("indexMappingsAsJsonProvider")
  void shouldServeResourceMappingsAsJson(String json) {
    assertThat(json, is(notNullValue()));
  }

  @ParameterizedTest
  @MethodSource("indexSettingsAsJsonProvider")
  void shouldServeSettingsAsJson(String json) {
    assertThat(json, is(notNullValue()));
  }

  @ParameterizedTest
  @MethodSource("indexMappingsAsMapProvider")
  void shouldServeResourceMappingsAsMap(Map<String, Object> map) {
    assertThat(map, is(notNullValue()));
  }

  @ParameterizedTest
  @MethodSource("indexSettingsAsMapProvider")
  void shouldServeSettingsAsMap(Map<String, Object> map) {
    assertThat(map, is(notNullValue()));
  }

  static Stream<Arguments> indexMappingsAsJsonProvider() {
    return Stream.of(
        Arguments.argumentSet(
            "import candidate mappings as json", IMPORT_CANDIDATE_MAPPINGS.asJson()),
        Arguments.argumentSet("resource mappings as json", RESOURCE_MAPPINGS.asJson()),
        Arguments.argumentSet("ticket mappings as json", TICKET_MAPPINGS.asJson()));
  }

  static Stream<Arguments> indexSettingsAsJsonProvider() {
    return Stream.of(
        Arguments.argumentSet("resource settings as json", RESOURCE_SETTINGS.asJson()));
  }

  static Stream<Arguments> indexMappingsAsMapProvider() {
    return Stream.of(
        Arguments.argumentSet(
            "import candidate mappings as map", IMPORT_CANDIDATE_MAPPINGS.asMap()),
        Arguments.argumentSet("resource mappings as map", RESOURCE_MAPPINGS.asMap()),
        Arguments.argumentSet("ticket mappings as map", TICKET_MAPPINGS.asMap()));
  }

  static Stream<Arguments> indexSettingsAsMapProvider() {
    return Stream.of(Arguments.argumentSet("resource settings as map", RESOURCE_SETTINGS.asMap()));
  }
}
