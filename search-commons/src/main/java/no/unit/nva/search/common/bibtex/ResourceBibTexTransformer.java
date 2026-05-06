package no.unit.nva.search.common.bibtex;

import static java.util.function.Predicate.not;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NonNull;

public final class ResourceBibTexTransformer {

  private static final String MAIN_TITLE_POINTER = "/entityDescription/mainTitle";
  private static final String YEAR_POINTER = "/entityDescription/publicationDate/year";
  private static final String MONTH_POINTER = "/entityDescription/publicationDate/month";
  private static final String CONTRIBUTORS_POINTER = "/entityDescription/contributors";
  private static final String IDENTITY_NAME_POINTER = "/identity/name";
  private static final String DOI_POINTER = "/entityDescription/reference/doi";
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
  private static final String ANTHOLOGY_MAIN_TITLE_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/mainTitle";
  private static final String ANTHOLOGY_PUBLISHER_NAME_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/reference"
          + "/publicationContext/publisher/name";
  private static final String INSTANCE_TYPE_POINTER =
      "/entityDescription/reference/publicationInstance/type";
  private static final String VOLUME_POINTER =
      "/entityDescription/reference/publicationInstance/volume";
  private static final String ISSUE_POINTER =
      "/entityDescription/reference/publicationInstance/issue";
  private static final String PAGES_BEGIN_POINTER =
      "/entityDescription/reference/publicationInstance/pages/begin";
  private static final String PAGES_END_POINTER =
      "/entityDescription/reference/publicationInstance/pages/end";

  private static final String AND = " and ";
  private static final String ENTRY_SEPARATOR = "\n\n";
  private static final String PAGES = "pages";
  private static final String DOI_URI_HOST_REGEX = "(?i)https?://doi\\.org/";
  public static final String EMPTY_STRING = "";

  // NVA publication context types
  private static final String CONTEXT_TYPE_UNCONFIRMED_JOURNAL = "UnconfirmedJournal";
  private static final String CONTEXT_TYPE_ANTHOLOGY = "Anthology";
  private static final String CONTEXT_TYPE_BOOK_ANTHOLOGY = "BookAnthology";
  private static final int JANUARY = 1;
  private static final int DECEMBER = 12;
  public static final String INTERVAL = "--";

  private ResourceBibTexTransformer() {}

  public static String transform(Collection<JsonNode> hits) {
    return hits.stream()
        .map(ResourceBibTexTransformer::toEntry)
        .collect(Collectors.joining(ENTRY_SEPARATOR));
  }

  private static String toEntry(JsonNode doc) {
    var id = extractText(doc, "/id").orElse(EMPTY_STRING);
    var key = deriveKey(id);
    var nvaType = extractText(doc, INSTANCE_TYPE_POINTER).orElse(EMPTY_STRING);
    var bibType = BibtexType.toBibtexType(nvaType);

    var fields = new LinkedHashSet<BibtexField>();
    addUniversalFields(doc, id, fields);
    addTypeSpecificFields(doc, bibType, fields);

    return new BibtexEntry(bibType, key, fields).toString();
  }

  private static void addUniversalFields(JsonNode doc, String id, Collection<BibtexField> fields) {
    extractAuthors(doc).ifPresent(authors -> addField(fields, "author", authors));
    extractDoi(doc).ifPresent(doi -> addField(fields, "doi", doi));
    extractMonth(doc).ifPresent(month -> addField(fields, "month", month));
    extractText(doc, MAIN_TITLE_POINTER).ifPresent(title -> addField(fields, "title", title));
    if (!id.isBlank()) {
      addField(fields, "url", id);
    }
    extractText(doc, YEAR_POINTER).ifPresent(year -> addField(fields, "year", year));
  }

