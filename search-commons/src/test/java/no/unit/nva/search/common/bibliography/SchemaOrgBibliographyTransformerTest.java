package no.unit.nva.search.common.bibliography;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
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
    assertThat(transform("AcademicArticle").path("@context").asText(), is("https://schema.org"));
  }

  @Test
  void shouldProduceItemListType() {
    assertThat(transform("AcademicArticle").path("@type").asText(), is("ItemList"));
  }

  @Test
  void shouldIncludeTotalSizeAsNumberOfItems() {
    var tree = parse(SchemaOrgBibliographyTransformer.transform(List.of(doc("AcademicArticle")), 42));
    assertThat(tree.path("numberOfItems").asInt(), is(42));
  }

  @Test
  void shouldProduceOneItemPerHit() {
    var doc = doc("AcademicArticle");
    var tree = parse(SchemaOrgBibliographyTransformer.transform(List.of(doc, doc), 2));
    assertThat(tree.path("itemListElement").size(), is(2));
  }

  @Test
  void shouldProduceEmptyItemListElementForNoHits() {
    var tree = parse(SchemaOrgBibliographyTransformer.transform(List.of(), 0));
    assertThat(tree.path("itemListElement").isEmpty(), is(true));
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
    assertThat(item(transform(nvaType)).path("@type").asText(), is(expectedSchemaType));
  }

  // -- @id and url -------------------------------------------------------

  @Test
  void shouldUseHandleAsIdWhenPresent() {
    var doc = doc("AcademicArticle");
    doc.put("id", "https://api.nva.unit.no/publication/abc-123");
    doc.put("handle", "https://hdl.handle.net/11250/9999999");
    var item = item(transform(doc));
    assertThat(item.path("@id").asText(), is("https://hdl.handle.net/11250/9999999"));
    assertThat(item.path("url").asText(), is("https://hdl.handle.net/11250/9999999"));
  }

  @Test
  void shouldFallBackToIdWhenHandleAbsent() {
    var doc = doc("AcademicArticle");
    doc.put("id", "https://api.nva.unit.no/publication/abc-123");
    assertThat(
        item(transform(doc)).path("@id").asText(),
        is("https://api.nva.unit.no/publication/abc-123"));
  }

  @Test
  void shouldOmitIdAndUrlWhenBothAbsent() {
    var item = item(transform("AcademicArticle"));
    assertThat(item.has("@id"), is(false));
    assertThat(item.has("url"), is(false));
  }

  // -- common fields -----------------------------------------------------

  @Test
  void shouldExtractTitle() {
    var doc = doc("AcademicArticle");
    entity(doc).put("mainTitle", "The Main Title");
    assertThat(item(transform(doc)).path("name").asText(), is("The Main Title"));
  }

  @Test
  void shouldExtractYear() {
    var doc = doc("AcademicArticle");
    entity(doc).putObject("publicationDate").put("year", "2023");
    assertThat(item(transform(doc)).path("datePublished").asText(), is("2023"));
  }

  @Test
  void shouldExtractAbstract() {
    var doc = doc("AcademicArticle");
    entity(doc).put("abstract", "This is the abstract.");
    assertThat(item(transform(doc)).path("abstract").asText(), is("This is the abstract."));
  }

  @Test
  void shouldExtractKeywordsFromTags() {
    var doc = doc("AcademicArticle");
    entity(doc).putArray("tags").add("climate").add("arctic");
    assertThat(item(transform(doc)).path("keywords").asText(), is("climate, arctic"));
  }

  @Test
  void shouldExtractDoiAsIdentifier() {
    var doc = doc("AcademicArticle");
    ref(doc).put("doi", "https://doi.org/10.1234/test");
    assertThat(item(transform(doc)).path("identifier").asText(), is("https://doi.org/10.1234/test"));
  }

  @Test
  void shouldFallBackToTopLevelDoiWhenRefDoiAbsent() {
    var doc = doc("AcademicArticle");
    doc.put("doi", "https://doi.org/10.1234/fallback");
    assertThat(
        item(transform(doc)).path("identifier").asText(),
        is("https://doi.org/10.1234/fallback"));
  }

  // -- authors -----------------------------------------------------------

  @Test
  void shouldExtractSingleAuthor() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    var person = item(transform(doc)).path("author").get(0);
    assertThat(person.path("@type").asText(), is("Person"));
    assertThat(person.path("name").asText(), is("Alice Aaberg"));
  }

  @Test
  void shouldExtractMultipleAuthors() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    addContributor(doc, "Bob Bakke", null, null);
    var authors = item(transform(doc)).path("author");
    assertThat(authors.get(0).path("name").asText(), is("Alice Aaberg"));
    assertThat(authors.get(1).path("name").asText(), is("Bob Bakke"));
  }

  @Test
  void shouldUseNvaPersonIdAsAtId() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", "https://api.nva.unit.no/cristin/person/12345", null);
    assertThat(
        item(transform(doc)).path("author").get(0).path("@id").asText(),
        is("https://api.nva.unit.no/cristin/person/12345"));
  }

  @Test
  void shouldIncludeOrcidAsSameAs() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, "https://orcid.org/0000-0002-1234-5678");
    assertThat(
        item(transform(doc)).path("author").get(0).path("sameAs").asText(),
        is("https://orcid.org/0000-0002-1234-5678"));
  }

  @Test
  void shouldIncludeBothNvaIdAndOrcid() {
    var doc = doc("AcademicArticle");
    addContributor(
        doc,
        "Alice Aaberg",
        "https://api.nva.unit.no/cristin/person/12345",
        "https://orcid.org/0000-0002-1234-5678");
    var person = item(transform(doc)).path("author").get(0);
    assertThat(person.path("@id").asText(), is("https://api.nva.unit.no/cristin/person/12345"));
    assertThat(person.path("sameAs").asText(), is("https://orcid.org/0000-0002-1234-5678"));
  }

  @Test
  void shouldOmitPersonIdWhenNvaIdAbsent() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    assertThat(item(transform(doc)).path("author").get(0).has("@id"), is(false));
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
    var affiliation = item(transform(doc)).path("author").get(0).path("affiliation").get(0);
    assertThat(affiliation.has("affiliation"), is(false));
    assertThat(
        affiliation.path("@id").asText(),
        is("https://api.nva.unit.no/cristin/organization/184.16.0.0"));
    assertThat(affiliation.path("name").asText(), is("Faculty of Law"));
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
    var affiliation = item(transform(doc)).path("author").get(0).path("affiliation").get(0);
    assertThat(affiliation.path("name").asText(), is("Det juridiske fakultet"));
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
    var affiliations = item(transform(doc)).path("author").get(0).path("affiliation");
    assertThat(affiliations.size(), is(2));
    assertThat(affiliations.get(0).path("name").asText(), is("Faculty of Law"));
    assertThat(affiliations.get(1).path("name").asText(), is("NTNU"));
  }

  @Test
  void shouldOmitAffiliationWhenNone() {
    var doc = doc("AcademicArticle");
    addContributor(doc, "Alice Aaberg", null, null);
    assertThat(item(transform(doc)).path("author").get(0).has("affiliation"), is(false));
  }

  @Test
  void shouldOmitAuthorFieldWhenNoContributors() {
    assertThat(item(transform("AcademicArticle")).has("author"), is(false));
  }

  // -- @article / ScholarlyArticle fields --------------------------------

  @Test
  void shouldProduceFullIsPartOfChainForArticleWithVolumeAndIssue() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    setInstance(doc, "AcademicArticle", "10", "3", "100", "110");
    var item = item(transform(doc));
    var issue = item.path("isPartOf");
    var volume = issue.path("isPartOf");
    var periodical = volume.path("isPartOf");
    assertThat(issue.path("@type").asText(), is("PublicationIssue"));
    assertThat(issue.path("issueNumber").asText(), is("3"));
    assertThat(volume.path("@type").asText(), is("PublicationVolume"));
    assertThat(volume.path("volumeNumber").asText(), is("10"));
    assertThat(periodical.path("@type").asText(), is("Periodical"));
    assertThat(periodical.path("name").asText(), is("Nature"));
    assertThat(periodical.path("issn").asText(), is("1234-5678"));
    assertThat(item.path("pageStart").asText(), is("100"));
    assertThat(item.path("pageEnd").asText(), is("110"));
  }

  @Test
  void shouldOmitPublicationIssueWhenIssueAbsent() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    setInstance(doc, "AcademicArticle", "10", null, null, null);
    var isPartOf = item(transform(doc)).path("isPartOf");
    assertThat(isPartOf.path("@type").asText(), is("PublicationVolume"));
    assertThat(findInChain(isPartOf, "PublicationIssue").isMissingNode(), is(true));
  }

  @Test
  void shouldProducePeriodicalDirectlyWhenVolumeAndIssueAbsent() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "1234-5678", null);
    var isPartOf = item(transform(doc)).path("isPartOf");
    assertThat(isPartOf.path("@type").asText(), is("Periodical"));
    assertThat(isPartOf.path("isPartOf").isMissingNode(), is(true));
  }

  @Test
  void shouldPreferOnlineIssnOverPrintIssn() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "Journal", "Nature", null, "print-1234", "online-5678");
    assertThat(
        findInChain(item(transform(doc)).path("isPartOf"), "Periodical").path("issn").asText(),
        is("online-5678"));
  }

  @Test
  void shouldUseContextTitleForUnconfirmedJournal() {
    var doc = doc("AcademicArticle");
    setJournalContext(doc, "UnconfirmedJournal", null, "Science Advances", null, null);
    assertThat(
        findInChain(item(transform(doc)).path("isPartOf"), "Periodical").path("name").asText(),
        is("Science Advances"));
  }

  // -- @book fields ------------------------------------------------------

  @Test
  void shouldExtractPublisherForBook() {
    var doc = doc("AcademicMonograph");
    setBookContext(doc, "Springer", null);
    var publisher = item(transform(doc)).path("publisher");
    assertThat(publisher.path("@type").asText(), is("Organization"));
    assertThat(publisher.path("name").asText(), is("Springer"));
  }

  @Test
  void shouldExtractSeriesAsBookSeriesForBook() {
    var doc = doc("AcademicMonograph");
    setBookContext(doc, null, "Lecture Notes in CS");
    var isPartOf = item(transform(doc)).path("isPartOf");
    assertThat(isPartOf.path("@type").asText(), is("BookSeries"));
    assertThat(isPartOf.path("name").asText(), is("Lecture Notes in CS"));
  }

  @Test
  void shouldExtractIsbnForBook() {
    var doc = doc("AcademicMonograph");
    setContextIsbn(doc, "9781234567890");
    assertThat(item(transform(doc)).path("isbn").asText(), is("9781234567890"));
  }

  @Test
  void shouldExtractNumberOfPagesForBook() {
    var doc = doc("AcademicMonograph");
    setMonographPages(doc, "248");
    assertThat(item(transform(doc)).path("numberOfPages").asText(), is("248"));
  }

  // -- @chapter / Chapter fields ----------------------------------------

  @Test
  void shouldExtractBookIsPartOfForChapterWithFlatContext() {
    var doc = doc("AcademicChapter");
    setReportContext(doc, "Handbook of CS", "MIT Press");
    var isPartOf = item(transform(doc)).path("isPartOf");
    assertThat(isPartOf.path("@type").asText(), is("Book"));
    assertThat(isPartOf.path("name").asText(), is("Handbook of CS"));
    assertThat(isPartOf.path("publisher").path("name").asText(), is("MIT Press"));
  }

  @Test
  void shouldExtractIsbnForChapterWithFlatContext() {
    var doc = doc("AcademicChapter");
    setReportContext(doc, "Handbook of CS", null);
    setContextIsbn(doc, "9781234500001");
    assertThat(item(transform(doc)).path("isPartOf").path("isbn").asText(), is("9781234500001"));
  }

  @Test
  void shouldExtractPagesForChapter() {
    var doc = doc("AcademicChapter");
    setReportContext(doc, "Handbook of CS", null);
    setInstance(doc, "AcademicChapter", null, null, "42", "55");
    var item = item(transform(doc));
    assertThat(item.path("pageStart").asText(), is("42"));
    assertThat(item.path("pageEnd").asText(), is("55"));
  }

  @Test
  void shouldExtractNestedBookTitleForAnthologyChapter() {
    var doc = doc("AcademicChapter");
    setAnthologyContext(doc, "Anthology", "Collected Essays", "Oxford UP");
    var isPartOf = item(transform(doc)).path("isPartOf");
    assertThat(isPartOf.path("name").asText(), is("Collected Essays"));
    assertThat(isPartOf.path("publisher").path("name").asText(), is("Oxford UP"));
  }

  @Test
  void shouldExtractIsbnForAnthologyChapter() {
    var doc = doc("AcademicChapter");
    setAnthologyContextWithIsbn(doc, "Anthology", "Collected Essays", null, "9781111111111");
    assertThat(item(transform(doc)).path("isPartOf").path("isbn").asText(), is("9781111111111"));
  }

  // -- @inproceedings / Conference ---------------------------------------

  @Test
  void shouldExtractConferenceNameAsBookIsPartOf() {
    var doc = doc("ConferenceLecture");
    setReportContext(doc, "ISWC 2024", null);
    var item = item(transform(doc));
    assertThat(item.path("@type").asText(), is("PresentationDigitalDocument"));
    assertThat(item.path("isPartOf").path("@type").asText(), is("Book"));
    assertThat(item.path("isPartOf").path("name").asText(), is("ISWC 2024"));
  }

  @Test
  void shouldExtractPagesForPresentation() {
    var doc = doc("ConferenceLecture");
    setReportContext(doc, "ISWC 2024", null);
    setInstance(doc, "ConferenceLecture", null, null, "10", "20");
    var item = item(transform(doc));
    assertThat(item.path("pageStart").asText(), is("10"));
    assertThat(item.path("pageEnd").asText(), is("20"));
  }

  // -- @report / Report fields ------------------------------------------

  @Test
  void shouldExtractPublisherAsInstitutionForReport() {
    var doc = doc("ReportResearch");
    setReportContext(doc, null, "SINTEF");
    var item = item(transform(doc));
    assertThat(item.path("@type").asText(), is("Report"));
    assertThat(item.path("publisher").path("name").asText(), is("SINTEF"));
  }

  @Test
  void shouldExtractSeriesAsPeriodicalForReport() {
    var doc = doc("ReportResearch");
    setReportContextWithSeries(doc, null, "SINTEF", "SINTEF Report");
    var isPartOf = item(transform(doc)).path("isPartOf");
    assertThat(isPartOf.path("@type").asText(), is("Periodical"));
    assertThat(isPartOf.path("name").asText(), is("SINTEF Report"));
  }

  @Test
  void shouldExtractIsbnForReport() {
    var doc = doc("ReportResearch");
    setContextIsbn(doc, "9780000000001");
    assertThat(item(transform(doc)).path("isbn").asText(), is("9780000000001"));
  }

  @Test
  void shouldExtractNumberOfPagesForReport() {
    var doc = doc("ReportResearch");
    setMonographPages(doc, "120");
    assertThat(item(transform(doc)).path("numberOfPages").asText(), is("120"));
  }

  // -- thesis fields ---------------------------------------------------

  @Test
  void shouldExtractPublisherAsSchoolForMastersThesis() {
    var doc = doc("DegreeMaster");
    setReportContext(doc, null, "NTNU");
    var item = item(transform(doc));
    assertThat(item.path("@type").asText(), is("Thesis"));
    assertThat(item.path("publisher").path("name").asText(), is("NTNU"));
  }

  @Test
  void shouldExtractPublisherAsSchoolForPhdThesis() {
    var doc = doc("DegreePhd");
    setReportContext(doc, null, "University of Oslo");
    var item = item(transform(doc));
    assertThat(item.path("@type").asText(), is("Thesis"));
    assertThat(item.path("publisher").path("name").asText(), is("University of Oslo"));
  }

  @Test
  void shouldExtractIsbnForPhdThesis() {
    var doc = doc("DegreePhd");
    setContextIsbn(doc, "9789876543211");
    assertThat(item(transform(doc)).path("isbn").asText(), is("9789876543211"));
  }

  @Test
  void shouldExtractNumberOfPagesForThesis() {
    var doc = doc("DegreePhd");
    setMonographPages(doc, "320");
    assertThat(item(transform(doc)).path("numberOfPages").asText(), is("320"));
  }

  // -- LiteraryArts / manifestation fallback ----------------------------

  @Test
  void shouldExtractIsbnFromLiteraryArtsMonographManifestation() {
    var doc = doc("LiteraryArts");
    addLiteraryArtsMonographManifestation(doc, "9780123456789", null, null);
    assertThat(item(transform(doc)).path("isbn").asText(), is("9780123456789"));
  }

  @Test
  void shouldExtractPublisherFromLiteraryArtsMonographManifestation() {
    var doc = doc("LiteraryArts");
    addLiteraryArtsMonographManifestation(doc, null, "Cappelen Damm", null);
    assertThat(item(transform(doc)).path("publisher").path("name").asText(), is("Cappelen Damm"));
  }

  @Test
  void shouldExtractNumberOfPagesFromLiteraryArtsMonographManifestation() {
    var doc = doc("LiteraryArts");
    addLiteraryArtsMonographManifestation(doc, null, null, "212");
    assertThat(item(transform(doc)).path("numberOfPages").asText(), is("212"));
  }

  // -- contributor role mapping -----------------------------------------

  @Test
  void shouldMapCreatorToAuthorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Alice Aaberg", null, null, "Creator");
    var item = item(transform(doc));
    assertThat(item.has("author"), is(true));
    assertThat(item.has("contributor"), is(false));
  }

  @Test
  void shouldMapEditorToEditorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Bob Bakke", null, null, "Editor");
    var item = item(transform(doc));
    assertThat(item.has("editor"), is(true));
    assertThat(item.has("author"), is(false));
  }

  @Test
  void shouldMapTranslatorAdapterToTranslatorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Carol Christoffersen", null, null, "TranslatorAdapter");
    var item = item(transform(doc));
    assertThat(item.has("translator"), is(true));
    assertThat(item.has("author"), is(false));
  }

  @Test
  void shouldMapWriterToAuthorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "David Dahl", null, null, "Writer");
    assertThat(item(transform(doc)).has("author"), is(true));
  }

  @Test
  void shouldMapIllustratorToIllustratorProperty() {
    var doc = doc("AcademicMonograph");
    addContributorNodeWithRole(doc, "Eve Eriksen", null, null, "Illustrator");
    assertThat(item(transform(doc)).has("illustrator"), is(true));
  }

  @Test
  void shouldMapProducerToProducerProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Finn Frydenlund", null, null, "Producer");
    assertThat(item(transform(doc)).has("producer"), is(true));
  }

  @Test
  void shouldMapDirectorToDirectorProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Gerd Grøndahl", null, null, "Director");
    assertThat(item(transform(doc)).has("director"), is(true));
  }

  @Test
  void shouldMapArtisticDirectorToDirectorProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Hans Hansen", null, null, "ArtisticDirector");
    assertThat(item(transform(doc)).has("director"), is(true));
  }

  @Test
  void shouldMapActorToActorProperty() {
    var doc = doc("MovingPicture");
    addContributorNodeWithRole(doc, "Ida Iversen", null, null, "Actor");
    assertThat(item(transform(doc)).has("actor"), is(true));
  }

  @Test
  void shouldMapComposerToComposerProperty() {
    var doc = doc("MusicPerformance");
    addContributorNodeWithRole(doc, "Jan Jensen", null, null, "Composer");
    assertThat(item(transform(doc)).has("composer"), is(true));
  }

  @Test
  void shouldMapSupervisorToContributorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Kari Knutsen", null, null, "Supervisor");
    var item = item(transform(doc));
    assertThat(item.has("contributor"), is(true));
    assertThat(item.has("author"), is(false));
  }

  @Test
  void shouldMapRoleOtherToContributorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Lars Larsen", null, null, "RoleOther");
    assertThat(item(transform(doc)).has("contributor"), is(true));
  }

  @Test
  void shouldMapUnknownRoleToContributorProperty() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Mona Moe", null, null, "SomeFutureRole");
    assertThat(item(transform(doc)).has("contributor"), is(true));
  }

  @Test
  void shouldGroupContributorsByRole() {
    var doc = doc("AcademicArticle");
    addContributorNodeWithRole(doc, "Alice Aaberg", null, null, "Creator");
    addContributorNodeWithRole(doc, "Bob Bakke", null, null, "Editor");
    var item = item(transform(doc));
    assertThat(item.has("author"), is(true));
    assertThat(item.has("editor"), is(true));
  }

  // -- helpers ----------------------------------------------------------

  private static JsonNode transform(String nvaType) {
    return transform(doc(nvaType));
  }

  private static JsonNode transform(ObjectNode doc) {
    return parse(SchemaOrgBibliographyTransformer.transform(List.of(doc), 1));
  }

  private static JsonNode parse(String json) {
    try {
      return MAPPER.readTree(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static JsonNode item(JsonNode tree) {
    return tree.path("itemListElement").get(0);
  }

  private static JsonNode findInChain(JsonNode node, String type) {
    if (node.isMissingNode()) return node;
    if (type.equals(node.path("@type").asText())) return node;
    return findInChain(node.path("isPartOf"), type);
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
