package no.unit.nva.search2.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.constants.ApplicationConstants;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

import java.util.Objects;
import java.util.Optional;

import static no.unit.nva.search.IndexingClient.objectMapper;
import static nva.commons.core.attempt.Try.attempt;

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

    @JsonCreator
    public IndexDocument {
    }


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


    @Override
    @JacocoGenerated
    public EventConsumptionAttributes consumptionAttributes() {
        return consumptionAttributes;
    }

    @Override
    @JacocoGenerated
    public JsonNode resource() {
        return resource;
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

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexDocument that)) {
            return false;
        }
        return Objects.equals(consumptionAttributes(), that.consumptionAttributes())
            && Objects.equals(resource(), that.resource());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(consumptionAttributes(), resource());
    }

    private String serializeResource() {
        return attempt(() -> objectMapper.writeValueAsString(resource)).orElseThrow();
    }
}
