package no.unit.nva.search.common.bibtex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceBibTexTransformerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // -- type mapping ----------------------------------------------------------

  static Stream<Arguments> articleTypes() {
    return Stream.of(
            "AcademicArticle",
            "AcademicCommentary",
            "AcademicLiteratureReview",
            "JournalCorrigendum",
            "JournalLeader",
            "JournalLetter",
            "JournalReview",
            "MediaFeatureArticle",
            "MediaReaderOpinion",
            "PopularScienceArticle",
            "ProfessionalArticle")
        .map(t -> Arguments.of(t, "article"));
  }

  static Stream<Arguments> bookTypes() {
    return Stream.of(
            "AcademicMonograph",
            "BookAnthology",
            "Encyclopedia",
            "ExhibitionCatalog",
            "NonFictionMonograph",
            "PopularScienceMonograph",
            "Textbook")
        .map(t -> Arguments.of(t, "book"));
  }

  static Stream<Arguments> inbookTypes() {
    return Stream.of(
            "AcademicChapter",
            "ChapterInReport",
            "EncyclopediaChapter",
            "ExhibitionCatalogChapter",
            "Introduction",
            "NonFictionChapter",
            "PopularScienceChapter",
            "TextbookChapter")
        .map(t -> Arguments.of(t, "inbook"));
  }

  static Stream<Arguments> inproceedingsTypes() {
    return Stream.of(
            "ChapterConferenceAbstract",
            "ConferenceAbstract",
            "ConferenceLecture",
            "ConferencePoster")
        .map(t -> Arguments.of(t, "inproceedings"));
  }

  static Stream<Arguments> techreportTypes() {
    return Stream.of(
            "CaseReport",
            "ConferenceReport",
            "ReportBasic",
            "ReportBookOfAbstract",
            "ReportPolicy",
            "ReportResearch",
            "ReportWorkingPaper")
        .map(t -> Arguments.of(t, "techreport"));
  }

  static Stream<Arguments> mastersthesisTypes() {
    return Stream.of("DegreeBachelor", "DegreeLicentiate", "DegreeMaster")
        .map(t -> Arguments.of(t, "mastersthesis"));
  }

  static Stream<Arguments> phdthesisTypes() {
    return Stream.of("ArtisticDegreePhd", "DegreePhd").map(t -> Arguments.of(t, "phdthesis"));
  }

  @ParameterizedTest(name = "{0} → @{1}")
  @MethodSource({
    "articleTypes",
    "bookTypes",
    "inbookTypes",
    "inproceedingsTypes",
    "techreportTypes",
    "mastersthesisTypes",
    "phdthesisTypes"
  })
  void shouldMapNvaTypeToCorrectBibTexType(String nvaType, String expectedBibType) {
    var doc = docWithInstanceType(nvaType);
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@" + expectedBibType + "{"));
  }

  @Test
  void shouldFallBackToMiscForUnknownType() {
    var doc = docWithInstanceType("DataSet");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@misc{"));
  }

  // -- entry format ----------------------------------------------------------

  @Test
  void shouldProduceAlphabeticallySortedFields() {
    var doc = fullArticleDoc();
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    var authorPos = result.indexOf("  author");
    var titlePos = result.indexOf("  title");
    var yearPos = result.indexOf("  year");
    assertThat(authorPos < titlePos, is(true));
    assertThat(titlePos < yearPos, is(true));
  }

  @Test
  void shouldNotHaveTrailingCommaOnLastField() {
    var doc = fullArticleDoc();
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    // Last field line ends with "}" not "},"
    var closingBrace = result.lastIndexOf("}");
    var lastFieldEnd = result.lastIndexOf("},");
    assertThat(closingBrace > lastFieldEnd, is(true));
  }

  @Test
  void shouldSeparateMultipleEntriesWithBlankLine() {
    var doc = docWithInstanceType("AcademicArticle");
    var result = ResourceBibTexTransformer.transform(List.of(doc, doc));
    assertThat(result, containsString("}\n\n@"));
  }

  @Test
  void shouldReturnEmptyStringForEmptyHitList() {
    var result = ResourceBibTexTransformer.transform(List.of());
    assertThat(result, is(""));
  }

  // -- key derivation --------------------------------------------------------

  @Test
  void shouldDeriveKeyFromLastPathSegmentOfId() {
    var doc = doc("AcademicArticle");
    setPath(doc, "id", "https://api.nva.unit.no/publication/abc-123");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@article{abc-123,"));
  }

  @Test
  void shouldUseUnknownKeyWhenIdIsMissing() {
    var doc = docWithInstanceType("AcademicArticle");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@article{unknown,"));
  }

  // -- universal fields ------------------------------------------------------

  @Test
  void shouldExtractTitle() {
    var doc = fullArticleDoc();
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  title = {The Main Title}"));
  }

  @Test
  void shouldExtractYear() {
    var doc = fullArticleDoc();
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  year = {2023}"));
  }

  @Test
  void shouldConvertNumericMonthToAbbreviation() {
    var doc = docWithInstanceType("AcademicArticle");
    setPublicationDate(doc, "2023", "3", null);
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  month = {mar}"));
  }

  @Test
  void shouldHandleAllTwelveMonths() {
    String[] expected = {
      "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };
    for (int i = 1; i <= 12; i++) {
      var doc = docWithInstanceType("AcademicArticle");
      setPublicationDate(doc, "2023", String.valueOf(i), null);
      var result = ResourceBibTexTransformer.transform(List.of(doc));
      assertThat(result, containsString("  month = {" + expected[i - 1] + "}"));
    }
  }

  @Test
  void shouldNotIncludeMonthFieldWhenAbsent() {
    var doc = docWithInstanceType("AcademicArticle");
    setPublicationDate(doc, "2023", null, null);
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, not(containsString("month")));
  }

  @Test
  void shouldJoinMultipleAuthorsWithAnd() {
    var doc = docWithInstanceType("AcademicArticle");
    setContributors(doc, "Alice Aaberg", "Bob Bakke");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  author = {Alice Aaberg and Bob Bakke}"));
  }

  @Test
  void shouldNotIncludeAuthorFieldWhenNoContributors() {
    var doc = docWithInstanceType("AcademicArticle");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, not(containsString("author")));
  }

  @Test
  void shouldStripDoiResolverPrefix() {
    var doc = docWithInstanceType("AcademicArticle");
    setDoi(doc, "https://doi.org/10.1234/test");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  doi = {10.1234/test}"));
  }

  @Test
  void shouldStripHttpDoiResolverPrefix() {
    var doc = docWithInstanceType("AcademicArticle");
    setDoi(doc, "http://doi.org/10.1234/test");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  doi = {10.1234/test}"));
  }

  @Test
  void shouldNotIncludeDoiFieldWhenAbsent() {
    var doc = docWithInstanceType("AcademicArticle");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, not(containsString("doi")));
  }

  @Test
  void shouldIncludeUrlFromId() {
    var doc = docWithInstanceType("AcademicArticle");
    setPath(doc, "id", "https://api.nva.unit.no/publication/abc-123");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  url = {https://api.nva.unit.no/publication/abc-123}"));
  }

  // -- @article fields -------------------------------------------------------

  @Test
  void shouldExtractJournalNameFromConfirmedJournal() {
    var doc = docWithInstanceType("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  journal = {Nature}"));
  }

  @Test
  void shouldExtractJournalTitleFromUnconfirmedJournal() {
    var doc = docWithInstanceType("AcademicArticle");
    setJournalContext(doc, "UnconfirmedJournal", null, "Science Advances", null, "2375-2548");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  journal = {Science Advances}"));
  }

  @Test
  void shouldPreferOnlineIssnOverPrintIssn() {
    var doc = docWithInstanceType("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-0000", "9999-0000");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  issn = {9999-0000}"));
  }

  @Test
  void shouldFallBackToPrintIssnWhenOnlineIssnAbsent() {
    var doc = docWithInstanceType("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  issn = {1234-5678}"));
  }

  @Test
  void shouldExtractVolumeIssueAndPages() {
    var doc = docWithInstanceType("AcademicArticle");
    setInstance(doc, "AcademicArticle", "10", "3", "100", "110");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  volume = {10}"));
    assertThat(result, containsString("  number = {3}"));
    assertThat(result, containsString("  pages = {100--110}"));
  }

  @Test
  void shouldProduceSinglePageWhenOnlyBeginPresent() {
    var doc = docWithInstanceType("AcademicArticle");
    setInstance(doc, "AcademicArticle", null, null, "42", null);
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  pages = {42}"));
  }

  // -- @book fields ----------------------------------------------------------

  @Test
  void shouldExtractPublisherAndSeriesForBook() {
    var doc = docWithInstanceType("AcademicMonograph");
    setBookContext(doc, "Springer", "Lecture Notes in CS");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  publisher = {Springer}"));
    assertThat(result, containsString("  series = {Lecture Notes in CS}"));
  }

  // -- @inbook fields --------------------------------------------------------

  @Test
  void shouldExtractBooktitleAndPublisherForChapterWithFlatContext() {
    var doc = docWithInstanceType("AcademicChapter");
    setReportContext(doc, "Handbook of CS", "MIT Press");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  booktitle = {Handbook of CS}"));
    assertThat(result, containsString("  publisher = {MIT Press}"));
  }

  @Test
  void shouldExtractNestedBooktitleAndPublisherForAnthologyChapter() {
    var doc = docWithInstanceType("AcademicChapter");
    setAnthologyContext(doc, "Anthology", "Collected Essays", "Oxford UP");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@inbook{"));
    assertThat(result, containsString("  booktitle = {Collected Essays}"));
    assertThat(result, containsString("  publisher = {Oxford UP}"));
  }

  @Test
  void shouldExtractNestedBooktitleAndPublisherForBookAnthologyChapter() {
    var doc = docWithInstanceType("AcademicChapter");
    setAnthologyContext(doc, "BookAnthology", "Handbook of Things", "Springer");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  booktitle = {Handbook of Things}"));
    assertThat(result, containsString("  publisher = {Springer}"));
  }

  // -- @techreport fields ----------------------------------------------------

  @Test
  void shouldExtractInstitutionForTechReport() {
    var doc = docWithInstanceType("ReportResearch");
    setReportContext(doc, null, "SINTEF");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, containsString("  institution = {SINTEF}"));
  }

  // -- thesis fields ---------------------------------------------------------

  @Test
  void shouldExtractSchoolForMastersThesis() {
    var doc = docWithInstanceType("DegreeMaster");
    setReportContext(doc, null, "NTNU");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@mastersthesis{"));
    assertThat(result, containsString("  school = {NTNU}"));
  }

  @Test
  void shouldExtractSchoolForPhdThesis() {
    var doc = docWithInstanceType("DegreePhd");
    setReportContext(doc, null, "University of Oslo");
    var result = ResourceBibTexTransformer.transform(List.of(doc));
    assertThat(result, startsWith("@phdthesis{"));
    assertThat(result, containsString("  school = {University of Oslo}"));
  }

  // -- helpers ---------------------------------------------------------------

  private static ObjectNode doc(String instanceType) {
    var doc = MAPPER.createObjectNode();
    var entity = doc.putObject("entityDescription");
    var ref = entity.putObject("reference");
    var instance = ref.putObject("publicationInstance");
    instance.put("type", instanceType);
    ref.putObject("publicationContext").put("type", "Unknown");
    return doc;
  }

  private static ObjectNode docWithInstanceType(String instanceType) {
    return doc(instanceType);
  }

  private static ObjectNode fullArticleDoc() {
    var doc = doc("AcademicArticle");
    setPath(doc, "id", "https://api.nva.unit.no/publication/abc-123");
    var entity = (ObjectNode) doc.get("entityDescription");
    entity.put("mainTitle", "The Main Title");
    setPublicationDate(doc, "2023", "6", null);
    setContributors(doc, "Alice Aaberg");
    setDoi(doc, "https://doi.org/10.1234/test");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    return doc;
  }

  private static void setPath(ObjectNode doc, String field, String value) {
    doc.put(field, value);
  }

  private static void setPublicationDate(ObjectNode doc, String year, String month, String day) {
    var date = ((ObjectNode) doc.path("entityDescription")).putObject("publicationDate");
    if (year != null) date.put("year", year);
    if (month != null) date.put("month", month);
    if (day != null) date.put("day", day);
  }

  private static void setContributors(ObjectNode doc, String... names) {
    var contributors = ((ObjectNode) doc.path("entityDescription")).putArray("contributors");
    for (var name : names) {
      var c = contributors.addObject();
      c.putObject("identity").put("name", name);
    }
  }

  private static void setDoi(ObjectNode doc, String doi) {
    ((ObjectNode) doc.path("entityDescription").path("reference")).put("doi", doi);
  }

  private static void setJournalContext(
      ObjectNode doc, String type, String name, String title, String printIssn, String onlineIssn) {
    var ctx =
        (ObjectNode) doc.path("entityDescription").path("reference").path("publicationContext");
    ctx.put("type", type);
    if (name != null) ctx.put("name", name);
    if (title != null) ctx.put("title", title);
    if (printIssn != null) ctx.put("printIssn", printIssn);
    if (onlineIssn != null) ctx.put("onlineIssn", onlineIssn);
  }

  private static void setInstance(
      ObjectNode doc,
      String type,
      String volume,
      String issue,
      String pagesBegin,
      String pagesEnd) {
    var instance =
        (ObjectNode) doc.path("entityDescription").path("reference").path("publicationInstance");
    instance.put("type", type);
    if (volume != null) instance.put("volume", volume);
    if (issue != null) instance.put("issue", issue);
    if (pagesBegin != null || pagesEnd != null) {
      var pages = instance.putObject("pages");
      if (pagesBegin != null) pages.put("begin", pagesBegin);
      if (pagesEnd != null) pages.put("end", pagesEnd);
    }
  }

  private static void setBookContext(ObjectNode doc, String publisherName, String seriesName) {
    var ctx =
        (ObjectNode) doc.path("entityDescription").path("reference").path("publicationContext");
    ctx.put("type", "Book");
    if (publisherName != null) ctx.putObject("publisher").put("name", publisherName);
    if (seriesName != null) ctx.putObject("series").put("name", seriesName);
  }

  private static void setReportContext(ObjectNode doc, String contextName, String publisherName) {
    var ctx =
        (ObjectNode) doc.path("entityDescription").path("reference").path("publicationContext");
    ctx.put("type", "Report");
    if (contextName != null) ctx.put("name", contextName);
    if (publisherName != null) ctx.putObject("publisher").put("name", publisherName);
  }

  private static void setAnthologyContext(
      ObjectNode doc, String contextType, String bookTitle, String publisherName) {
    var ctx =
        (ObjectNode) doc.path("entityDescription").path("reference").path("publicationContext");
    ctx.put("type", contextType);
    var nested = ctx.putObject("entityDescription");
    nested.put("mainTitle", bookTitle);
    nested
        .putObject("reference")
        .putObject("publicationContext")
        .putObject("publisher")
        .put("name", publisherName);
  }
}
