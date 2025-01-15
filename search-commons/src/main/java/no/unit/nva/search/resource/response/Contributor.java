package no.unit.nva.search.resource.response;

import static java.util.Objects.isNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.LABELS;
import static no.unit.nva.constants.Words.NAME;
import static no.unit.nva.constants.Words.ORC_ID;
import static no.unit.nva.constants.Words.ROLE;
import static no.unit.nva.constants.Words.TYPE;
import static no.unit.nva.search.resource.Constants.SEQUENCE;
import static no.unit.nva.search.resource.SimplifiedMutator.CORRESPONDING_AUTHOR;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record Contributor(
    Identity identity,
    String role,
    Set<Affiliation> affiliations,
    boolean correspondingAuthor,
    int sequence) {

  public Contributor(
      JsonNode identity,
      JsonNode role,
      JsonNode affiliations,
      JsonNode correspondingAuthor,
      JsonNode sequence) {
    this(
        isNull(identity) ? null : new Identity(identity),
        isNull(role) ? null : role.asText(),
        isNull(affiliations) ? null : affiliationsFromJsonNode(affiliations),
        !isNull(correspondingAuthor) && correspondingAuthor.asBoolean(),
        isNull(role) ? 0 : sequence.asInt());
  }

  public Contributor(JsonNode contributor) {
    this(
        contributor.path(IDENTITY),
        contributor.path(ROLE).path(TYPE),
        contributor.path(AFFILIATIONS),
        contributor.path(CORRESPONDING_AUTHOR),
        contributor.path(SEQUENCE));
  }

  private static Set<Affiliation> affiliationsFromJsonNode(JsonNode affiliationNode) {
    var affiliations = new HashSet<Affiliation>();
    if (!affiliationNode.isMissingNode()) {
      affiliationNode
          .iterator()
          .forEachRemaining(aff -> affiliations.add(new Contributor.Affiliation(aff)));
    }
    return Collections.unmodifiableSet(affiliations);
  }

  public record Identity(URI id, String name, URI orcId) {

    public Identity(JsonNode identity) {
      this(identity.path(ID), identity.path(NAME), identity.path(ORC_ID));
    }

    public Identity(JsonNode id, JsonNode name, JsonNode orcId) {
      this(NodeUtils.toUri(id), isNull(name) ? null : name.asText(), NodeUtils.toUri(orcId));
    }
  }

  public record Affiliation(String id, String type, Map<String, String> labels) {

    public Affiliation(JsonNode affiliation) {
      this(
          affiliation.get(ID).asText(),
          affiliation.get(TYPE).asText(),
          jsonNodeMapToMap(affiliation.path(LABELS)));
    }

    private static Map<String, String> jsonNodeMapToMap(JsonNode source) {
      return dtoObjectMapper.convertValue(source, new TypeReference<>() {});
    }
  }
}