package no.unit.nva.search.common.bibliography;

import static java.util.function.Predicate.not;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import no.unit.nva.search.common.bibliography.SchemaOrgType.FieldCategory;

@SuppressWarnings("PMD.GodClass")
final class SchemaOrgItemTransformer {

  private static final String ID_POINTER = "/id";
  private static final String HANDLE_POINTER = "/handle";
  private static final String INSTANCE_TYPE_POINTER =
      "/entityDescription/reference/publicationInstance/type";
  private static final String MAIN_TITLE_POINTER = "/entityDescription/mainTitle";
  private static final String ABSTRACT_POINTER = "/entityDescription/abstract";
  private static final String YEAR_POINTER = "/entityDescription/publicationDate/year";
  private static final String CONTRIBUTORS_POINTER = "/entityDescription/contributors";
  private static final String IDENTITY_NAME_POINTER = "/identity/name";
  private static final String IDENTITY_ID_POINTER = "/identity/id";
  private static final String IDENTITY_ORC_ID_POINTER = "/identity/orcId";
  private static final String AFFILIATIONS_POINTER = "/affiliations";
  private static final String AFFILIATION_ID_POINTER = "/id";
  private static final String AFFILIATION_LABEL_EN_POINTER = "/labels/en";
  private static final String AFFILIATION_LABEL_NB_POINTER = "/labels/nb";
  private static final String TAGS_POINTER = "/entityDescription/tags";
  private static final String DOI_POINTER = "/entityDescription/reference/doi";
  private static final String NVA_DOI_POINTER = "/doi";
  private static final String CONTEXT_TYPE_POINTER =
      "/entityDescription/reference/publicationContext/type";
  private static final String CONTEXT_NAME_POINTER =
      "/entityDescription/reference/publicationContext/name";
  private static final String CONTEXT_TITLE_POINTER =
      "/entityDescription/reference/publicationContext/title";
  private static final String CONTEXT_ONLINE_ISSN_POINTER =
      "/entityDescription/reference/publicationContext/onlineIssn";
  private static final String CONTEXT_PRINT_ISSN_POINTER =
      "/entityDescription/reference/publicationContext/printIssn";
  private static final String CONTEXT_PUBLISHER_NAME_POINTER =
      "/entityDescription/reference/publicationContext/publisher/name";
  private static final String CONTEXT_SERIES_NAME_POINTER =
      "/entityDescription/reference/publicationContext/series/name";
  private static final String CONTEXT_ISBN_POINTER =
      "/entityDescription/reference/publicationContext/isbnList/0";
  private static final String ANTHOLOGY_MAIN_TITLE_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/mainTitle";
  private static final String ANTHOLOGY_PUBLISHER_NAME_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/reference"
          + "/publicationContext/publisher/name";
  private static final String ANTHOLOGY_ISBN_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/reference"
          + "/publicationContext/isbnList/0";
  private static final String VOLUME_POINTER =
      "/entityDescription/reference/publicationInstance/volume";
  private static final String ISSUE_POINTER =
      "/entityDescription/reference/publicationInstance/issue";
  private static final String PAGES_BEGIN_POINTER =
      "/entityDescription/reference/publicationInstance/pages/begin";
  private static final String PAGES_END_POINTER =
      "/entityDescription/reference/publicationInstance/pages/end";
  private static final String PAGES_MONOGRAPH_POINTER =
      "/entityDescription/reference/publicationInstance/pages/pages";
  private static final String MANIFESTATIONS_POINTER =
      "/entityDescription/reference/publicationInstance/manifestations";
  private static final String ROLE_TYPE_POINTER = "/role/type";

  private static final String UNCONFIRMED_JOURNAL = "UnconfirmedJournal";
  private static final String ANTHOLOGY = "Anthology";
  private static final String LITERARY_ARTS_MONOGRAPH = "LiteraryArtsMonograph";

  private static final String TYPE_PERSON = "Person";
  private static final String TYPE_ORGANIZATION = "Organization";
  private static final String TYPE_PERIODICAL = "Periodical";
  private static final String TYPE_PUBLICATION_VOLUME = "PublicationVolume";
  private static final String TYPE_PUBLICATION_ISSUE = "PublicationIssue";
  private static final String TYPE_BOOK = "Book";
  private static final String TYPE_BOOK_SERIES = "BookSeries";

  private static final String PROP_AUTHOR = "author";
  private static final String PROP_EDITOR = "editor";
  private static final String PROP_TRANSLATOR = "translator";
  private static final String PROP_ILLUSTRATOR = "illustrator";
  private static final String PROP_PRODUCER = "producer";
  private static final String PROP_DIRECTOR = "director";
  private static final String PROP_ACTOR = "actor";
  private static final String PROP_COMPOSER = "composer";
  private static final String PROP_CONTRIBUTOR = "contributor";

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

