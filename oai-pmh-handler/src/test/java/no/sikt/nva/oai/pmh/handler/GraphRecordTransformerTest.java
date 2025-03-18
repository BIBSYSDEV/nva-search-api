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
  void shouldReturnRecordsWithHeadersContainingIdWhenValidContentIsPresent()
      throws JsonProcessingException {
    var hits = getSearchHits();

    var records = recordTransformer.transform(hits);
    assertThat(records, is(not(emptyIterable())));
    for (RecordType record : records) {
      JAXBElement<OaiDcType> type = (JAXBElement<OaiDcType>) record.getMetadata().getAny();
      var title = extractTitle(type);
      assertThat(title, is(notNullValue()));
      assertThat(extractCreators(type), is(not(emptyIterable())));
      assertThat(extractDate(type), is(notNullValue()));
      assertThat(extractType(type), is(notNullValue()));
      assertThat(extractPublisher(type), is(notNullValue()));
      assertWellFormedHeader(record);
    }
  }

  private String extractType(JAXBElement<OaiDcType> type) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("type"))
        .findAny()
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .orElse(null);
  }

  private String extractDate(JAXBElement<OaiDcType> type) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("date"))
        .findAny()
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .orElse(null);
  }

  private List<String> extractCreators(JAXBElement<OaiDcType> type) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("creator"))
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .toList();
  }

  private String extractTitle(JAXBElement<OaiDcType> type) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("title"))
        .findAny()
        .map(JAXBElement::getValue)
        .map(ElementType::getValue)
        .orElse(null);
  }

  private String extractPublisher(JAXBElement<OaiDcType> type) {
    return type.getValue().getTitleOrCreatorOrSubject().stream()
        .filter(e -> e.getName().getLocalPart().equals("publisher"))
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
