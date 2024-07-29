package no.unit.nva.indexingclient.models;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.Optional;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.constants.ApplicationConstants;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

public record IndexDocument(
    @JsonProperty(CONSUMPTION_ATTRIBUTES) EventConsumptionAttributes consumptionAttributes,
    @JsonProperty(BODY) JsonNode resource
) implements JsonSerializable {

    public static final String BODY = "body";
    public static final String CONSUMPTION_ATTRIBUTES = "consumptionAttributes";
    public static final String MISSING_IDENTIFIER_IN_RESOURCE = "Missing identifier in resource";
    public static final String MISSING_INDEX_NAME_IN_RESOURCE = "Missing index name in resource";
    private static final String IMPORT_CANDIDATE = "ImportCandidate";
    private static final String TICKET = "Ticket";
    private static final String RESOURCE = "Resource";

    public static IndexDocument fromJsonString(String json) {
        return attempt(() -> IndexingClient.objectMapper.readValue(json, IndexDocument.class)).orElseThrow();
    }

    public IndexDocument validate() {
        Objects.requireNonNull(getIndexName());
        Objects.requireNonNull(getDocumentIdentifier());
        return this;
    }

    @JacocoGenerated
    @JsonIgnore
    public String getType() {
        var indexName = consumptionAttributes.index();
        if (ApplicationConstants.RESOURCES_INDEX.equals(indexName)) {
            return RESOURCE;
        }
        if (ApplicationConstants.TICKETS_INDEX.equals(indexName)) {
            return TICKET;
        }
        if (ApplicationConstants.IMPORT_CANDIDATES_INDEX.equals(indexName)) {
            return IMPORT_CANDIDATE;
        } else {
            throw new IllegalArgumentException("Unknown type!");
        }
    }

    @JsonIgnore
    public String getIndexName() {
        return Optional.ofNullable(consumptionAttributes.index())
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new RuntimeException(MISSING_INDEX_NAME_IN_RESOURCE));
    }

    @JsonIgnore
    public String getDocumentIdentifier() {
        return Optional.ofNullable(consumptionAttributes.documentIdentifier())
            .map(SortableIdentifier::toString)
            .orElseThrow(() -> new RuntimeException(MISSING_IDENTIFIER_IN_RESOURCE));
    }

    public IndexRequest toIndexRequest() {
        return new IndexRequest(getIndexName())
            .source(serializeResource(), XContentType.JSON)
            .id(getDocumentIdentifier());
    }


    private String serializeResource() {
        return attempt(() -> IndexingClient.objectMapper.writeValueAsString(resource)).orElseThrow();
    }
}
