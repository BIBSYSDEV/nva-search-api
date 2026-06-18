package no.unit.nva.search.common.bibliography;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
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

class SchemaOrgBibliographyTransformerTest {

  private static final ObjectMapper MAPPER = dtoObjectMapper;

  // -- envelope ----------------------------------------------------------

  @Test
  void shouldProduceSchemaOrgContext() {
    var result = transform("AcademicArticle");
    assertThat(result, containsString("\"@context\":\"https://schema.org\""));
  }

  @Test
  void shouldProduceItemListType() {
    var result = transform("AcademicArticle");
    assertThat(result, containsString("\"@type\":\"ItemList\""));
  }

  @Test
  void shouldIncludeTotalSizeAsNumberOfItems() {
    var doc = doc("AcademicArticle");
    var result = SchemaOrgBibliographyTransformer.transform(List.of(doc), 42);
    assertThat(result, containsString("\"numberOfItems\":42"));
  }

  @Test
  void shouldProduceOneItemPerHit() {
    var doc = doc("AcademicArticle");
    var result = SchemaOrgBibliographyTransformer.transform(List.of(doc, doc), 2);
    var tree = parseTree(result);
    assertThat(tree.path("itemListElement").size(), is(2));
  }

  @Test
  void shouldProduceEmptyItemListElementForNoHits() {
    var result = SchemaOrgBibliographyTransformer.transform(List.of(), 0);
    assertThat(result, containsString("\"itemListElement\":[]"));
  }

  // -- schema.org type mapping ------------------------------------------

  static Stream<Arguments> typeMapping() {
    return Stream.of(
        // ScholarlyArticle
        Arguments.of("AcademicArticle", "ScholarlyArticle"),
        Arguments.of("AcademicLiteratureReview", "ScholarlyArticle"),
        Arguments.of("CaseReport", "ScholarlyArticle"),
        Arguments.of("JournalArticle", "ScholarlyArticle"),
        Arguments.of("JournalInterview", "ScholarlyArticle"),
        Arguments.of("PopularScienceArticle", "ScholarlyArticle"),
        Arguments.of("StudyProtocol", "ScholarlyArticle"),
        // Book — note AcademicCommentary is a Book in the NVA ontology
        Arguments.of("AcademicCommentary", "Book"),
        Arguments.of("AcademicMonograph", "Book"),
        Arguments.of("BookAnthology", "Book"),
        Arguments.of("BookMonograph", "Book"),
        Arguments.of("Textbook", "Book"),
        // Chapter
        Arguments.of("AcademicChapter", "Chapter"),
        Arguments.of("ChapterArticle", "Chapter"),
        Arguments.of("ChapterInReport", "Chapter"),
        Arguments.of("TextbookChapter", "Chapter"),
        // PresentationDigitalDocument (not ScholarlyArticle)
        Arguments.of("ConferenceAbstract", "PresentationDigitalDocument"),
        Arguments.of("ConferenceLecture", "PresentationDigitalDocument"),
        Arguments.of("ConferencePoster", "PresentationDigitalDocument"),
        Arguments.of("Lecture", "PresentationDigitalDocument"),
        Arguments.of("OtherPresentation", "PresentationDigitalDocument"),
        // Report (not TechArticle)
        Arguments.of("ConferenceReport", "Report"),
        Arguments.of("ReportBasic", "Report"),
        Arguments.of("ReportResearch", "Report"),
        Arguments.of("ReportWorkingPaper", "Report"),
        // Thesis
        Arguments.of("DegreeBachelor", "Thesis"),
        Arguments.of("DegreeMaster", "Thesis"),
        Arguments.of("DegreePhd", "Thesis"),
        Arguments.of("ArtisticDegreePhd", "Thesis"),
        // Media types
        Arguments.of("MediaFeatureArticle", "NewsArticle"),
        Arguments.of("MediaInterview", "NewsArticle"),
        Arguments.of("MediaReaderOpinion", "OpinionNewsArticle"),
        Arguments.of("MediaBlogPost", "BlogPosting"),
        Arguments.of("MediaParticipationInRadioOrTv", "BroadcastEvent"),
        Arguments.of("MediaPodcast", "PodcastEpisode"),
        // Other specific types
        Arguments.of("DataSet", "Dataset"),
        Arguments.of("SoftwareSourceCode", "SoftwareSourceCode"),
        Arguments.of("MovingPicture", "Movie"),
        Arguments.of("MusicPerformance", "MusicEvent"),
        Arguments.of("PerformingArts", "Play"),
        Arguments.of("ExhibitionProduction", "ExhibitionEvent"),
        Arguments.of("VisualArts", "VisualArtwork"),
        Arguments.of("Map", "Map"),
        // CreativeWork fallback (Architecture, ArtisticDesign, LiteraryArts, etc.)
        Arguments.of("Architecture", "CreativeWork"),
        Arguments.of("ArtisticDesign", "CreativeWork"),
        Arguments.of("LiteraryArts", "CreativeWork"),
        Arguments.of("OtherArtisticOutput", "CreativeWork"),
        Arguments.of("DataManagementPlan", "CreativeWork"),
        Arguments.of("OtherStudentWork", "CreativeWork"),
        Arguments.of("UnknownFutureType", "CreativeWork"));
  }

