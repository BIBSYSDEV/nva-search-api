package no.unit.nva.search.resource;

import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import no.unit.nva.search.resource.response.ResourceSearchResponse;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class SimplifiedResourceModelMutatorTest {

    @Test
    void shouldNotThrowOnEmptyBody() {
        assertDoesNotThrow(
                () ->
                        new SimplifiedResourceModelMutator()
                                .transform(new ObjectMapper().createObjectNode()));
    }

    @Test
    void shouldOutputIdAndNoEmptyStringsIdIfOnlyIdIsProvidedAsInput() {
        var id = randomString();
        var input = new ObjectMapper().createObjectNode();
        input.set("id", new TextNode(id));
        var result = new SimplifiedResourceModelMutator().transform(input);
        assertTrue(result.isObject());
        ObjectNode resultAsObject = (ObjectNode) result;
        resultAsObject
                .iterator()
                .forEachRemaining(
                        jsonNode -> {
                            if (jsonNode.isTextual()) {
                                assertFalse(jsonNode.textValue().equals(""));
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

        var mutated = new SimplifiedResourceModelMutator().transform(json);
        var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);
        assertThat(asDto.contributorsPreview().size(), is(equalTo(contributorsInSampleJson)));
        assertThat(asDto.contributorsCount(), is(equalTo(contributorsInSampleJson)));
    }

    @Test
    void shouldMapAlternativeTitles() throws JsonProcessingException {
        var expectedAlternativeTitle = randomString();
        var input = new ObjectMapper().createObjectNode();
        var entityDescription = new ObjectMapper().createObjectNode();
        var alternativeTitles = new ObjectMapper().createObjectNode();
        alternativeTitles.put("no", expectedAlternativeTitle);
        entityDescription.set("alternativeTitles", alternativeTitles);
        input.set("entityDescription", entityDescription);

        var mutated = new SimplifiedResourceModelMutator().transform(input);
        var asDto = objectMapper.treeToValue(mutated, ResourceSearchResponse.class);
        assertThat(asDto.alternativeTitles().size(), is(equalTo(1)));
        assertThat(asDto.alternativeTitles().get(0), is(equalTo(expectedAlternativeTitle)));
    }

    @Test
    void shouldParseSampleFileWithNoExceptions() throws JsonProcessingException {
        var json =
                new ObjectMapper()
                        .readTree(stringFromResources(Path.of("resource_datasource.json")))
                        .get(0);
        var result = new SimplifiedResourceModelMutator().transform(json);
        assertNotNull(result);
    }
}
