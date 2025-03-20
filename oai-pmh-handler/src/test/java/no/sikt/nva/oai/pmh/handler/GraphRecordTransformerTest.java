package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.xml.bind.JAXBElement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openarchives.oai.pmh.v2.ElementType;
import org.openarchives.oai.pmh.v2.OaiDcType;
import org.openarchives.oai.pmh.v2.RecordType;

public class GraphRecordTransformerTest {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  private final GraphRecordTransformer recordTransformer = new GraphRecordTransformer();

  @ParameterizedTest
  @MethodSource("inputProvidingEmptyOutputProvider")
  void shouldReturnEmptyListWhenInputIsNullOrEmpty(List<JsonNode> input) {
    var records = recordTransformer.transform(input);

    assertThat(records, is(emptyIterable()));
  }

  @Test
  void shouldReturnRecordsWithRecordValuesAndHeadersContainingIdentifierWhenValidContentIsPresent()
      throws JsonProcessingException {
    var hits = getSearchHits();

    var records = recordTransformer.transform(hits);
    assertThat(records, is(not(emptyIterable())));
    for (RecordType record : records) {
      JAXBElement<OaiDcType> type = (JAXBElement<OaiDcType>) record.getMetadata().getAny();
      assertThat(extractValue(type, "title"), is(notNullValue()));
      assertThat(extractValues(type, "creator"), is(not(emptyIterable())));
      assertThat(extractValue(type, "date"), is(notNullValue()));
      assertThat(extractValue(type, "type"), is(notNullValue()));
      assertThat(extractValue(type, "publisher"), is(notNullValue()));
      assertWellFormedHeader(record);
    }
  }

  private String extractValue(JAXBElement<OaiDcType> type, String localPart) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals(localPart))
        .findAny()
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .orElse(null);
  }

  private List<String> extractValues(JAXBElement<OaiDcType> type, String localPart) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals(localPart))
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .toList();
  }

  private static void assertWellFormedHeader(RecordType record) {
    var header = record.getHeader();
    assertThat(header.getIdentifier(), notNullValue());
    assertThat(header.getDatestamp(), notNullValue());
  }

  private static List<JsonNode> getSearchHits() throws JsonProcessingException {
    var json = IoUtils.stringFromResources(Path.of("hits.json"));
    var arrayNode = (ArrayNode) MAPPER.readTree(json);

    List<JsonNode> hitsList = new ArrayList<>();
    for (JsonNode node : arrayNode) {
      hitsList.add(node);
    }
    return hitsList;
  }

  static Stream<Arguments> inputProvidingEmptyOutputProvider() {
    return Stream.of(Arguments.of(Collections.emptyList(), null));
  }
}
