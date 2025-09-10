package no.unit.nva.indexingclient.models;

import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.constants.ErrorMessages.MISSING_IDENTIFIER_IN_RESOURCE;
import static no.unit.nva.constants.ErrorMessages.MISSING_INDEX_NAME_IN_RESOURCE;
import static no.unit.nva.constants.Words.BODY;
import static no.unit.nva.constants.Words.CONSUMPTION_ATTRIBUTES;
import static no.unit.nva.constants.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.ShardRoutingService;
import nva.commons.core.StringUtils;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

public record IndexDocument(
    @JsonProperty(CONSUMPTION_ATTRIBUTES) EventConsumptionAttributes consumptionAttributes,
    @JsonProperty(BODY) JsonNode resource)
    implements JsonSerializable {

  static final String IMPORT_CANDIDATE = "ImportCandidate";
  static final String TICKET = "Ticket";
  static final String RESOURCE = "Resource";
  private static final String INDEX_DOCUMENT_CREATED_AT = "indexDocumentCreatedAt";

  public IndexDocument {
    if (resource instanceof ObjectNode objectNode && !objectNode.has(INDEX_DOCUMENT_CREATED_AT)) {
      objectNode.put(INDEX_DOCUMENT_CREATED_AT, Instant.now().toString());
    }
  }

  public static IndexDocument fromJsonString(String json) {
    return attempt(() -> objectMapperWithEmpty.readValue(json, IndexDocument.class)).orElseThrow();
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

  public IndexRequest toIndexRequest(ShardRoutingService shardRoutingService) {
    Objects.requireNonNull(shardRoutingService, "ShardRoutingService cannot be null");
    var routingKey = shardRoutingService.calculateRoutingKey(resource);
    return new IndexRequest(getIndexName())
        .source(serializeResource(), XContentType.JSON)
        .routing(routingKey)
        .id(getDocumentIdentifier());
  }

  private String serializeResource() {
    return attempt(() -> objectMapperWithEmpty.writeValueAsString(resource)).orElseThrow();
  }
}
