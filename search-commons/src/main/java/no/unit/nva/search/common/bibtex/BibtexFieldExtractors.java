package no.unit.nva.search.common.bibtex;

import static java.util.function.Predicate.not;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class BibtexFieldExtractors {

  static final String ABSTRACT_POINTER = "/entityDescription/abstract";
  static final String MAIN_TITLE_POINTER = "/entityDescription/mainTitle";
  static final String YEAR_POINTER = "/entityDescription/publicationDate/year";
  static final String MONTH_POINTER = "/entityDescription/publicationDate/month";
  static final String TAGS_POINTER = "/entityDescription/tags";
  static final String CONTRIBUTORS_POINTER = "/entityDescription/contributors";
  static final String IDENTITY_NAME_POINTER = "/identity/name";
  static final String DOI_POINTER = "/entityDescription/reference/doi";
  static final String NVA_DOI_POINTER = "/doi";
  static final String CONTEXT_TYPE_POINTER = "/entityDescription/reference/publicationContext/type";
  static final String CONTEXT_NAME_POINTER = "/entityDescription/reference/publicationContext/name";
  static final String CONTEXT_TITLE_POINTER =
      "/entityDescription/reference/publicationContext/title";
  static final String CONTEXT_ONLINE_ISSN_POINTER =
      "/entityDescription/reference/publicationContext/onlineIssn";
  static final String CONTEXT_PRINT_ISSN_POINTER =
      "/entityDescription/reference/publicationContext/printIssn";
  static final String CONTEXT_PUBLISHER_NAME_POINTER =
      "/entityDescription/reference/publicationContext/publisher/name";
  static final String CONTEXT_SERIES_NAME_POINTER =
      "/entityDescription/reference/publicationContext/series/name";
  static final String ANTHOLOGY_MAIN_TITLE_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/mainTitle";
  static final String ANTHOLOGY_PUBLISHER_NAME_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/reference"
          + "/publicationContext/publisher/name";
  static final String CONTEXT_ISBN_POINTER =
      "/entityDescription/reference/publicationContext/isbnList/0";
  static final String ANTHOLOGY_ISBN_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/reference"
          + "/publicationContext/isbnList/0";
  static final String INSTANCE_TYPE_POINTER =
      "/entityDescription/reference/publicationInstance/type";
  static final String VOLUME_POINTER = "/entityDescription/reference/publicationInstance/volume";
  static final String ISSUE_POINTER = "/entityDescription/reference/publicationInstance/issue";
  static final String PAGES_BEGIN_POINTER =
      "/entityDescription/reference/publicationInstance/pages/begin";
  static final String PAGES_END_POINTER =
      "/entityDescription/reference/publicationInstance/pages/end";
  static final String PAGES_MONOGRAPH_POINTER =
      "/entityDescription/reference/publicationInstance/pages/pages";
  static final String MANIFESTATIONS_POINTER =
      "/entityDescription/reference/publicationInstance/manifestations";

  private static final String AND = " and ";
  private static final String INTERVAL = "--";
  private static final String EN_DASH = "–";
  private static final String DOI_URI_HOST_REGEX = "(?i)https?://doi\\.org/";
  private static final String CONTEXT_TYPE_UNCONFIRMED_JOURNAL = "UnconfirmedJournal";
  private static final String CONTEXT_TYPE_ANTHOLOGY = "Anthology";
  private static final String LITERARY_ARTS_MONOGRAPH_TYPE = "LiteraryArtsMonograph";
  private static final int JANUARY = 1;
  private static final int DECEMBER = 12;
  public static final String EMPTY_STRING = "";
  public static final String COMMA_SPACE_DELIMITER = ", ";

  private BibtexFieldExtractors() {}

  public static BibtexFieldExtractor text(String field, String pointer) {
    return doc -> extractText(doc, pointer).map(value -> new BibtexField(field, value));
  }

  public static BibtexFieldExtractor authors() {
    return doc -> {
      var contributors = doc.at(CONTRIBUTORS_POINTER);
      if (contributors.isMissingNode() || !contributors.isArray()) {
        return Optional.empty();
      }
      return StreamSupport.stream(contributors.spliterator(), false)
          .flatMap(contributor -> extractText(contributor, IDENTITY_NAME_POINTER).stream())
          .filter(not(String::isBlank))
          .collect(
              Collectors.collectingAndThen(
                  Collectors.joining(AND),
                  names ->
                      names.isEmpty()
                          ? Optional.empty()
                          : Optional.of(new BibtexField("author", names))));
    };
  }

  public static BibtexFieldExtractor doi() {
    return doc ->
        extractText(doc, DOI_POINTER)
            .or(() -> extractText(doc, NVA_DOI_POINTER))
            .map(doi -> doi.replaceFirst(DOI_URI_HOST_REGEX, EMPTY_STRING).strip())
            .filter(not(String::isBlank))
            .map(doi -> new BibtexField("doi", doi));
  }

  public static BibtexFieldExtractor issn() {
    return doc ->
        extractText(doc, CONTEXT_ONLINE_ISSN_POINTER)
            .or(() -> extractText(doc, CONTEXT_PRINT_ISSN_POINTER))
            .map(issn -> new BibtexField("issn", issn));
  }

  public static BibtexFieldExtractor journalName() {
    return doc -> {
      var contextType = extractText(doc, CONTEXT_TYPE_POINTER).orElse(EMPTY_STRING);
      return (CONTEXT_TYPE_UNCONFIRMED_JOURNAL.equals(contextType)
              ? extractText(doc, CONTEXT_TITLE_POINTER)
              : extractText(doc, CONTEXT_NAME_POINTER))
          .map(name -> new BibtexField("journal", name));
    };
  }

  public static BibtexFieldExtractor keywords() {
    return doc -> {
      var tags = doc.at(TAGS_POINTER);
      if (tags.isMissingNode() || !tags.isArray()) {
        return Optional.empty();
      }
      var joined =
          StreamSupport.stream(tags.spliterator(), false)
              .map(JsonNode::asText)
              .filter(not(String::isBlank))
              .collect(Collectors.joining(COMMA_SPACE_DELIMITER));
      return joined.isEmpty() ? Optional.empty() : Optional.of(new BibtexField("keywords", joined));
    };
  }

  public static BibtexFieldExtractor month() {
    return doc ->
        extractText(doc, MONTH_POINTER)
            .map(BibtexFieldExtractors::constructMonthName)
            .map(month -> new BibtexField("month", month));
  }

  public static BibtexFieldExtractor nvaTypeNote() {
    return doc ->
        extractText(doc, INSTANCE_TYPE_POINTER)
            .filter(not(String::isBlank))
            .map(type -> new BibtexField("note", "nva type: " + type));
  }

  public static BibtexFieldExtractor pages() {
    return doc ->
        extractText(doc, PAGES_BEGIN_POINTER)
            .map(
                begin ->
                    extractText(doc, PAGES_END_POINTER)
                        .map(end -> begin + INTERVAL + end)
                        .orElse(begin))
            .or(() -> extractText(doc, PAGES_MONOGRAPH_POINTER))
            .map(BibtexFieldExtractors::normalizePages)
            .map(pages -> new BibtexField("pages", pages));
  }

  public static BibtexFieldExtractor pagesOrManifestationPages() {
    return doc ->
        extractText(doc, PAGES_BEGIN_POINTER)
            .map(
                begin ->
                    extractText(doc, PAGES_END_POINTER)
                        .map(end -> begin + INTERVAL + end)
                        .orElse(begin))
            .or(() -> extractText(doc, PAGES_MONOGRAPH_POINTER))
            .or(() -> extractFromMonographManifestation(doc, "/pages/pages"))
            .map(BibtexFieldExtractors::normalizePages)
            .map(pages -> new BibtexField("pages", pages));
  }

  public static BibtexFieldExtractor isbn(String pointer) {
    return doc -> extractText(doc, pointer).map(isbn -> new BibtexField("isbn", isbn));
  }

  public static BibtexFieldExtractor isbnOrManifestationIsbn() {
    return doc ->
        extractText(doc, CONTEXT_ISBN_POINTER)
            .or(() -> extractFromMonographManifestation(doc, "/isbnList/0"))
            .map(isbn -> new BibtexField("isbn", isbn));
  }

  public static BibtexFieldExtractor publisherOrManifestationPublisher() {
    return doc ->
        extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER)
            .or(() -> extractFromMonographManifestation(doc, "/publisher/name"))
            .map(name -> new BibtexField("publisher", name));
  }

  public static BibtexFieldExtractor anthologyTitle() {
    return doc ->
        (isAnthologyContext(doc)
                ? extractText(doc, ANTHOLOGY_MAIN_TITLE_POINTER)
                : extractText(doc, CONTEXT_NAME_POINTER))
            .map(title -> new BibtexField("booktitle", title));
  }

  public static BibtexFieldExtractor anthologyIsbn() {
    return doc -> extractText(doc, ANTHOLOGY_ISBN_POINTER).map(isbn -> new BibtexField("isbn", isbn));
  }

  public static BibtexFieldExtractor anthologyPublisher() {
    return doc ->
        (isAnthologyContext(doc)
                ? extractText(doc, ANTHOLOGY_PUBLISHER_NAME_POINTER)
                : extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER))
            .map(name -> new BibtexField("publisher", name));
  }

  static Optional<String> extractText(JsonNode node, String pointer) {
    var value = node.at(pointer);
    if (value.isMissingNode() || value.isNull()) return Optional.empty();
    var text = value.asText();
    return text.isBlank() ? Optional.empty() : Optional.of(text);
  }

  private static boolean isAnthologyContext(JsonNode doc) {
    return CONTEXT_TYPE_ANTHOLOGY.equals(extractText(doc, CONTEXT_TYPE_POINTER).orElse(EMPTY_STRING));
  }

  private static Optional<String> extractFromMonographManifestation(
      JsonNode doc, String fieldPointer) {
    var manifestations = doc.at(MANIFESTATIONS_POINTER);
    if (manifestations.isMissingNode() || !manifestations.isArray()) {
      return Optional.empty();
    }
    return StreamSupport.stream(manifestations.spliterator(), false)
        .filter(manifestation -> LITERARY_ARTS_MONOGRAPH_TYPE.equals(manifestation.path("type").asText()))
        .flatMap(manifestation -> extractText(manifestation, fieldPointer).stream())
        .findFirst();
  }

  private static String normalizePages(String raw) {
    return raw.replace(EN_DASH, INTERVAL);
  }

  private static String constructMonthName(String raw) {
    try {
      int month = Integer.parseInt(raw.strip());
      if (month >= JANUARY && month <= DECEMBER) {
        return Month.of(month)
            .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ROOT)
            .toLowerCase(Locale.ROOT);
      }
    } catch (NumberFormatException ignored) {
      // already an abbreviation or other string
    }
    return raw;
  }
}