  @ParameterizedTest(name = "{0} → {1}")
  @MethodSource("typeMapping")
  void shouldMapNvaTypeToSchemaOrgType(String nvaType, String expectedSchemaType) {
    var result = transform(nvaType);
    assertThat(result, containsString("\"@type\":\"" + expectedSchemaType + "\""));
  }

  // -- @id and url -------------------------------------------------------

  @Test
  void shouldUseHandleAsIdWhenPresent() {
    var doc = doc("AcademicArticle");
    doc.put("id", "https://api.nva.unit.no/publication/abc-123");
    doc.put("handle", "https://hdl.handle.net/11250/9999999");
    var result = transform(doc);
    assertThat(result, containsString("\"@id\":\"https://hdl.handle.net/11250/9999999\""));
    assertThat(result, containsString("\"url\":\"https://hdl.handle.net/11250/9999999\""));
  }

  @Test
  void shouldFallBackToIdWhenHandleAbsent() {
    var doc = doc("AcademicArticle");
    doc.put("id", "https://api.nva.unit.no/publication/abc-123");
    var result = transform(doc);
    assertThat(result, containsString("\"@id\":\"https://api.nva.unit.no/publication/abc-123\""));
  }

  @Test
  void shouldOmitIdAndUrlWhenBothAbsent() {
    var result = transform("AcademicArticle");
    assertThat(result, not(containsString("\"@id\"")));
    assertThat(result, not(containsString("\"url\"")));
  }

  // -- common fields -----------------------------------------------------

  @Test
  void shouldExtractTitle() {
    var doc = doc("AcademicArticle");
    entity(doc).put("mainTitle", "The Main Title");
    var result = transform(doc);
    assertThat(result, containsString("\"name\":\"The Main Title\""));
  }

  @Test
  void shouldExtractYear() {
    var doc = doc("AcademicArticle");
    entity(doc).putObject("publicationDate").put("year", "2023");
    var result = transform(doc);
    assertThat(result, containsString("\"datePublished\":\"2023\""));
  }

  @Test
  void shouldExtractAbstract() {
    var doc = doc("AcademicArticle");
    entity(doc).put("abstract", "This is the abstract.");
    var result = transform(doc);
    assertThat(result, containsString("\"abstract\":\"This is the abstract.\""));
  }

  @Test
  void shouldExtractKeywordsFromTags() {
    var doc = doc("AcademicArticle");
    entity(doc).putArray("tags").add("climate").add("arctic");
    var result = transform(doc);
    assertThat(result, containsString("\"keywords\":\"climate, arctic\""));
  }

