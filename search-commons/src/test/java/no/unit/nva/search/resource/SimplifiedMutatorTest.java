package no.unit.nva.search.resource;

import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.file.Path;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.jupiter.api.Test;

class SimplifiedMutatorTest {

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
                assertFalse(jsonNode.textValue().isEmpty());
              }
            });
    assertFalse(input.path("id").isMissingNode());
  }

  @Test
  void shouldKeepAllContributorsOMutating() throws JsonProcessingException {
    var json =
        new ObjectMapper()
            .readTree(stringFromResources(Path.of("resource_datasource.json")))
            .get(0);

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
    entityDescription.set("alternativeTitles", alternativeTitles);
    input.set("entityDescription", entityDescription);

    var mutated = new SimplifiedMutator().transform(input);
    var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);
    assertThat(asDto.alternativeTitles().size(), is(equalTo(1)));
    assertThat(asDto.alternativeTitles().get(language), is(equalTo(expectedAlternativeTitle)));
  }

  @Test
  void shouldMapTags() throws JsonProcessingException {
    var input = new ObjectMapper().createObjectNode();
    var entityDescription = new ObjectMapper().createObjectNode();
    var tags = new ObjectMapper().createArrayNode();
    tags.add("tag1");
    tags.add("tag2");
    entityDescription.set("tags", tags);
    input.set("entityDescription", entityDescription);

    var mutated = new SimplifiedMutator().transform(input);
    var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);
    assertThat(asDto.tags(), hasItems("tag1", "tag2"));
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
    manifistation.set("isbnList", isbnList);
    manifistations.add(manifistation);
    publicationInstance.set("manifestations", manifistations);
    reference.set("publicationInstance", publicationInstance);
    entityDescription.set("reference", reference);
    input.set("entityDescription", entityDescription);

    var mutated = new SimplifiedMutator().transform(input);
    var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);

    assertThat(asDto.otherIdentifiers().isbn(), hasItem(isbn1));
    assertThat(asDto.otherIdentifiers().isbn(), hasItem(isbn2));
  }

  @Test
  void shouldParseSampleFileWithNoExceptions() throws JsonProcessingException {
    var json =
        new ObjectMapper()
            .readTree(stringFromResources(Path.of("resource_datasource.json")))
            .get(0);
    var result = new SimplifiedMutator().transform(json);
    assertNotNull(result);
  }

  @Test
  void shouldMapParticipatingOrganizations() throws JsonProcessingException {
    var rootNode =
        JsonUtils.dtoObjectMapper.readTree(
            "{\n"
                + "\t\t\t\"identifier\": \"0198acf5d83b-fbce8171-fe14-4f58-9e71-c340e63062d7\",\n"
                + "\t\t\t\"contributorOrganizations\": [\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/194.0.0.0\",\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/7428.10.0.0\",\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\",\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\",\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\"\n"
                + "\t\t\t],\n"
                + "\t\t\t\"modelVersion\": \"0.23.3\",\n"
                + "\t\t\t\"resourceOwner\": {\n"
                + "\t\t\t\t\"owner\": \"ffi@7428.0.0.0\",\n"
                + "\t\t\t\t\"ownerAffiliation\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\"\n"
                + "\t\t\t},\n"
                + "\t\t\t\"handle\": \"https://hdl.handle.net/11250/2825856\",\n"
                + "\t\t\t\"type\": \"Publication\",\n"
                + "\t\t\t\"@context\": \"https://api.test.nva.aws.unit.no/publication/context\",\n"
                + "\t\t\t\"curatingInstitutions\": [\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/194.0.0.0\",\n"
                + "\t\t\t\t\"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\"\n"
                + "\t\t\t],\n"
                + "\t\t\t\"entityDescription\": {\n"
                + "\t\t\t\t\"reference\": {\n"
                + "\t\t\t\t\t\"type\": \"Reference\",\n"
                + "\t\t\t\t\t\"publicationInstance\": {\n"
                + "\t\t\t\t\t\t\"volume\": \"21\",\n"
                + "\t\t\t\t\t\t\"pages\": {\n"
                + "\t\t\t\t\t\t\t\"end\": \"510\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Range\",\n"
                + "\t\t\t\t\t\t\t\"begin\": \"501\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"type\": \"AcademicArticle\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t\"publicationContext\": {\n"
                + "\t\t\t\t\t\t\"identifier\": \"437272D6-8E65-4A6E-AE11-80D499BDBF1F\",\n"
                + "\t\t\t\t\t\t\"year\": \"2020\",\n"
                + "\t\t\t\t\t\t\"name\": \"Annals of Computer Science and Information Systems\",\n"
                + "\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/publication-channels-v2/serial-publication/437272D6-8E65-4A6E-AE11-80D499BDBF1F/2020\",\n"
                + "\t\t\t\t\t\t\"type\": \"Journal\",\n"
                + "\t\t\t\t\t\t\"onlineIssn\": \"2300-5963\",\n"
                + "\t\t\t\t\t\t\"scientificValue\": \"LevelOne\",\n"
                + "\t\t\t\t\t\t\"sameAs\":"
                + " \"https://kanalregister.hkdir.no/publiseringskanaler/info/tidsskrift?pid=437272D6-8E65-4A6E-AE11-80D499BDBF1F\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t\"doi\": \"https://doi.org/10.15439/2020F99\"\n"
                + "\t\t\t\t},\n"
                + "\t\t\t\t\"contributorsCount\": 5,\n"
                + "\t\t\t\t\"mainTitle\": \"A LoRa Mesh Network Asset Tracking Prototype\",\n"
                + "\t\t\t\t\"alternativeAbstracts\": {},\n"
                + "\t\t\t\t\"language\": \"http://lexvo.org/id/iso639-3/eng\",\n"
                + "\t\t\t\t\"abstract\": \"—Long Range (LoRa) is a low powered wide area\\r"
                + "\\n"
                + "communications technology, which uses radio frequencies in\\r"
                + "\\n"
                + "the industrial, scientific and medical (ISM) band to transmit\\r"
                + "\\n"
                + "data over long distances. Due to these properties, i.e., the long\\r"
                + "\\n"
                + "range and little restrictions on deployment and use, LoRa is\\r"
                + "\\n"
                + "a good candidate for building an asset tracking application\\r"
                + "\\n"
                + "on, for example targeting search and rescue operations. This\\r"
                + "\\n"
                + "paper describes the development and testing of such a prototype,\\r"
                + "\\n"
                + "using commercial off-the-shelf Internet of Things (IoT) consumer\\r"
                + "\\n"
                + "devices and a proprietary mesh protocol.\\r"
                + "\\n"
                + "The prototype enables distributed position tracking utilizing\\r"
                + "\\n"
                + "the Global Positioning System (GPS), a gateway to the Internet, a\\r"
                + "\\n"
                + "server for data storage and analysis, as well as a Web application\\r"
                + "\\n"
                + "for visualizing position tracking data. The devices are small,\\r"
                + "\\n"
                + "and our tests have included both personnel on foot carrying\\r"
                + "\\n"
                + "the equipment, as well as having the devices on vehicles.\\r"
                + "\\n"
                + "Index Terms—Internet of Things, Wireless mesh networks,\\r"
                + "\\n"
                + "Web services\\r"
                + "\\n"
                + "\",\n"
                + "\t\t\t\t\"contributors\": [\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"1\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Emil Andersen\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1272213\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"2\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Thomas Blaalid\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269511\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"3\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Hans Engstad\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269512\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"4\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Sigve Røkenes\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269510\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"5\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Frank T. Johnsen\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/24646\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.10.0.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Strategiske analyser og fellessystemer\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Strategic Analyses and Joint Systems\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t],\n"
                + "\t\t\t\t\"contributorsPreview\": [\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"1\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Emil Andersen\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1272213\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"2\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Thomas Blaalid\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269511\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"3\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Hans Engstad\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269512\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"4\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Sigve Røkenes\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269510\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t},\n"
                + "\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\"sequence\": \"5\",\n"
                + "\t\t\t\t\t\t\"role\": {\n"
                + "\t\t\t\t\t\t\t\"type\": \"Creator\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"identity\": {\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Frank T. Johnsen\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/24646\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t\"correspondingAuthor\": \"false\",\n"
                + "\t\t\t\t\t\t\"affiliations\": [\n"
                + "\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.10.0.0\",\n"
                + "\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\"nb\": \"Strategiske analyser og fellessystemer\",\n"
                + "\t\t\t\t\t\t\t\t\t\"en\": \"Strategic Analyses and Joint Systems\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\"type\": \"Contributor\"\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t],\n"
                + "\t\t\t\t\"type\": \"EntityDescription\",\n"
                + "\t\t\t\t\"publicationDate\": {\n"
                + "\t\t\t\t\t\"year\": \"2020\",\n"
                + "\t\t\t\t\t\"type\": \"PublicationDate\"\n"
                + "\t\t\t\t}\n"
                + "\t\t\t},\n"
                + "\t\t\t\"createdDate\": \"2025-08-15T09:00:49.595425755Z\",\n"
                + "\t\t\t\"publicationContextUris\": [\n"
                + "\t\t\t"
                + "\t\"https://api.test.nva.aws.unit.no/publication-channels-v2/serial-publication/437272D6-8E65-4A6E-AE11-80D499BDBF1F/2020\"\n"
                + "\t\t\t],\n"
                + "\t\t\t\"modifiedDate\": \"2025-08-15T09:00:49.595425755Z\",\n"
                + "\t\t\t\"additionalIdentifiers\": [\n"
                + "\t\t\t\t{\n"
                + "\t\t\t\t\t\"sourceName\": \"cristin@ffi\",\n"
                + "\t\t\t\t\t\"type\": \"CristinIdentifier\",\n"
                + "\t\t\t\t\t\"value\": \"1867879\"\n"
                + "\t\t\t\t},\n"
                + "\t\t\t\t{\n"
                + "\t\t\t\t\t\"sourceName\": \"cristin@ffi\",\n"
                + "\t\t\t\t\t\"type\": \"ScopusIdentifier\",\n"
                + "\t\t\t\t\t\"value\": \"2-s2.0-85095785380\"\n"
                + "\t\t\t\t}\n"
                + "\t\t\t],\n"
                + "\t\t\t\"publisher\": {\n"
                + "\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/customer/0baf8fcb-b18d-4c09-88bb-956b4f659103\",\n"
                + "\t\t\t\t\"type\": \"Organization\"\n"
                + "\t\t\t},\n"
                + "\t\t\t\"filesStatus\": \"noFiles\",\n"
                + "\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/publication/0198acf5d83b-fbce8171-fe14-4f58-9e71-c340e63062d7\",\n"
                + "\t\t\t\"pendingOpenFileCount\": \"0\",\n"
                + "\t\t\t\"publishedDate\": \"2025-08-15T09:00:49.595425755Z\",\n"
                + "\t\t\t\"scientificIndex\": {\n"
                + "\t\t\t\t\"year\": \"2020\",\n"
                + "\t\t\t\t\"type\": \"ScientificIndex\",\n"
                + "\t\t\t\t\"status\": \"Reported\"\n"
                + "\t\t\t},\n"
                + "\t\t\t\"topLevelOrganizations\": [\n"
                + "\t\t\t\t{\n"
                + "\t\t\t\t\t\"hasPart\": [\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.10.0.0\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\"nb\": \"Strategiske analyser og fellessystemer\",\n"
                + "\t\t\t\t\t\t\t\t\"en\": \"Strategic Analyses and Joint Systems\"\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t],\n"
                + "\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/7428.0.0.0\",\n"
                + "\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\"contributorCristinIds\": [\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Frank T. Johnsen\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/24646\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t],\n"
                + "\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\"nn\": \"Forsvarets forskingsinstitutt\",\n"
                + "\t\t\t\t\t\t\"nb\": \"Forsvarets forskningsinstitutt\",\n"
                + "\t\t\t\t\t\t\"en\": \"Norwegian Defence Research Establishment\"\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t},\n"
                + "\t\t\t\t{\n"
                + "\t\t\t\t\t\"hasPart\": [\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.0.0.0\"\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\"hasPart\": [\n"
                + "\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\"partOf\": [\n"
                + "\t\t\t\t\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\"\n"
                + "\t\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.10.0\",\n"
                + "\t\t\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\t\t\"nb\": \"Institutt for datateknologi og informatikk\",\n"
                + "\t\t\t\t\t\t\t\t\t\t\"en\": \"Department of Computer Science\"\n"
                + "\t\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t\t],\n"
                + "\t\t\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.63.0.0\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\t\t\"nb\": \"Fakultet for informasjonsteknologi og"
                + " elektroteknikk\",\n"
                + "\t\t\t\t\t\t\t\t\"en\": \"Faculty of Information Technology and Electrical"
                + " Engineering\"\n"
                + "\t\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t],\n"
                + "\t\t\t\t\t\"countryCode\": \"NO\",\n"
                + "\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/organization/194.0.0.0\",\n"
                + "\t\t\t\t\t\"type\": \"Organization\",\n"
                + "\t\t\t\t\t\"contributorCristinIds\": [\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Emil Andersen\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1272213\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Thomas Blaalid\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269511\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Sigve Røkenes\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269510\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t},\n"
                + "\t\t\t\t\t\t{\n"
                + "\t\t\t\t\t\t\t\"verificationStatus\": \"Verified\",\n"
                + "\t\t\t\t\t\t\t\"name\": \"Hans Engstad\",\n"
                + "\t\t\t\t\t\t\t\"id\":"
                + " \"https://api.test.nva.aws.unit.no/cristin/person/1269512\",\n"
                + "\t\t\t\t\t\t\t\"type\": \"Identity\"\n"
                + "\t\t\t\t\t\t}\n"
                + "\t\t\t\t\t],\n"
                + "\t\t\t\t\t\"labels\": {\n"
                + "\t\t\t\t\t\t\"nn\": \"Noregs teknisk-naturvitskaplege universitet\",\n"
                + "\t\t\t\t\t\t\"nb\": \"Norges teknisk-naturvitenskapelige universitet\",\n"
                + "\t\t\t\t\t\t\"en\": \"Norwegian University of Science and Technology\"\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t}\n"
                + "\t\t\t],\n"
                + "\t\t\t\"status\": \"PUBLISHED\"\n"
                + "\t\t}");

    var simplifiedMutator = new SimplifiedMutator();
    var mutated = simplifiedMutator.transform(rootNode);
    var response = JsonUtils.dtoObjectMapper.convertValue(mutated, ResourceSearchResponse.class);

    assertThat(response.participatingOrganizations(), IsIterableWithSize.iterableWithSize(2));
  }
}
