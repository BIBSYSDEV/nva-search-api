package no.unit.nva.search.common.bibliography;

import static no.unit.nva.search.common.bibliography.SchemaOrgNodeReader.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.stream.StreamSupport;
import no.unit.nva.search.common.bibliography.SchemaOrgType.FieldCategory;

final class SchemaOrgPublicationContextBuilder {

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

  private static final String UNCONFIRMED_JOURNAL = "UnconfirmedJournal";
  private static final String ANTHOLOGY = "Anthology";
  private static final String LITERARY_ARTS_MONOGRAPH = "LiteraryArtsMonograph";

  private static final String TYPE_PERIODICAL = "Periodical";
  private static final String TYPE_PUBLICATION_VOLUME = "PublicationVolume";
  private static final String TYPE_PUBLICATION_ISSUE = "PublicationIssue";
  private static final String TYPE_BOOK = "Book";
  private static final String TYPE_BOOK_SERIES = "BookSeries";
  private static final String TYPE_ORGANIZATION = "Organization";

  private SchemaOrgPublicationContextBuilder() {} // NO-OP

  static SchemaOrgContainer buildIsPartOf(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case ARTICLE -> buildArticleIsPartOf(doc);
      case PRESENTATION -> buildPresentationIsPartOf(doc);
      case BOOK -> buildBookIsPartOf(doc);
      case CHAPTER -> buildChapterIsPartOf(doc);
      case REPORT -> buildReportIsPartOf(doc);
      default -> null;
    };
  }

  static String buildIsbn(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case BOOK ->
          text(doc, CONTEXT_ISBN_POINTER)
              .or(() -> extractFromManifestations(doc, "/isbnList/0"))
              .orElse(null);
      case REPORT, THESIS -> text(doc, CONTEXT_ISBN_POINTER).orElse(null);
      default -> null;
    };
  }

  static String buildNumberOfPages(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case BOOK ->
          text(doc, PAGES_MONOGRAPH_POINTER)
              .or(() -> extractFromManifestations(doc, "/pages/pages"))
              .orElse(null);
      case REPORT, THESIS -> text(doc, PAGES_MONOGRAPH_POINTER).orElse(null);
      default -> null;
    };
  }

  static SchemaOrgOrganization buildPublisher(JsonNode doc, FieldCategory category) {
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

  static String buildPageStart(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case ARTICLE, PRESENTATION, CHAPTER -> text(doc, PAGES_BEGIN_POINTER).orElse(null);
      default -> null;
    };
  }

  static String buildPageEnd(JsonNode doc, FieldCategory category) {
    return switch (category) {
      case ARTICLE, PRESENTATION, CHAPTER -> text(doc, PAGES_END_POINTER).orElse(null);
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
}
