package no.unit.nva.search.common.bibliography;

import static java.util.function.Predicate.not;
import static no.unit.nva.search.common.bibliography.SchemaOrgNodeReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

final class SchemaOrgPersonBuilder {

  private static final String CONTRIBUTORS_POINTER = "/entityDescription/contributors";
  private static final String IDENTITY_NAME_POINTER = "/identity/name";
  private static final String IDENTITY_ID_POINTER = "/identity/id";
  private static final String IDENTITY_ORC_ID_POINTER = "/identity/orcId";
  private static final String AFFILIATIONS_POINTER = "/affiliations";
  private static final String AFFILIATION_ID_POINTER = "/id";
  private static final String AFFILIATION_LABEL_EN_POINTER = "/labels/en";
  private static final String AFFILIATION_LABEL_NB_POINTER = "/labels/nb";
  private static final String ROLE_TYPE_POINTER = "/role/type";

  private static final String TYPE_PERSON = "Person";
  private static final String TYPE_ORGANIZATION = "Organization";

  static final String PROP_AUTHOR = "author";
  static final String PROP_EDITOR = "editor";
  static final String PROP_TRANSLATOR = "translator";
  static final String PROP_ILLUSTRATOR = "illustrator";
  static final String PROP_PRODUCER = "producer";
  static final String PROP_DIRECTOR = "director";
  static final String PROP_ACTOR = "actor";
  static final String PROP_COMPOSER = "composer";
  static final String PROP_CONTRIBUTOR = "contributor";

  private static final Map<String, String> ROLE_TO_PROPERTY =
      Map.ofEntries(
          Map.entry("Creator", PROP_AUTHOR),
          Map.entry("Writer", PROP_AUTHOR),
          Map.entry("Dramatist", PROP_AUTHOR),
          Map.entry("Screenwriter", PROP_AUTHOR),
          Map.entry("Librettist", PROP_AUTHOR),
          Map.entry("Editor", PROP_EDITOR),
          Map.entry("EditorialBoardMember", PROP_EDITOR),
          Map.entry("TranslatorAdapter", PROP_TRANSLATOR),
          Map.entry("Illustrator", PROP_ILLUSTRATOR),
          Map.entry("Producer", PROP_PRODUCER),
          Map.entry("Director", PROP_DIRECTOR),
          Map.entry("ArtisticDirector", PROP_DIRECTOR),
          Map.entry("Actor", PROP_ACTOR),
          Map.entry("Composer", PROP_COMPOSER));

  private SchemaOrgPersonBuilder() {} // NO-OP

  static Map<String, List<SchemaOrgPerson>> buildContributors(JsonNode doc) {
    var contributors = doc.at(CONTRIBUTORS_POINTER);
    if (contributors.isMissingNode() || !contributors.isArray()) {
      return Map.of();
    }
    var byRole = new LinkedHashMap<String, List<SchemaOrgPerson>>();
    StreamSupport.stream(contributors.spliterator(), false)
        .forEach(
            contributor ->
                text(contributor, IDENTITY_NAME_POINTER)
                    .filter(not(String::isBlank))
                    .ifPresent(
                        name -> {
                          var person = buildPerson(contributor, name);
                          var property = toSchemaOrgProperty(contributor);
                          byRole
                              .computeIfAbsent(property, ignored -> new ArrayList<>())
                              .add(person);
                        }));
    return byRole;
  }

  private static SchemaOrgPerson buildPerson(JsonNode contributor, String name) {
    return new SchemaOrgPerson(
        TYPE_PERSON,
        text(contributor, IDENTITY_ID_POINTER).orElse(null),
        text(contributor, IDENTITY_ORC_ID_POINTER).orElse(null),
        name,
        buildAffiliations(contributor).orElse(null));
  }

  private static Optional<List<SchemaOrgOrganization>> buildAffiliations(JsonNode contributor) {
    var affiliations = contributor.at(AFFILIATIONS_POINTER);
    if (affiliations.isMissingNode() || !affiliations.isArray() || affiliations.isEmpty()) {
      return Optional.empty();
    }
    var result =
        StreamSupport.stream(affiliations.spliterator(), false)
            .map(
                affiliation ->
                    new SchemaOrgOrganization(
                        TYPE_ORGANIZATION,
                        text(affiliation, AFFILIATION_ID_POINTER).orElse(null),
                        text(affiliation, AFFILIATION_LABEL_EN_POINTER)
                            .or(() -> text(affiliation, AFFILIATION_LABEL_NB_POINTER))
                            .orElse(null)))
            .toList();
    return result.isEmpty() ? Optional.empty() : Optional.of(result);
  }

  private static String toSchemaOrgProperty(JsonNode contributor) {
    return text(contributor, ROLE_TYPE_POINTER)
        .map(role -> ROLE_TO_PROPERTY.getOrDefault(role, PROP_CONTRIBUTOR))
        .orElse(PROP_CONTRIBUTOR);
  }
}
