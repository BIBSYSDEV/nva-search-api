package no.unit.nva.search.common.bibtex;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ResourceBibTexTransformer {

  // Publication context types
  private static final String JOURNAL = "Journal";
  private static final String UNCONFIRMED_JOURNAL = "UnconfirmedJournal";
  private static final String ANTHOLOGY = "Anthology";
  private static final String BOOK_ANTHOLOGY = "BookAnthology";

  // JSON pointers — publication-level
  private static final String ID_POINTER = "/id";
  private static final String MAIN_TITLE_POINTER = "/entityDescription/mainTitle";
  private static final String YEAR_POINTER = "/entityDescription/publicationDate/year";
  private static final String MONTH_POINTER = "/entityDescription/publicationDate/month";
  private static final String CONTRIBUTORS_POINTER = "/entityDescription/contributors";
  private static final String IDENTITY_NAME_POINTER = "/identity/name";
  private static final String DOI_POINTER = "/entityDescription/reference/doi";

  // JSON pointers — publication context
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

  // JSON pointers — anthology (chapter) context: book title and publisher are nested
  private static final String ANTHOLOGY_MAIN_TITLE_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/mainTitle";
  private static final String ANTHOLOGY_PUBLISHER_NAME_POINTER =
      "/entityDescription/reference/publicationContext/entityDescription/reference"
          + "/publicationContext/publisher/name";

  // JSON pointers — publication instance
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
  private static final String[] MONTH_ABBR = {
    "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec"
  };

  private static final Map<String, String> TYPE_MAP =
      Map.ofEntries(
          // Journal publications → @article
          Map.entry("AcademicArticle", "article"),
          Map.entry("AcademicCommentary", "article"),
          Map.entry("AcademicLiteratureReview", "article"),
          Map.entry("CaseReport", "article"),
          Map.entry("JournalCorrigendum", "article"),
          Map.entry("JournalLeader", "article"),
          Map.entry("JournalLetter", "article"),
          Map.entry("JournalReview", "article"),
          Map.entry("MediaFeatureArticle", "article"),
          Map.entry("MediaReaderOpinion", "article"),
          Map.entry("PopularScienceArticle", "article"),
          Map.entry("ProfessionalArticle", "article"),
          Map.entry("StudyProtocol", "article"),
          // Monographs → @book
          Map.entry("AcademicMonograph", "book"),
          Map.entry("BookAnthology", "book"),
          Map.entry("Encyclopedia", "book"),
          Map.entry("ExhibitionCatalog", "book"),
          Map.entry("NonFictionMonograph", "book"),
          Map.entry("PopularScienceMonograph", "book"),
          Map.entry("Textbook", "book"),
          // Chapters in books → @incollection
          Map.entry("AcademicChapter", "incollection"),
          Map.entry("ChapterConferenceAbstract", "incollection"),
          Map.entry("EncyclopediaChapter", "incollection"),
          Map.entry("ExhibitionCatalogChapter", "incollection"),
          Map.entry("Introduction", "incollection"),
          Map.entry("NonFictionChapter", "incollection"),
          Map.entry("PopularScienceChapter", "incollection"),
          Map.entry("TextbookChapter", "incollection"),
          // Chapters in reports → @inbook
          Map.entry("ChapterInReport", "inbook"),
          // Conference → @inproceedings
          Map.entry("ConferenceAbstract", "inproceedings"),
          Map.entry("ConferenceLecture", "inproceedings"),
          Map.entry("ConferencePoster", "inproceedings"),
          // Conference proceedings / journal issue → @proceedings
          Map.entry("ConferenceReport", "proceedings"),
          Map.entry("JournalIssue", "proceedings"),
          // Reports → @techreport
          Map.entry("ReportBasic", "techreport"),
          Map.entry("ReportBookOfAbstract", "techreport"),
          Map.entry("ReportPolicy", "techreport"),
          Map.entry("ReportResearch", "techreport"),
          Map.entry("ReportWorkingPaper", "techreport"),
          // Theses
          Map.entry("DegreeBachelor", "mastersthesis"),
          Map.entry("DegreeLicentiate", "mastersthesis"),
          Map.entry("DegreeMaster", "mastersthesis"),
          Map.entry("ArtisticDegreePhd", "phdthesis"),
          Map.entry("DegreePhd", "phdthesis"),
          // Other student work → @unpublished
          Map.entry("OtherStudentWork", "unpublished"));
  // Everything else (artistic, media, data, software) → @misc (default)

  private ResourceBibTexTransformer() {}

  public static String transform(List<JsonNode> hits) {
    return hits.stream()
        .map(ResourceBibTexTransformer::toEntry)
        .collect(Collectors.joining(ENTRY_SEPARATOR));
  }

  private static String toEntry(JsonNode doc) {
    var id = extractText(doc, ID_POINTER, "");
    var key = deriveKey(id);
    var nvaType = extractText(doc, INSTANCE_TYPE_POINTER, "");
    var bibType = TYPE_MAP.getOrDefault(nvaType, "misc");

    var fields = new TreeMap<String, String>();
    addUniversalFields(doc, id, fields);
    addTypeSpecificFields(doc, bibType, fields);

    var fieldsString =
        fields.entrySet().stream()
            .map(e -> "  " + e.getKey() + " = {" + e.getValue() + "}")
            .collect(Collectors.joining(",\n"));

    return "@" + bibType + "{" + key + ",\n" + fieldsString + "\n}";
  }

  private static void addUniversalFields(JsonNode doc, String id, Map<String, String> fields) {
    putIfPresent(fields, "author", extractAuthors(doc));
    putIfPresent(fields, "doi", extractDoi(doc));
    putIfPresent(fields, "month", extractMonth(doc));
    putIfPresent(fields, "title", extractText(doc, MAIN_TITLE_POINTER, null));
    putIfPresent(fields, "url", id.isBlank() ? null : id);
    putIfPresent(fields, "year", extractText(doc, YEAR_POINTER, null));
  }

  private static void addTypeSpecificFields(
      JsonNode doc, String bibType, Map<String, String> fields) {
    switch (bibType) {
      case "article" -> {
        putIfPresent(fields, "issn", extractIssn(doc));
        putIfPresent(fields, "journal", extractJournalName(doc));
        putIfPresent(fields, "number", extractText(doc, ISSUE_POINTER, null));
        putIfPresent(fields, "pages", extractPages(doc));
        putIfPresent(fields, "volume", extractText(doc, VOLUME_POINTER, null));
      }
      case "book" -> {
        putIfPresent(fields, "publisher", extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER, null));
        putIfPresent(fields, "series", extractText(doc, CONTEXT_SERIES_NAME_POINTER, null));
      }
      case "incollection" -> {
        putIfPresent(fields, "booktitle", extractAnthologyTitle(doc));
        putIfPresent(fields, "pages", extractPages(doc));
        putIfPresent(fields, "publisher", extractAnthologyPublisher(doc));
      }
      case "inbook" -> {
        putIfPresent(fields, "booktitle", extractText(doc, CONTEXT_NAME_POINTER, null));
        putIfPresent(fields, "pages", extractPages(doc));
        putIfPresent(fields, "publisher", extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER, null));
      }
      case "inproceedings" -> {
        putIfPresent(fields, "booktitle", extractText(doc, CONTEXT_NAME_POINTER, null));
        putIfPresent(fields, "pages", extractPages(doc));
      }
      case "techreport" -> {
        putIfPresent(fields, "institution", extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER, null));
      }
      case "mastersthesis", "phdthesis" -> {
        putIfPresent(fields, "school", extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER, null));
      }
      default -> { /* @misc, @proceedings, @unpublished: universal fields suffice */ }
    }
  }

  private static String extractJournalName(JsonNode doc) {
    var contextType = extractText(doc, CONTEXT_TYPE_POINTER, "");
    if (UNCONFIRMED_JOURNAL.equals(contextType)) {
      return extractText(doc, CONTEXT_TITLE_POINTER, null);
    }
    return extractText(doc, CONTEXT_NAME_POINTER, null);
  }

  private static String extractAnthologyTitle(JsonNode doc) {
    var contextType = extractText(doc, CONTEXT_TYPE_POINTER, "");
    if (ANTHOLOGY.equals(contextType) || BOOK_ANTHOLOGY.equals(contextType)) {
      return extractText(doc, ANTHOLOGY_MAIN_TITLE_POINTER, null);
    }
    return extractText(doc, CONTEXT_NAME_POINTER, null);
  }

  private static String extractAnthologyPublisher(JsonNode doc) {
    var contextType = extractText(doc, CONTEXT_TYPE_POINTER, "");
    if (ANTHOLOGY.equals(contextType) || BOOK_ANTHOLOGY.equals(contextType)) {
      return extractText(doc, ANTHOLOGY_PUBLISHER_NAME_POINTER, null);
    }
    return extractText(doc, CONTEXT_PUBLISHER_NAME_POINTER, null);
  }

  private static String extractIssn(JsonNode doc) {
    var online = extractText(doc, CONTEXT_ONLINE_ISSN_POINTER, null);
    return online != null ? online : extractText(doc, CONTEXT_PRINT_ISSN_POINTER, null);
  }

  private static String extractDoi(JsonNode doc) {
    var doi = extractText(doc, DOI_POINTER, null);
    if (doi == null) {
      return null;
    }
    return doi.replaceFirst("(?i)https?://doi\\.org/", "").strip();
  }

  private static String extractAuthors(JsonNode doc) {
    var contributors = doc.at(CONTRIBUTORS_POINTER);
    if (contributors.isMissingNode() || !contributors.isArray()) {
      return null;
    }
    var names =
        StreamSupport.stream(contributors.spliterator(), false)
            .map(c -> extractText(c, IDENTITY_NAME_POINTER, null))
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.joining(AND));
    return names.isBlank() ? null : names;
  }

  private static String extractMonth(JsonNode doc) {
    var raw = extractText(doc, MONTH_POINTER, null);
    if (raw == null) {
      return null;
    }
    try {
      int month = Integer.parseInt(raw.strip());
      if (month >= 1 && month <= 12) {
        return MONTH_ABBR[month - 1];
      }
    } catch (NumberFormatException ignored) {
      // already an abbreviation or other string
    }
    return raw;
  }

  private static String extractPages(JsonNode doc) {
    var begin = extractText(doc, PAGES_BEGIN_POINTER, null);
    if (begin == null) {
      return null;
    }
    var end = extractText(doc, PAGES_END_POINTER, null);
    return end != null ? begin + "--" + end : begin;
  }

  private static String deriveKey(String id) {
    if (id.isBlank()) {
      return "unknown";
    }
    var lastSlash = id.lastIndexOf('/');
    return lastSlash >= 0 && lastSlash < id.length() - 1 ? id.substring(lastSlash + 1) : id;
  }

  private static void putIfPresent(Map<String, String> fields, String name, String value) {
    if (value != null && !value.isBlank()) {
      fields.put(name, value);
    }
  }

  private static String extractText(JsonNode node, String pointer, String defaultValue) {
    var value = node.at(pointer);
    return value.isMissingNode() || value.isNull() ? defaultValue : value.asText();
  }

  public static List<String> getJsonFields() {
    return List.of(
        "id",
        "entityDescription.mainTitle",
        "entityDescription.publicationDate.year",
        "entityDescription.publicationDate.month",
        "entityDescription.contributors.identity.name",
        "entityDescription.reference.doi",
        "entityDescription.reference.publicationInstance.type",
        "entityDescription.reference.publicationInstance.volume",
        "entityDescription.reference.publicationInstance.issue",
        "entityDescription.reference.publicationInstance.pages.begin",
        "entityDescription.reference.publicationInstance.pages.end",
        "entityDescription.reference.publicationContext.type",
        "entityDescription.reference.publicationContext.name",
        "entityDescription.reference.publicationContext.title",
        "entityDescription.reference.publicationContext.onlineIssn",
        "entityDescription.reference.publicationContext.printIssn",
        "entityDescription.reference.publicationContext.publisher.name",
        "entityDescription.reference.publicationContext.series.name",
        "entityDescription.reference.publicationContext.entityDescription.mainTitle",
        "entityDescription.reference.publicationContext.entityDescription.reference"
            + ".publicationContext.publisher.name");
  }
}
