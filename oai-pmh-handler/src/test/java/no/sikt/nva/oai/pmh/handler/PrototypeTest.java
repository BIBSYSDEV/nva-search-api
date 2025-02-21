package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.openarchives.oai.pmh.v2.RecordType;

public class PrototypeTest {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void shouldThrowIllegalArgumentExceptionWhenInputIsNotJsonArray() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Prototype.from(new ObjectMapper().createObjectNode()));
  }

  @Test
  void shouldReturnEmptyListWhenInputIsEmptyArray() {
    var records = Prototype.from(MAPPER.createArrayNode());

    assertThat(records, is(emptyIterable()));
  }

  @Test
  void shouldReturnRecordsWithHeadersContainingIdWhenValidContentIsPresent()
      throws JsonProcessingException {
    var hits = getSearchHits();

    var records = Prototype.from(hits);
    for (RecordType record : records) {
      var header = record.getHeader();
      assertThat(header.getIdentifier(), notNullValue());
    }
  }

  private static JsonNode getSearchHits() throws JsonProcessingException {
    var json = IoUtils.stringFromResources(Path.of("hits.json"));
    return MAPPER.readTree(json);
  }
}
