package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBElement;
import java.nio.file.Path;
import java.util.List;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.openarchives.oai.pmh.v2.ElementType;
import org.openarchives.oai.pmh.v2.OaiDcType;
import org.openarchives.oai.pmh.v2.RecordType;

public class PrototypeTest {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void shouldThrowIllegalArgumentExceptionWhenInputIsNotJsonArray() {
    assertThrows(IllegalArgumentException.class, () -> Prototype.from(MAPPER.createObjectNode()));
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
    assertThat(records, is(not(emptyIterable())));
    for (RecordType record : records) {
      OaiDcType type = (OaiDcType) record.getMetadata().getAny();
      var title = extractTitle(type);
      assertThat(title, is(notNullValue()));
      assertThat(extractCreators(type), is(not(emptyIterable())));
      assertThat(extractDate(type), is(notNullValue()));
      assertWellFormedHeader(record);
    }
  }

  private String extractDate(OaiDcType type) {
    return type.getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("date"))
        .findAny()
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .orElse(null);
  }

  private List<String> extractCreators(OaiDcType type) {
    return type.getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("creator"))
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .toList();
  }

  private String extractTitle(OaiDcType type) {
    return type.getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("title"))
        .findAny()
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .orElse(null);
  }

  private static void assertWellFormedHeader(RecordType record) {
    var header = record.getHeader();
    assertThat(header.getIdentifier(), notNullValue());
    assertThat(header.getDatestamp(), notNullValue());
  }

  private static JsonNode getSearchHits() throws JsonProcessingException {
    var json = IoUtils.stringFromResources(Path.of("hits.json"));
    return MAPPER.readTree(json);
  }
}