  private SchemaOrgItemTransformer() {}

  static SchemaOrgItem transform(JsonNode doc) {
    var nvaType = text(doc, INSTANCE_TYPE_POINTER).orElse("");
    var id = text(doc, ID_POINTER).orElse("");
    var url = text(doc, HANDLE_POINTER).orElse(id);
    var resolvedUrl = url.isBlank() ? null : url;
    var category = SchemaOrgType.fieldCategory(nvaType);
    var contributors = buildContributors(doc);

    return new SchemaOrgItem(
        SchemaOrgType.toSchemaOrgType(nvaType),
        resolvedUrl,
        resolvedUrl,
        text(doc, MAIN_TITLE_POINTER).orElse(null),
        contributors.get(PROP_AUTHOR),
        contributors.get(PROP_EDITOR),
        contributors.get(PROP_TRANSLATOR),
        contributors.get(PROP_ILLUSTRATOR),
        contributors.get(PROP_PRODUCER),
        contributors.get(PROP_DIRECTOR),
        contributors.get(PROP_ACTOR),
        contributors.get(PROP_COMPOSER),
        contributors.get(PROP_CONTRIBUTOR),
        text(doc, YEAR_POINTER).orElse(null),
        text(doc, ABSTRACT_POINTER).orElse(null),
        buildKeywords(doc).orElse(null),
        buildDoi(doc).orElse(null),
        buildIsPartOf(doc, category),
        buildIsbn(doc, category),
        buildNumberOfPages(doc, category),
        buildPublisher(doc, category),
        buildPageStart(doc, category),
        buildPageEnd(doc, category));
  }

  private static Map<String, List<SchemaOrgPerson>> buildContributors(JsonNode doc) {
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

  private static SchemaOrgContainer buildIsPartOf(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case ARTICLE -> buildArticleIsPartOf(doc);
      case PRESENTATION -> buildPresentationIsPartOf(doc);
      case BOOK -> buildBookIsPartOf(doc);
      case CHAPTER -> buildChapterIsPartOf(doc);
      case REPORT -> buildReportIsPartOf(doc);
      default -> null;
    };
  }

  private static SchemaOrgContainer buildArticleIsPartOf(JsonNode doc) {
    var contextType = text(doc, CONTEXT_TYPE_POINTER).orElse("");
    var journalName =
        UNCONFIRMED_JOURNAL.equals(contextType)
            ? text(doc, CONTEXT_TITLE_POINTER)
            : text(doc, CONTEXT_NAME_POINTER);

    return journalName
        .map(
            name -> {
              var issn =
                  text(doc, CONTEXT_ONLINE_ISSN_POINTER)
                      .or(() -> text(doc, CONTEXT_PRINT_ISSN_POINTER))
                      .orElse(null);
              SchemaOrgContainer current = new SchemaOrgPeriodical(TYPE_PERIODICAL, name, issn);

              var volume = text(doc, VOLUME_POINTER);
              if (volume.isPresent()) {
                current =
                    new SchemaOrgPublicationVolume(TYPE_PUBLICATION_VOLUME, volume.get(), current);
              }

              var issue = text(doc, ISSUE_POINTER);
              if (issue.isPresent()) {
                current =
                    new SchemaOrgPublicationIssue(TYPE_PUBLICATION_ISSUE, issue.get(), current);
              }

              return current;
            })
        .orElse(null);
  }

  private static SchemaOrgContainer buildPresentationIsPartOf(JsonNode doc) {
    return text(doc, CONTEXT_NAME_POINTER)
        .map(name -> (SchemaOrgContainer) new SchemaOrgBook(TYPE_BOOK, name, null, null))
        .orElse(null);
  }

  private static SchemaOrgContainer buildBookIsPartOf(JsonNode doc) {
    return text(doc, CONTEXT_SERIES_NAME_POINTER)
        .map(name -> (SchemaOrgContainer) new SchemaOrgBookSeries(TYPE_BOOK_SERIES, name))
        .orElse(null);
  }

