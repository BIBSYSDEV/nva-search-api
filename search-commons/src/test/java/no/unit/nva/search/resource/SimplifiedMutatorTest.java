package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.constants.Words.ISBN_LIST;
import static no.unit.nva.constants.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.constants.Words.REFERENCE;
import static no.unit.nva.search.resource.Constants.ALTERNATIVE_TITLES;
import static no.unit.nva.search.resource.Constants.MANIFESTATIONS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.file.Path;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import no.unit.nva.testutils.RandomDataGenerator;
import org.junit.jupiter.api.Test;

class SimplifiedMutatorTest {

  public static final String RESOURCE_DATASOURCE_JSON = "resource_datasource.json";

  public static final ObjectMapper objectMapper =
      RandomDataGenerator.objectMapper.copy().findAndRegisterModules();

  @Test
  void shouldNotThrowOnEmptyBody() {
    assertDoesNotThrow(
        () -> new SimplifiedMutator().transform(new ObjectMapper().createObjectNode()));
  }

  @Test
  void shouldOutputIdAndNoEmptyStringsIdIfOnlyIdIsProvidedAsInput() {
    var id = randomString();
    var input = new ObjectMapper().createObjectNode();
    input.set("id", new TextNode(id));
    var result = new SimplifiedMutator().transform(input);
    assertTrue(result.isObject());
    ObjectNode resultAsObject = (ObjectNode) result;
    resultAsObject
        .iterator()
        .forEachRemaining(
            jsonNode -> {
              if (jsonNode.isTextual()) {
                assertNotEquals("", jsonNode.textValue());
              }
            });
    assertFalse(input.path(ID).isMissingNode());
  }

  @Test
  void shouldKeepAllContributorsOMutating() throws JsonProcessingException {
    var json =
        new ObjectMapper().readTree(stringFromResources(Path.of(RESOURCE_DATASOURCE_JSON))).get(0);

    var contributorsInSampleJson = 1;

    var mutated = new SimplifiedMutator().transform(json);
    var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);
    assertThat(asDto.contributorsPreview().size(), is(equalTo(contributorsInSampleJson)));
    assertThat(asDto.contributorsCount(), is(equalTo(contributorsInSampleJson)));
  }

  @Test
  void shouldMapAlternativeTitles() throws JsonProcessingException {
    var language = randomString();
    var expectedAlternativeTitle = randomString();
    var input = new ObjectMapper().createObjectNode();
    var entityDescription = new ObjectMapper().createObjectNode();
    var alternativeTitles = new ObjectMapper().createObjectNode();
    alternativeTitles.put(language, expectedAlternativeTitle);
    entityDescription.set(ALTERNATIVE_TITLES, alternativeTitles);
    input.set(ENTITY_DESCRIPTION, entityDescription);

    var mutated = new SimplifiedMutator().transform(input);
    var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);
    assertThat(asDto.alternativeTitles().size(), is(equalTo(1)));
    assertThat(asDto.alternativeTitles().get(language), is(equalTo(expectedAlternativeTitle)));
  }

  @Test
  void shouldMapIsbnsForArtistics() throws JsonProcessingException {
    var isbn1 = randomString();
    var isbn2 = randomString();
    var input = new ObjectMapper().createObjectNode();
    var entityDescription = new ObjectMapper().createObjectNode();
    var reference = new ObjectMapper().createObjectNode();
    var publicationInstance = new ObjectMapper().createObjectNode();
    var manifistations = new ObjectMapper().createArrayNode();
    var manifistation = new ObjectMapper().createObjectNode();
    var isbnList = new ObjectMapper().createArrayNode();
    isbnList.add(new TextNode(isbn1));
    isbnList.add(new TextNode(isbn2));
    manifistation.set(ISBN_LIST, isbnList);
    manifistations.add(manifistation);
    publicationInstance.set(MANIFESTATIONS, manifistations);
    reference.set(PUBLICATION_INSTANCE, publicationInstance);
    entityDescription.set(REFERENCE, reference);
    input.set(ENTITY_DESCRIPTION, entityDescription);

    var mutated = new SimplifiedMutator().transform(input);
    var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);

    assertThat(asDto.otherIdentifiers().isbn(), hasItem(isbn1));
    assertThat(asDto.otherIdentifiers().isbn(), hasItem(isbn2));
  }

  @Test
  void shouldParseSampleFileWithNoExceptions() throws JsonProcessingException {
    var json =
        new ObjectMapper().readTree(stringFromResources(Path.of(RESOURCE_DATASOURCE_JSON))).get(0);
    var result = new SimplifiedMutator().transform(json);
    assertNotNull(result);
  }
}