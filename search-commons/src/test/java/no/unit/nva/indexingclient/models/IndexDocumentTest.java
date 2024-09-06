package no.unit.nva.indexingclient.models;

import static no.unit.nva.indexingclient.IndexingClient.objectMapper;
import static no.unit.nva.indexingclient.models.IndexDocument.IMPORT_CANDIDATE;
import static no.unit.nva.indexingclient.models.IndexDocument.RESOURCE;
import static no.unit.nva.indexingclient.models.IndexDocument.TICKET;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static nva.commons.core.attempt.Try.attempt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.constants.ApplicationConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class IndexDocumentTest {

    public static Stream<IndexDocument> invalidConsumptionAttributes() {
        var consumptionAttributesMissingIndexName =
                new EventConsumptionAttributes(null, SortableIdentifier.next());
        var consumptionAttributesMissingDocumentIdentifier =
                new EventConsumptionAttributes(randomString(), null);
        return Stream.of(
                        consumptionAttributesMissingIndexName,
                        consumptionAttributesMissingDocumentIdentifier)
                .map(
                        consumptionAttributes ->
                                new IndexDocument(consumptionAttributes, randomJsonObject()));
    }

    private static ObjectNode randomJsonObject() {
        String json = randomJson();
        return attempt(() -> (ObjectNode) objectMapper.readTree(json)).orElseThrow();
    }

    static Stream<Arguments> nameProvider() {
        return Stream.of(
                Arguments.of(IMPORT_CANDIDATE, ApplicationConstants.IMPORT_CANDIDATES_INDEX),
                Arguments.of(TICKET, ApplicationConstants.TICKETS_INDEX),
                Arguments.of(RESOURCE, ApplicationConstants.RESOURCES_INDEX));
    }

    @Test
    void shouldReturnOpenSearchIndexRequestWithIndexNameSpecifiedByConsumptionAttributes() {
        var consumptionAttributes = randomConsumptionAttributes();
        var indexDocument = new IndexDocument(consumptionAttributes, randomJsonObject());
        var indexRequest = indexDocument.toIndexRequest();
        assertThat(indexRequest.index(), is(equalTo(consumptionAttributes.index())));
    }

    @Test
    void shouldReturnObjectWhenInputIsValidJsonString() throws JsonProcessingException {
        var indexDocument = new IndexDocument(randomConsumptionAttributes(), randomJsonObject());
        var json = objectMapper.writeValueAsString(indexDocument);
        var deserialized = IndexDocument.fromJsonString(json);
        assertThat(deserialized, is(equalTo(indexDocument)));
    }

    @Test
    void
            shouldReturnDocumentIdentifierOfContainedObjectWhenEventConsumptionAttributesContainIdentifier() {
        var indexDocument = new IndexDocument(randomConsumptionAttributes(), randomJsonObject());
        assertThat(
                indexDocument.getDocumentIdentifier(),
                is(equalTo(indexDocument.consumptionAttributes().documentIdentifier().toString())));
    }

    @Test
    void shouldThrowExceptionWhenEventConsumptionAttributesDoNotContainIndexName() {
        var consumptionAttributes = new EventConsumptionAttributes(null, SortableIdentifier.next());
        var indexDocument = new IndexDocument(consumptionAttributes, randomJsonObject());
        var error = assertThrows(RuntimeException.class, indexDocument::getIndexName);
        assertThat(
                error.getMessage(), containsString(IndexDocument.MISSING_INDEX_NAME_IN_RESOURCE));
    }

    @Test
    void shouldThrowExceptionWhenEventConsumptionAttributesDoNotContainDocumentIdentifier() {
        var consumptionAttributes = new EventConsumptionAttributes(randomString(), null);
        var indexDocument = new IndexDocument(consumptionAttributes, randomJsonObject());
        var error = assertThrows(RuntimeException.class, indexDocument::getDocumentIdentifier);
        assertThat(
                error.getMessage(), containsString(IndexDocument.MISSING_IDENTIFIER_IN_RESOURCE));
    }

    @Test
    void shouldFailWithInvalidType() {
        var consumptionAttributes =
                new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
        var indexDocument = new IndexDocument(consumptionAttributes, randomJsonObject());
        assertThrows(IllegalArgumentException.class, indexDocument::getType);
        assertNotNull(indexDocument.validate());
    }

    @ParameterizedTest(name = "Checking type:{0}")
    @MethodSource("nameProvider")
    void shouldUseGetTypeAsWell(String name, String indexName) {
        var consumptionAttributes =
                new EventConsumptionAttributes(indexName, SortableIdentifier.next());
        var indexDocument = new IndexDocument(consumptionAttributes, randomJsonObject());
        assertThat(indexDocument.getType(), is(name));
        assertNotNull(indexDocument.validate());
    }

    @ParameterizedTest(
            name = "should throw exception when validating and missing mandatory fields:{0}")
    @MethodSource("invalidConsumptionAttributes")
    void shouldThrowExceptionWhenValidatingAndMissingMandatoryFields(
            IndexDocument invalidIndexDocument) {
        assertThrows(Exception.class, invalidIndexDocument::validate);
    }

    private EventConsumptionAttributes randomConsumptionAttributes() {
        return new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
    }
}
