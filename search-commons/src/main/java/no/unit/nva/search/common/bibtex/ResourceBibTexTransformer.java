package no.unit.nva.search.common.bibtex;

import static no.unit.nva.search.common.bibtex.BibtexConstants.ARTICLE;
import static no.unit.nva.search.common.bibtex.BibtexConstants.BOOK;
import static no.unit.nva.search.common.bibtex.BibtexConstants.INBOOK;
import static no.unit.nva.search.common.bibtex.BibtexConstants.INPROCEEDINGS;
import static no.unit.nva.search.common.bibtex.BibtexConstants.MASTERSTHESIS;
import static no.unit.nva.search.common.bibtex.BibtexConstants.PHDTHESIS;
import static no.unit.nva.search.common.bibtex.BibtexConstants.TECHREPORT;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.ABSTRACT_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.CONTEXT_ISBN_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.CONTEXT_NAME_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.CONTEXT_PUBLISHER_NAME_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.CONTEXT_SERIES_NAME_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.INSTANCE_TYPE_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.ISSUE_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.MAIN_TITLE_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.VOLUME_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.YEAR_POINTER;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.anthologyIsbn;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.anthologyPublisher;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.anthologyTitle;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.authors;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.doi;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.extractText;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.isbn;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.isbnOrManifestationIsbn;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.issn;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.journalName;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.keywords;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.month;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.nvaTypeNote;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.pages;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.pagesOrManifestationPages;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.publisherOrManifestationPublisher;
import static no.unit.nva.search.common.bibtex.BibtexFieldExtractors.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ResourceBibTexTransformer {

  private static final String HANDLE_POINTER = "/handle";
  private static final String ID_POINTER = "/id";
  private static final String ENTRY_SEPARATOR = "\n\n";

  private static final List<BibtexFieldExtractor> UNIVERSAL_EXTRACTORS =
      List.of(
          text("abstract", ABSTRACT_POINTER),
          authors(),
          doi(),
          keywords(),
          month(),
          nvaTypeNote(),
          text("title", MAIN_TITLE_POINTER),
          text("year", YEAR_POINTER));

  private static final Map<BibtexConstants, List<BibtexFieldExtractor>> TYPE_EXTRACTORS =
      Map.ofEntries(
          Map.entry(
              ARTICLE,
              List.of(
                  issn(),
                  journalName(),
                  text("number", ISSUE_POINTER),
                  pages(),
                  text("volume", VOLUME_POINTER))),
          Map.entry(
              BOOK,
              List.of(
                  isbnOrManifestationIsbn(),
                  pagesOrManifestationPages(),
                  publisherOrManifestationPublisher(),
                  text("series", CONTEXT_SERIES_NAME_POINTER))),
          Map.entry(
              INBOOK, List.of(anthologyTitle(), anthologyIsbn(), pages(), anthologyPublisher())),
          Map.entry(INPROCEEDINGS, List.of(text("booktitle", CONTEXT_NAME_POINTER), pages())),
          Map.entry(
              TECHREPORT,
              List.of(
                  isbn(CONTEXT_ISBN_POINTER),
                  text("institution", CONTEXT_PUBLISHER_NAME_POINTER),
                  pages(),
                  text("series", CONTEXT_SERIES_NAME_POINTER))),
          Map.entry(
              MASTERSTHESIS, List.of(pages(), text("school", CONTEXT_PUBLISHER_NAME_POINTER))),
          Map.entry(
              PHDTHESIS,
              List.of(
                  isbn(CONTEXT_ISBN_POINTER),
                  pages(),
                  text("school", CONTEXT_PUBLISHER_NAME_POINTER))));

  private ResourceBibTexTransformer() {}

  public static String transform(Collection<JsonNode> hits) {
    return hits.stream()
        .map(ResourceBibTexTransformer::toEntry)
        .collect(Collectors.joining(ENTRY_SEPARATOR));
  }

  private static String toEntry(JsonNode doc) {
    var id = extractText(doc, ID_POINTER).orElse("");
    var url = extractText(doc, HANDLE_POINTER).orElse(id);
    var key = deriveKey(url);
    var nvaType = extractText(doc, INSTANCE_TYPE_POINTER).orElse("");
    var bibType = BibtexType.toBibtexType(nvaType);

    var fields = new LinkedHashSet<BibtexField>();
    if (!url.isBlank()) {
      fields.add(new BibtexField("url", url));
    }
    Stream.concat(
            UNIVERSAL_EXTRACTORS.stream(),
            TYPE_EXTRACTORS.getOrDefault(bibType, List.of()).stream())
        .flatMap(e -> e.extract(doc).stream())
        .forEach(fields::add);

    return new BibtexEntry(bibType, key, fields).toString();
  }

  private static String deriveKey(String id) {
    if (id.isBlank()) {
      return "unknown";
    }
    try {
      var path = URI.create(id).getPath();
      if (path != null && path.contains("/")) {
        var lastSegment = path.substring(path.lastIndexOf('/') + 1);
        if (!lastSegment.isBlank()) {
          return sanitizeKey(lastSegment);
        }
      }
    } catch (IllegalArgumentException ignored) {
      // fall through to raw id
    }
    return sanitizeKey(id);
  }

  private static String sanitizeKey(String raw) {
    return raw.replaceAll("[{}\\\\%#]", "");
  }
}
