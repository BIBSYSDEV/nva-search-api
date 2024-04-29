package no.unit.nva.search2.common;

import static no.unit.nva.search2.common.constant.Words.RESOURCES;
import static no.unit.nva.search2.common.constant.Words.TICKETS;
import static no.unit.nva.search2.importcandidate.Constants.IMPORT_CANDIDATES_INDEX_NAME;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.StringUtils;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

import java.util.Optional;

public record IndexDocument(
    EventConsumptionAttributes consumptionAttributes,
    JsonNode resource
) implements JsonSerializable {

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
        if (RESOURCES.equals(indexName)) {
            return RESOURCE;
        }
        if (TICKETS.equals(indexName)) {
            return TICKET;
        }
        if (IMPORT_CANDIDATES_INDEX_NAME.equals(indexName)) {
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
        return attempt(() -> JsonUtils.singleLineObjectMapper.writeValueAsString(resource)).orElseThrow();
    }
}