  private static void addTypeSpecificFields(
      JsonNode doc, BibtexConstants bibType, Collection<BibtexField> fields) {
    switch (bibType) {
      case ARTICLE -> {
        extractIssn(doc).ifPresent(issn -> addField(fields, "issn", issn));
        extractJournalName(doc).ifPresent(journal -> addField(fields, "journal", journal));
        extractText(doc, ISSUE_POINTER).ifPresent(issue -> addField(fields, "number", issue));
        extractPages(doc).ifPresent(pages -> addField(fields, PAGES, pages));
        extractText(doc, VOLUME_POINTER).ifPresent(volume -> addField(fields, "volume", volume));
      }
      case BOOK -> {
        extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER)
            .ifPresent(publisher -> addField(fields, "publisher", publisher));
        extractText(doc, CONTEXT_SERIES_NAME_POINTER)
            .ifPresent(series -> addField(fields, "series", series));
      }
      case INBOOK -> {
        extractAnthologyTitle(doc).ifPresent(title -> addField(fields, "booktitle", title));
        extractPages(doc).ifPresent(pages -> addField(fields, PAGES, pages));
        extractAnthologyPublisher(doc)
            .ifPresent(publisher -> addField(fields, "publisher", publisher));
      }
      case INPROCEEDINGS -> {
        extractText(doc, CONTEXT_NAME_POINTER)
            .ifPresent(title -> addField(fields, "booktitle", title));
        extractPages(doc).ifPresent(pages -> addField(fields, PAGES, pages));
      }
      case TECHREPORT -> {
        extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER)
            .ifPresent(institution -> addField(fields, "institution", institution));
      }
      case MASTERSTHESIS, PHDTHESIS -> {
        extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER)
            .ifPresent(school -> addField(fields, "school", school));
      }
      default -> {
        /* @misc, @proceedings, @unpublished: universal fields suffice */
      }
    }
  }

  private static void addField(Collection<BibtexField> fields, String name, String value) {
    if (!value.isBlank()) {
      fields.add(new BibtexField(name, value));
    }
  }

  private static Optional<String> extractJournalName(JsonNode doc) {
    var contextType = extractText(doc, CONTEXT_TYPE_POINTER).orElse(EMPTY_STRING);
    if (CONTEXT_TYPE_UNCONFIRMED_JOURNAL.equals(contextType)) {
      return extractText(doc, CONTEXT_TITLE_POINTER);
    }
    return extractText(doc, CONTEXT_NAME_POINTER);
  }

  private static boolean isAnthologyContext(JsonNode doc) {
    var contextType = extractText(doc, CONTEXT_TYPE_POINTER).orElse(EMPTY_STRING);
    return CONTEXT_TYPE_ANTHOLOGY.equals(contextType)
        || CONTEXT_TYPE_BOOK_ANTHOLOGY.equals(contextType);
  }

  private static Optional<String> extractAnthologyTitle(JsonNode doc) {
    return isAnthologyContext(doc)
        ? extractText(doc, ANTHOLOGY_MAIN_TITLE_POINTER)
        : extractText(doc, CONTEXT_NAME_POINTER);
  }

  private static Optional<String> extractAnthologyPublisher(JsonNode doc) {
    return isAnthologyContext(doc)
        ? extractText(doc, ANTHOLOGY_PUBLISHER_NAME_POINTER)
        : extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER);
  }

  private static Optional<String> extractIssn(JsonNode doc) {
    return extractText(doc, CONTEXT_ONLINE_ISSN_POINTER)
        .or(() -> extractText(doc, CONTEXT_PRINT_ISSN_POINTER));
  }

  private static Optional<String> extractDoi(JsonNode doc) {
    return extractText(doc, DOI_POINTER)
        .map(doi -> doi.replaceFirst(DOI_URI_HOST_REGEX, EMPTY_STRING).strip());
  }

  private static Optional<String> extractAuthors(JsonNode doc) {
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
                names -> names.isEmpty() ? Optional.empty() : Optional.of(names)));
  }

  private static Optional<String> extractMonth(JsonNode doc) {
    return extractText(doc, MONTH_POINTER).map(ResourceBibTexTransformer::constructMonthName);
  }

  private static @NonNull String constructMonthName(String raw) {
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

  private static Optional<String> extractPages(JsonNode doc) {
    return extractText(doc, PAGES_BEGIN_POINTER)
        .map(
            begin ->
                extractText(doc, PAGES_END_POINTER)
                    .map(end -> begin + INTERVAL + end)
                    .orElse(begin));
  }

  private static String deriveKey(String id) {
    if (id.isBlank()) {
      return "unknown";
    }
    try {
      var path = URI.create(id).getPath();
      if (path != null && path.contains("/")) {
        var lastSegment = path.substring(path.lastIndexOf('/') + JANUARY);
        if (!lastSegment.isBlank()) {
          return lastSegment;
        }
      }
    } catch (IllegalArgumentException ignored) {
      // fall through to raw id
    }
    return id;
  }

  private static Optional<String> extractText(JsonNode node, String pointer) {
    var value = node.at(pointer);
    return value.isMissingNode() || value.isNull() ? Optional.empty() : Optional.of(value.asText());
  }
}
