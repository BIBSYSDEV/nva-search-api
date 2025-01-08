package no.unit.nva.search.service.resource.response;

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
        new Identity(identity),
        role.asText(),
        affiliationsFromJsonNode(affiliations),
        correspondingAuthor.asBoolean(),
        sequence.asInt());
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

  public record Identity(URI id, String name, URI orcId) implements WithUri {

    public Identity(JsonNode identity) {
      this(
          WithUri.fromNode(identity.get(ID)),
          identity.get(NAME).asText(),
          WithUri.fromNode(identity.get(ORC_ID)));
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
