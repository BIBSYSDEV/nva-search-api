package no.unit.nva.search.model.records;

import static no.unit.nva.search.model.records.Constants.SHARD_ID;
import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search.model.constant.ErrorMessages.MISSING_IDENTIFIER_IN_RESOURCE;
import static no.unit.nva.search.model.constant.ErrorMessages.MISSING_INDEX_NAME_IN_RESOURCE;
import static no.unit.nva.search.model.constant.Words.BODY;
import static no.unit.nva.search.model.constant.Words.CONSUMPTION_ATTRIBUTES;
import static no.unit.nva.search.model.constant.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search.model.constant.Words.RESOURCES;
import static no.unit.nva.search.model.constant.Words.TICKETS;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;

import nva.commons.core.StringUtils;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

import java.util.Objects;
import java.util.Optional;

public record IndexDocument(
        @JsonProperty(CONSUMPTION_ATTRIBUTES) EventConsumptionAttributes consumptionAttributes,
        @JsonProperty(BODY) JsonNode resource)
        implements JsonSerializable {

    public static final String IMPORT_CANDIDATE = "ImportCandidate";
    public static final String TICKET = "Ticket";
    public static final String RESOURCE = "Resource";

    public static IndexDocument fromJsonString(String json) {
        return attempt(() -> objectMapperWithEmpty.readValue(json, IndexDocument.class))
                .orElseThrow();
    }

    public IndexDocument validate() {
        Objects.requireNonNull(getIndexName());
        Objects.requireNonNull(getDocumentIdentifier());
        return this;
    }

    @JsonIgnore
    public String getType() {
        var indexName = consumptionAttributes.index();
        if (RESOURCES.equals(indexName)) {
            return RESOURCE;
        }
        if (TICKETS.equals(indexName)) {
            return TICKET;
        }
        if (IMPORT_CANDIDATES_INDEX.equals(indexName)) {
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
                .routing(SHARD_ID)
                .id(getDocumentIdentifier());
    }

    private String serializeResource() {
        return attempt(() -> objectMapperWithEmpty.writeValueAsString(resource)).orElseThrow();
    }
}
