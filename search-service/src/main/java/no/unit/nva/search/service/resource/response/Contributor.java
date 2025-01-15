package no.unit.nva.search.service.resource.response;

import static java.util.Objects.isNull;
import static no.unit.nva.search.model.constant.Words.ID;
import static no.unit.nva.search.model.constant.Words.NAME;
import static no.unit.nva.search.model.constant.Words.ORC_ID;
import static no.unit.nva.search.model.constant.Words.TYPE;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
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

  private static Set<Affiliation> affiliationsFromJsonNode(JsonNode affiliationNode) {
    var affiliations = new HashSet<Affiliation>();
    if (!affiliationNode.isMissingNode()) {
      affiliationNode
          .iterator()
          .forEachRemaining(aff -> affiliations.add(new Contributor.Affiliation(aff)));
    }
    return Collections.unmodifiableSet(affiliations);
  }

  public record Identity(URI id, String name, URI orcId) implements NodeUtils {

    public Identity(JsonNode identity) {
      this(identity.path(ID), identity.path(NAME), identity.path(ORC_ID));
    }

    public Identity(JsonNode id, JsonNode name, JsonNode orcId) {
      this(NodeUtils.toUri(id), isNull(name) ? null : name.asText(), NodeUtils.toUri(orcId));
    }
  }

  public record Affiliation(String id, String name, String type) {

    public Affiliation(JsonNode affiliation) {
      this(
          affiliation.get(ID).asText(),
          affiliation.get(NAME).asText(),
          affiliation.get(TYPE).asText());
    }
  }
}