  private static SchemaOrgContainer buildChapterIsPartOf(JsonNode doc) {
    var contextType = text(doc, CONTEXT_TYPE_POINTER).orElse("");
    var isAnthology = ANTHOLOGY.equals(contextType);
    var bookTitle =
        isAnthology ? text(doc, ANTHOLOGY_MAIN_TITLE_POINTER) : text(doc, CONTEXT_NAME_POINTER);

    return bookTitle
        .map(
            title -> {
              var isbn =
                  isAnthology
                      ? text(doc, ANTHOLOGY_ISBN_POINTER).orElse(null)
                      : text(doc, CONTEXT_ISBN_POINTER).orElse(null);
              var publisher =
                  (isAnthology
                          ? text(doc, ANTHOLOGY_PUBLISHER_NAME_POINTER)
                          : text(doc, CONTEXT_PUBLISHER_NAME_POINTER))
                      .map(
                          publisherName ->
                              new SchemaOrgOrganization(TYPE_ORGANIZATION, null, publisherName))
                      .orElse(null);
              return (SchemaOrgContainer) new SchemaOrgBook(TYPE_BOOK, title, isbn, publisher);
            })
        .orElse(null);
  }

  private static SchemaOrgContainer buildReportIsPartOf(JsonNode doc) {
    return text(doc, CONTEXT_SERIES_NAME_POINTER)
        .map(name -> (SchemaOrgContainer) new SchemaOrgPeriodical(TYPE_PERIODICAL, name, null))
        .orElse(null);
  }

  private static String buildIsbn(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case BOOK ->
          text(doc, CONTEXT_ISBN_POINTER)
              .or(() -> extractFromManifestations(doc, "/isbnList/0"))
              .orElse(null);
      case REPORT, THESIS -> text(doc, CONTEXT_ISBN_POINTER).orElse(null);
      default -> null;
    };
  }

  private static String buildNumberOfPages(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case BOOK ->
          text(doc, PAGES_MONOGRAPH_POINTER)
              .or(() -> extractFromManifestations(doc, "/pages/pages"))
              .orElse(null);
      case REPORT, THESIS -> text(doc, PAGES_MONOGRAPH_POINTER).orElse(null);
      default -> null;
    };
  }

  private static SchemaOrgOrganization buildPublisher(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case BOOK ->
          text(doc, CONTEXT_PUBLISHER_NAME_POINTER)
              .or(() -> extractFromManifestations(doc, "/publisher/name"))
              .map(name -> new SchemaOrgOrganization(TYPE_ORGANIZATION, null, name))
              .orElse(null);
      case REPORT, THESIS ->
          text(doc, CONTEXT_PUBLISHER_NAME_POINTER)
              .map(name -> new SchemaOrgOrganization(TYPE_ORGANIZATION, null, name))
              .orElse(null);
      default -> null;
    };
  }

  private static String buildPageStart(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case ARTICLE, PRESENTATION, CHAPTER -> text(doc, PAGES_BEGIN_POINTER).orElse(null);
      default -> null;
    };
  }

  private static String buildPageEnd(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case ARTICLE, PRESENTATION, CHAPTER -> text(doc, PAGES_END_POINTER).orElse(null);
      default -> null;
    };
  }

  private static Optional<String> buildKeywords(JsonNode doc) {
    var tags = doc.at(TAGS_POINTER);
    if (tags.isMissingNode() || !tags.isArray()) {
      return Optional.empty();
    }
    var joined =
        StreamSupport.stream(tags.spliterator(), false)
            .map(JsonNode::asText)
            .filter(not(String::isBlank))
            .collect(Collectors.joining(", "));
    return joined.isEmpty() ? Optional.empty() : Optional.of(joined);
  }

  private static Optional<String> buildDoi(JsonNode doc) {
    return text(doc, DOI_POINTER).or(() -> text(doc, NVA_DOI_POINTER)).filter(not(String::isBlank));
  }

  private static Optional<String> extractFromManifestations(JsonNode doc, String fieldPointer) {
    var manifestations = doc.at(MANIFESTATIONS_POINTER);
    if (manifestations.isMissingNode() || !manifestations.isArray()) {
      return Optional.empty();
    }
    return StreamSupport.stream(manifestations.spliterator(), false)
        .filter(
            manifestation -> LITERARY_ARTS_MONOGRAPH.equals(manifestation.path("type").asText()))
        .flatMap(manifestation -> text(manifestation, fieldPointer).stream())
        .findFirst();
  }

  private static String toSchemaOrgProperty(JsonNode contributor) {
    return text(contributor, ROLE_TYPE_POINTER)
        .map(role -> ROLE_TO_PROPERTY.getOrDefault(role, PROP_CONTRIBUTOR))
        .orElse(PROP_CONTRIBUTOR);
  }

  static Optional<String> text(JsonNode node, String pointer) {
    var value = node.at(pointer);
    if (value.isMissingNode() || value.isNull()) {
      return Optional.empty();
    }
    var text = value.asText();
    return text.isBlank() ? Optional.empty() : Optional.of(text);
  }
}