  @Test
  void shouldExtractDoiAsIdentifier() {
    var doc = doc("AcademicArticle");
    ref(doc).put("doi", "https://doi.org/10.1234/test");
    var result = transform(doc);
    assertThat(result, containsString("\"identifier\":\"https://doi.org/10.1234/test\""));
  }

  @Test
  void shouldFallBackToTopLevelDoiWhenRefDoiAbsent() {
    var doc = doc("AcademicArticle");
    doc.put("doi", "https://doi.org/10.1234/fallback");
    var result = transform(doc);
    assertThat(result, containsString("\"identifier\":\"https://doi.org/10.1234/fallback\""));
  }

  // -- authors -----------------------------------------------------------

  @Test
  void shouldExtractSingleAuthor() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"Person\""));
    assertThat(result, containsString("\"name\":\"Alice Aaberg\""));
  }

  @Test
  void shouldExtractMultipleAuthors() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    addContributor(doc, "Bob Bakke", null, null);
    var result = transform(doc);
    assertThat(result, containsString("\"name\":\"Alice Aaberg\""));
    assertThat(result, containsString("\"name\":\"Bob Bakke\""));
  }

  @Test
  void shouldUseNvaPersonIdAsAtId() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", "https://api.nva.unit.no/cristin/person/12345", null);
    var result = transform(doc);
    assertThat(result, containsString("\"@id\":\"https://api.nva.unit.no/cristin/person/12345\""));
  }

  @Test
  void shouldIncludeOrcidAsSameAs() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, "https://orcid.org/0000-0002-1234-5678");
    var result = transform(doc);
    assertThat(result, containsString("\"sameAs\":\"https://orcid.org/0000-0002-1234-5678\""));
  }

  @Test
  void shouldIncludeBothNvaIdAndOrcid() {
    var doc = doc("AcademicArticle");
    addContributor(
        doc,
        "Alice Aaberg",
        "https://api.nva.unit.no/cristin/person/12345",
        "https://orcid.org/0000-0002-1234-5678");
    var result = transform(doc);
    assertThat(result, containsString("\"@id\":\"https://api.nva.unit.no/cristin/person/12345\""));
    assertThat(result, containsString("\"sameAs\":\"https://orcid.org/0000-0002-1234-5678\""));
  }

  @Test
  void shouldOmitPersonIdWhenNvaIdAbsent() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    var result = transform(doc);
    assertThat(result, not(containsString("\"@id\":")));
  }

  @Test
  void shouldExtractAffiliationWithEnglishLabel() {
    var doc = doc("AcademicArticle");
    var contributor = addContributorNode(doc, "Alice Aaberg", null, null);
    addAffiliation(
        contributor,
        "https://api.nva.unit.no/cristin/organization/184.16.0.0",
        "Faculty of Law",
        null);
    var result = transform(doc);
    assertThat(result, containsString("\"affiliation\""));
    assertThat(
        result,
        containsString("\"@id\":\"https://api.nva.unit.no/cristin/organization/184.16.0.0\""));
    assertThat(result, containsString("\"name\":\"Faculty of Law\""));
  }

  @Test
  void shouldFallBackToNorwegianLabelWhenEnglishAbsent() {
    var doc = doc("AcademicArticle");
    var contributor = addContributorNode(doc, "Alice Aaberg", null, null);
    addAffiliation(
        contributor,
        "https://api.nva.unit.no/cristin/organization/184.16.0.0",
        null,
        "Det juridiske fakultet");
    var result = transform(doc);
    assertThat(result, containsString("\"name\":\"Det juridiske fakultet\""));
  }

  @Test
  void shouldExtractMultipleAffiliations() {
    var doc = doc("AcademicArticle");
    var contributor = addContributorNode(doc, "Alice Aaberg", null, null);
    addAffiliation(
        contributor,
        "https://api.nva.unit.no/cristin/organization/184.16.0.0",
        "Faculty of Law",
        null);
    addAffiliation(
        contributor, "https://api.nva.unit.no/cristin/organization/185.0.0.0", "NTNU", null);
    var result = transform(doc);
    assertThat(result, containsString("\"Faculty of Law\""));
    assertThat(result, containsString("\"NTNU\""));
  }

  @Test
  void shouldOmitAffiliationWhenNone() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    var result = transform(doc);
    assertThat(result, not(containsString("\"affiliation\"")));
  }

  @Test
  void shouldOmitAuthorFieldWhenNoContributors() {
    var result = transform("AcademicArticle");
    assertThat(result, not(containsString("\"author\"")));
  }

  // -- @article / ScholarlyArticle fields --------------------------------

  @Test
  void shouldProduceFullIsPartOfChainForArticleWithVolumeAndIssue() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    setInstance(doc, "AcademicArticle", "10", "3", "100", "110");
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"PublicationIssue\""));
    assertThat(result, containsString("\"issueNumber\":\"3\""));
    assertThat(result, containsString("\"@type\":\"PublicationVolume\""));
    assertThat(result, containsString("\"volumeNumber\":\"10\""));
    assertThat(result, containsString("\"@type\":\"Periodical\""));
    assertThat(result, containsString("\"name\":\"Nature\""));
    assertThat(result, containsString("\"issn\":\"1234-5678\""));
    assertThat(result, containsString("\"pageStart\":\"100\""));
    assertThat(result, containsString("\"pageEnd\":\"110\""));
  }

  @Test
  void shouldOmitPublicationIssueWhenIssueAbsent() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    setInstance(doc, "AcademicArticle", "10", null, null, null);
    var result = transform(doc);
    assertThat(result, not(containsString("PublicationIssue")));
    assertThat(result, containsString("\"@type\":\"PublicationVolume\""));
  }

  @Test
  void shouldProducePeriodicalDirectlyWhenVolumeAndIssueAbsent() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    var result = transform(doc);
    assertThat(result, not(containsString("PublicationIssue")));
    assertThat(result, not(containsString("PublicationVolume")));
    assertThat(result, containsString("\"@type\":\"Periodical\""));
  }

  @Test
  void shouldPreferOnlineIssnOverPrintIssn() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "print-1234", "online-5678");
    var result = transform(doc);
    assertThat(result, containsString("\"issn\":\"online-5678\""));
    assertThat(result, not(containsString("print-1234")));
  }

  @Test
  void shouldUseContextTitleForUnconfirmedJournal() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "UnconfirmedJournal", null, "Science Advances", null, null);
    var result = transform(doc);
    assertThat(result, containsString("\"name\":\"Science Advances\""));
  }

  // -- @book fields ------------------------------------------------------

  @Test
  void shouldExtractPublisherForBook() {
    var doc = doc("AcademicMonograph");
    setBookContext(doc, "Springer", null);
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"Organization\""));
    assertThat(result, containsString("\"name\":\"Springer\""));
  }

  @Test
  void shouldExtractSeriesAsBookSeriesForBook() {
    var doc = doc("AcademicMonograph");
    setBookContext(doc, null, "Lecture Notes in CS");
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"BookSeries\""));
    assertThat(result, containsString("\"name\":\"Lecture Notes in CS\""));
  }

  @Test
  void shouldExtractIsbnForBook() {
    var doc = doc("AcademicMonograph");
    setContextIsbn(doc, "9781234567890");
    var result = transform(doc);
    assertThat(result, containsString("\"isbn\":\"9781234567890\""));
  }

  @Test
  void shouldExtractNumberOfPagesForBook() {
    var doc = doc("AcademicMonograph");
    setMonographPages(doc, "248");
    var result = transform(doc);
    assertThat(result, containsString("\"numberOfPages\":\"248\""));
  }

  // -- @chapter / Chapter fields ----------------------------------------

  @Test
  void shouldExtractBookIsPartOfForChapterWithFlatContext() {
    var doc = doc("AcademicChapter");
    setReportContext(doc, "Handbook of CS", "MIT Press");
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"Book\""));
    assertThat(result, containsString("\"name\":\"Handbook of CS\""));
    assertThat(result, containsString("\"name\":\"MIT Press\""));
  }

  @Test
  void shouldExtractIsbnForChapterWithFlatContext() {
    var doc = doc("AcademicChapter");
    setReportContext(doc, "Handbook of CS", null);
    setContextIsbn(doc, "9781234500001");
    var result = transform(doc);
    assertThat(result, containsString("\"isbn\":\"9781234500001\""));
  }

  @Test
  void shouldExtractPagesForChapter() {
    var doc = doc("AcademicChapter");
    setReportContext(doc, "Handbook of CS", null);
    setInstance(doc, "AcademicChapter", null, null, "42", "55");
    var result = transform(doc);
    assertThat(result, containsString("\"pageStart\":\"42\""));
    assertThat(result, containsString("\"pageEnd\":\"55\""));
  }

  @Test
  void shouldExtractNestedBookTitleForAnthologyChapter() {
    var doc = doc("AcademicChapter");
    setAnthologyContext(doc, "Anthology", "Collected Essays", "Oxford UP");
    var result = transform(doc);
    assertThat(result, containsString("\"name\":\"Collected Essays\""));
    assertThat(result, containsString("\"name\":\"Oxford UP\""));
  }

  @Test
  void shouldExtractIsbnForAnthologyChapter() {
    var doc = doc("AcademicChapter");
    setAnthologyContextWithIsbn(doc, "Anthology", "Collected Essays", null, "9781111111111");
    var result = transform(doc);
    assertThat(result, containsString("\"isbn\":\"9781111111111\""));
  }

  // -- @inproceedings / Conference ---------------------------------------

  @Test
  void shouldExtractConferenceNameAsBookIsPartOf() {
    var doc = doc("ConferenceLecture");
    setReportContext(doc, "ISWC 2024", null);
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"PresentationDigitalDocument\""));
    assertThat(result, containsString("\"@type\":\"Book\""));
    assertThat(result, containsString("\"name\":\"ISWC 2024\""));
  }

  @Test
  void shouldExtractPagesForPresentation() {
    var doc = doc("ConferenceLecture");
    setReportContext(doc, "ISWC 2024", null);
    setInstance(doc, "ConferenceLecture", null, null, "10", "20");
    var result = transform(doc);
    assertThat(result, containsString("\"pageStart\":\"10\""));
    assertThat(result, containsString("\"pageEnd\":\"20\""));
  }

  // -- @report / Report fields ------------------------------------------

  @Test
  void shouldExtractPublisherAsInstitutionForReport() {
    var doc = doc("ReportResearch");
    setReportContext(doc, null, "SINTEF");
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"Report\""));
    assertThat(result, containsString("\"name\":\"SINTEF\""));
  }

  @Test
  void shouldExtractSeriesAsPeriodicalForReport() {
    var doc = doc("ReportResearch");
    setReportContextWithSeries(doc, null, "SINTEF", "SINTEF Report");
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"Periodical\""));
    assertThat(result, containsString("\"name\":\"SINTEF Report\""));
  }

  @Test
  void shouldExtractIsbnForReport() {
    var doc = doc("ReportResearch");
    setContextIsbn(doc, "9780000000001");
    var result = transform(doc);
    assertThat(result, containsString("\"isbn\":\"9780000000001\""));
  }

  @Test
  void shouldExtractNumberOfPagesForReport() {
    var doc = doc("ReportResearch");
    setMonographPages(doc, "120");
    var result = transform(doc);
    assertThat(result, containsString("\"numberOfPages\":\"120\""));
  }

  // -- thesis fields ---------------------------------------------------

  @Test
  void shouldExtractPublisherAsSchoolForMastersThesis() {
    var doc = doc("DegreeMaster");
    setReportContext(doc, null, "NTNU");
    var result = transform(doc);
    assertThat(result, startsWith("{\"@context\":\"https://schema.org\""));
    assertThat(result, containsString("\"@type\":\"Thesis\""));
    assertThat(result, containsString("\"name\":\"NTNU\""));
  }

  @Test
  void shouldExtractPublisherAsSchoolForPhdThesis() {
    var doc = doc("DegreePhd");
    setReportContext(doc, null, "University of Oslo");
    var result = transform(doc);
    assertThat(result, containsString("\"@type\":\"Thesis\""));
    assertThat(result, containsString("\"name\":\"University of Oslo\""));
  }

  @Test
  void shouldExtractIsbnForPhdThesis() {
    var doc = doc("DegreePhd");
    setContextIsbn(doc, "9789876543211");
    var result = transform(doc);
    assertThat(result, containsString("\"isbn\":\"9789876543211\""));
  }

  @Test
  void shouldExtractNumberOfPagesForThesis() {
    var doc = doc("DegreePhd");
    setMonographPages(doc, "320");
    var result = transform(doc);
    assertThat(result, containsString("\"numberOfPages\":\"320\""));
  }

  // -- LiteraryArts / manifestation fallback ----------------------------

  @Test
  void shouldExtractIsbnFromLiteraryArtsMonographManifestation() {
    var doc = doc("LiteraryArts");
    addLiteraryArtsMonographManifestation(doc, "9780123456789", null, null);
    var result = transform(doc);
    assertThat(result, containsString("\"isbn\":\"9780123456789\""));
  }

  @Test
  void shouldExtractPublisherFromLiteraryArtsMonographManifestation() {
    var doc = doc("LiteraryArts");
    addLiteraryArtsMonographManifestation(doc, null, "Cappelen Damm", null);
    var result = transform(doc);
    assertThat(result, containsString("\"name\":\"Cappelen Damm\""));
  }

  @Test
  void shouldExtractNumberOfPagesFromLiteraryArtsMonographManifestation() {
    var doc = doc("LiteraryArts");
    addLiteraryArtsMonographManifestation(doc, null, null, "212");
    var result = transform(doc);
    assertThat(result, containsString("\"numberOfPages\":\"212\""));
  }

  // -- contributor role mapping -----------------------------------------

  @Test
  void shouldMapCreatorToAuthorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Alice Aaberg", null, null, "Creator");
    var result = transform(doc);
    assertThat(result, containsString("\"author\""));
    assertThat(result, not(containsString("\"contributor\"")));
  }

  @Test
  void shouldMapEditorToEditorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Bob Bakke", null, null, "Editor");
    var result = transform(doc);
    assertThat(result, containsString("\"editor\""));
    assertThat(result, not(containsString("\"author\"")));
  }

  @Test
  void shouldMapTranslatorAdapterToTranslatorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Carol Christoffersen", null, null, "TranslatorAdapter");
    var result = transform(doc);
    assertThat(result, containsString("\"translator\""));
    assertThat(result, not(containsString("\"author\"")));
  }

  @Test
  void shouldMapWriterToAuthorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "David Dahl", null, null, "Writer");
    var result = transform(doc);
    assertThat(result, containsString("\"author\""));
  }

  @Test
  void shouldMapIllustratorToIllustratorProperty() {
    var doc = doc("AcademicMonograph");
    addContributorNodeWithRole(doc, "Eve Eriksen", null, null, "Illustrator");
    var result = transform(doc);
    assertThat(result, containsString("\"illustrator\""));
  }

  @Test
  void shouldMapProducerToProducerProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Finn Frydenlund", null, null, "Producer");
    var result = transform(doc);
    assertThat(result, containsString("\"producer\""));
  }

  @Test
  void shouldMapDirectorToDirectorProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Gerd Grøndahl", null, null, "Director");
    var result = transform(doc);
    assertThat(result, containsString("\"director\""));
  }

  @Test
  void shouldMapArtisticDirectorToDirectorProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Hans Hansen", null, null, "ArtisticDirector");
    var result = transform(doc);
    assertThat(result, containsString("\"director\""));
  }

  @Test
  void shouldMapActorToActorProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Ida Iversen", null, null, "Actor");
    var result = transform(doc);
    assertThat(result, containsString("\"actor\""));
  }

  @Test
  void shouldMapComposerToComposerProperty() {
    var doc = doc("MusicPerformance");
    addContributorNodeWithRole(doc, "Jan Jensen", null, null, "Composer");
    var result = transform(doc);
    assertThat(result, containsString("\"composer\""));
  }

  @Test
  void shouldMapSupervisorToContributorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Kari Knutsen", null, null, "Supervisor");
    var result = transform(doc);
    assertThat(result, containsString("\"contributor\""));
    assertThat(result, not(containsString("\"author\"")));
  }

  @Test
  void shouldMapRoleOtherToContributorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Lars Larsen", null, null, "RoleOther");
    var result = transform(doc);
    assertThat(result, containsString("\"contributor\""));
  }

  @Test
  void shouldMapUnknownRoleToContributorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Mona Moe", null, null, "SomeFutureRole");
    var result = transform(doc);
    assertThat(result, containsString("\"contributor\""));
  }

  @Test
  void shouldGroupContributorsByRole() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Alice Aaberg", null, null, "Creator");
    addContributorNodeWithRole(doc, "Bob Bakke", null, null, "Editor");
    var result = transform(doc);
    assertThat(result, containsString("\"author\""));
    assertThat(result, containsString("\"editor\""));
  }

  // -- helpers ----------------------------------------------------------

  private static String transform(String nvaType) {
    return transform(doc(nvaType));
  }

  private static String transform(ObjectNode doc) {
    return SchemaOrgBibliographyTransformer.transform(List.of(doc), 1);
  }

  private static com.fasterxml.jackson.databind.JsonNode parseTree(String json) {
    try {
      return MAPPER.readTree(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ObjectNode doc(String instanceType) {
    var doc = MAPPER.createObjectNode();
    var entity = doc.putObject("entityDescription");
    var ref = entity.putObject("reference");
    ref.putObject("publicationInstance").put("type", instanceType);
    ref.putObject("publicationContext").put("type", "Unknown");
    return doc;
  }

  private static ObjectNode entity(ObjectNode doc) {
    return (ObjectNode) doc.path("entityDescription");
  }

  private static ObjectNode ref(ObjectNode doc) {
    return (ObjectNode) doc.path("entityDescription").path("reference");
  }

  private static void addContributor(ObjectNode doc, String name, String nvaId, String orcId) {
    addContributorNode(doc, name, nvaId, orcId);
  }

  private static ObjectNode addContributorNode(
      ObjectNode doc, String name, String nvaId, String orcId) {
    return addContributorNodeWithRole(doc, name, nvaId, orcId, "Creator");
  }

  private static ObjectNode addContributorNodeWithRole(
      ObjectNode doc, String name, String nvaId, String orcId, String role) {
    var contributors = entity(doc).withArray("contributors");
    var contributor = contributors.addObject();
    var identity = contributor.putObject("identity");
    identity.put("name", name);
    if (nonNull(nvaId)) identity.put("id", nvaId);
    if (nonNull(orcId)) identity.put("orcId", orcId);
    contributor.putObject("role").put("type", role);
    return contributor;
  }

  private static void addAffiliation(
      ObjectNode contributor, String orgId, String labelEn, String labelNb) {
    var affiliation = contributor.withArray("affiliations").addObject();
    if (nonNull(orgId)) affiliation.put("id", orgId);
    if (nonNull(labelEn) || nonNull(labelNb)) {
      var labels = affiliation.putObject("labels");
      if (nonNull(labelEn)) labels.put("en", labelEn);
      if (nonNull(labelNb)) labels.put("nb", labelNb);
    }
  }

  private static void setJournalContext(
      ObjectNode doc, String type, String name, String title, String printIssn, String onlineIssn) {
    var ctx = (ObjectNode) ref(doc).path("publicationContext");
    ctx.put("type", type);
    if (nonNull(name)) ctx.put("name", name);
    if (nonNull(title)) ctx.put("title", title);
    if (nonNull(printIssn)) ctx.put("printIssn", printIssn);
    if (nonNull(onlineIssn)) ctx.put("onlineIssn", onlineIssn);
  }

  private static void setInstance(
      ObjectNode doc, String type, String volume, String issue, String begin, String end) {
    var instance = (ObjectNode) ref(doc).path("publicationInstance");
    instance.put("type", type);
    if (nonNull(volume)) instance.put("volume", volume);
    if (nonNull(issue)) instance.put("issue", issue);
    if (nonNull(begin) || nonNull(end)) {
      var pages = instance.putObject("pages");
      if (nonNull(begin)) pages.put("begin", begin);
      if (nonNull(end)) pages.put("end", end);
    }
  }

  private static void setBookContext(ObjectNode doc, String publisherName, String seriesName) {
    var ctx = (ObjectNode) ref(doc).path("publicationContext");
    ctx.put("type", "Book");
    if (nonNull(publisherName)) ctx.putObject("publisher").put("name", publisherName);
    if (nonNull(seriesName)) ctx.putObject("series").put("name", seriesName);
  }

  private static void setReportContext(ObjectNode doc, String contextName, String publisherName) {
    var ctx = (ObjectNode) ref(doc).path("publicationContext");
    ctx.put("type", "Report");
    if (nonNull(contextName)) ctx.put("name", contextName);
    if (nonNull(publisherName)) ctx.putObject("publisher").put("name", publisherName);
  }

  private static void setReportContextWithSeries(
      ObjectNode doc, String contextName, String publisherName, String seriesName) {
    setReportContext(doc, contextName, publisherName);
    var ctx = (ObjectNode) ref(doc).path("publicationContext");
    if (nonNull(seriesName)) ctx.putObject("series").put("name", seriesName);
  }

  private static void setAnthologyContext(
      ObjectNode doc, String contextType, String bookTitle, String publisherName) {
    var ctx = (ObjectNode) ref(doc).path("publicationContext");
    ctx.put("type", contextType);
    var nested = ctx.putObject("entityDescription");
    nested.put("mainTitle", bookTitle);
    nested
        .putObject("reference")
        .putObject("publicationContext")
        .putObject("publisher")
        .put("name", publisherName);
  }

  private static void setMonographPages(ObjectNode doc, String pageCount) {
    var instance = (ObjectNode) ref(doc).path("publicationInstance");
    instance.putObject("pages").put("pages", pageCount);
  }

  private static void setContextIsbn(ObjectNode doc, String isbn) {
    ((ObjectNode) ref(doc).path("publicationContext")).putArray("isbnList").add(isbn);
  }

  private static void setAnthologyContextWithIsbn(
      ObjectNode doc, String contextType, String bookTitle, String publisherName, String isbn) {
    setAnthologyContext(doc, contextType, bookTitle, nonNull(publisherName) ? publisherName : "");
    var nested =
        (ObjectNode)
            ref(doc)
                .path("publicationContext")
                .path("entityDescription")
                .path("reference")
                .path("publicationContext");
    nested.putArray("isbnList").add(isbn);
  }

  private static void addLiteraryArtsMonographManifestation(
      ObjectNode doc, String isbn, String publisherName, String pages) {
    var instance = (ObjectNode) ref(doc).path("publicationInstance");
    var manifestation = instance.withArray("manifestations").addObject();
    manifestation.put("type", "LiteraryArtsMonograph");
    if (nonNull(isbn)) manifestation.putArray("isbnList").add(isbn);
    if (nonNull(publisherName)) manifestation.putObject("publisher").put("name", publisherName);
    if (nonNull(pages)) manifestation.putObject("pages").put("pages", pages);
  }
}
