package no.unit.nva.search.models;

import static no.unit.nva.search.IndexingClient.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.constants.ApplicationConstants;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

public class IndexDocument implements JsonSerializable {

    public static final String BODY = "body";
    public static final String CONSUMPTION_ATTRIBUTES = "consumptionAttributes";
    public static final String MISSING_IDENTIFIER_IN_RESOURCE = "Missing identifier in resource";
    public static final String MISSING_INDEX_NAME_IN_RESOURCE = "Missing index name in resource";
    private static final String IMPORT_CANDIDATE = "ImportCandidate";
    private static final String TICKET = "Ticket";
    private static final String RESOURCE = "Resource";
    @JsonProperty(CONSUMPTION_ATTRIBUTES)
    private final EventConsumptionAttributes consumptionAttributes;
    @JsonProperty(BODY)
    private final JsonNode resource;

    @JsonCreator
    public IndexDocument(@JsonProperty(CONSUMPTION_ATTRIBUTES) EventConsumptionAttributes consumptionAttributes,
                         @JsonProperty(BODY) JsonNode resource) {
        this.consumptionAttributes = consumptionAttributes;
        this.resource = resource;
    }

    public IndexDocument validate() {
        Objects.requireNonNull(getIndexName());
        Objects.requireNonNull(getDocumentIdentifier());
        return this;
    }

    @JsonIgnore
    public String getType() {
        var indexName = consumptionAttributes.getIndex();
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

    public static IndexDocument fromJsonString(String json) {
        return attempt(() -> objectMapper.readValue(json, IndexDocument.class)).orElseThrow();
    }

    @JacocoGenerated
    public EventConsumptionAttributes getConsumptionAttributes() {
        return consumptionAttributes;
    }

    @JacocoGenerated
    public JsonNode getResource() {
        return resource;
    }

    @JsonIgnore
    public String getIndexName() {
        return Optional.ofNullable(consumptionAttributes.getIndex())
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new RuntimeException(MISSING_INDEX_NAME_IN_RESOURCE));
    }

    @JsonIgnore
    public String getDocumentIdentifier() {
        return Optional.ofNullable(consumptionAttributes.getDocumentIdentifier())
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
        if (!(o instanceof IndexDocument)) {
            return false;
        }
        IndexDocument that = (IndexDocument) o;
        return Objects.equals(getConsumptionAttributes(), that.getConsumptionAttributes())
               && Objects.equals(getResource(), that.getResource());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getConsumptionAttributes(), getResource());
    }

    private String serializeResource() {
        return attempt(() -> objectMapper.writeValueAsString(resource)).orElseThrow();
    }
}
