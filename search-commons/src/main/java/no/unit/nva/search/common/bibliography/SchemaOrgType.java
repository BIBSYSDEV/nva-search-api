package no.unit.nva.search.common.bibliography;

public final class SchemaOrgType {

  /**
   * Field extraction strategy. Fewer buckets than schema.org types because many types share the
   * same data structure (e.g. NewsArticle and ScholarlyArticle both live in a journal/periodical).
   */
  public enum FieldCategory {
    ARTICLE, // journal isPartOf chain, pages
    BOOK, // isbn, numberOfPages, publisher, series
    CHAPTER, // book isPartOf with title/isbn/publisher, pages
    PRESENTATION, // venue name as isPartOf Book, pages
    REPORT, // publisher (institution), isbn, series, pages
    THESIS, // publisher (school), isbn, pages
    OTHER // common fields only
  }

  private SchemaOrgType() {}

  /** Maps an NVA publication instance type to the correct schema.org type string. */
  public static String toSchemaOrgType(String nvaType) {
    return switch (nvaType) {
      case "AcademicArticle",
          "AcademicLiteratureReview",
          "CaseReport",
          "FeatureArticle",
          "JournalArticle",
          "JournalCorrigendum",
          "JournalInterview",
          "JournalLeader",
          "JournalLetter",
          "JournalReview",
          "PopularScienceArticle",
          "ProfessionalArticle",
          "StudyProtocol" ->
          "ScholarlyArticle";

      case "AcademicCommentary",
          "AcademicMonograph",
          "BookAbstracts",
          "BookAnthology",
          "BookMonograph",
          "Encyclopedia",
          "ExhibitionCatalog",
          "NonFictionMonograph",
          "PopularScienceMonograph",
          "Textbook" ->
          "Book";

      case "AcademicChapter",
          "ChapterArticle",
          "ChapterInReport",
          "EncyclopediaChapter",
          "ExhibitionCatalogChapter",
          "Introduction",
          "NonFictionChapter",
          "PopularScienceChapter",
          "TextbookChapter" ->
          "Chapter";

      case "ChapterConferenceAbstract",
          "ConferenceAbstract",
          "ConferenceLecture",
          "ConferencePoster",
          "Lecture",
          "OtherPresentation" ->
          "PresentationDigitalDocument";

      case "ConferenceReport",
          "ReportBasic",
          "ReportBookOfAbstract",
          "ReportPolicy",
          "ReportResearch",
          "ReportWorkingPaper" ->
          "Report";

      case "ArtisticDegreePhd", "DegreeBachelor", "DegreeLicentiate", "DegreeMaster", "DegreePhd" ->
          "Thesis";

      case "MediaFeatureArticle", "MediaInterview" -> "NewsArticle";
      case "MediaReaderOpinion" -> "OpinionNewsArticle";
      case "MediaBlogPost" -> "BlogPosting";
      case "MediaParticipationInRadioOrTv" -> "BroadcastEvent";
      case "MediaPodcast" -> "PodcastEpisode";
      case "DataSet" -> "Dataset";
      case "SoftwareSourceCode" -> "SoftwareSourceCode";
      case "MovingPicture" -> "Movie";
      case "MusicPerformance" -> "MusicEvent";
      case "PerformingArts" -> "Play";
      case "ExhibitionProduction" -> "ExhibitionEvent";
      case "VisualArts" -> "VisualArtwork";
      case "Map" -> "Map";
      case "JournalIssue" -> "PublicationIssue";

      // Architecture, ArtisticDesign, DataManagementPlan, LiteraryArts,
      // OtherArtisticOutput, OtherStudentWork, and any unknown types
      default -> "CreativeWork";
    };
  }

  /**
   * Maps an NVA publication instance type to the field extraction strategy. This is deliberately
   * coarser than toSchemaOrgType — many schema.org types share the same underlying data shape.
   *
   * <p>Note: LiteraryArts uses BOOK extraction because NVA stores its data in a
   * LiteraryArtsMonograph manifestation that carries isbn, publisher, and page count.
   */
  public static FieldCategory fieldCategory(String nvaType) {
    return switch (nvaType) {
      case "AcademicArticle",
          "AcademicLiteratureReview",
          "CaseReport",
          "FeatureArticle",
          "JournalArticle",
          "JournalCorrigendum",
          "JournalInterview",
          "JournalLeader",
          "JournalLetter",
          "JournalReview",
          "MediaFeatureArticle",
          "MediaInterview",
          "MediaReaderOpinion",
          "PopularScienceArticle",
          "ProfessionalArticle",
          "StudyProtocol" ->
          FieldCategory.ARTICLE;

      case "AcademicCommentary",
          "AcademicMonograph",
          "BookAbstracts",
          "BookAnthology",
          "BookMonograph",
          "Encyclopedia",
          "ExhibitionCatalog",
          "LiteraryArts",
          "NonFictionMonograph",
          "PopularScienceMonograph",
          "Textbook" ->
          FieldCategory.BOOK;

      case "AcademicChapter",
          "ChapterArticle",
          "ChapterInReport",
          "EncyclopediaChapter",
          "ExhibitionCatalogChapter",
          "Introduction",
          "NonFictionChapter",
          "PopularScienceChapter",
          "TextbookChapter" ->
          FieldCategory.CHAPTER;

      case "ChapterConferenceAbstract",
          "ConferenceAbstract",
          "ConferenceLecture",
          "ConferencePoster",
          "Lecture",
          "OtherPresentation" ->
          FieldCategory.PRESENTATION;

      case "ConferenceReport",
          "ReportBasic",
          "ReportBookOfAbstract",
          "ReportPolicy",
          "ReportResearch",
          "ReportWorkingPaper" ->
          FieldCategory.REPORT;

      case "ArtisticDegreePhd", "DegreeBachelor", "DegreeLicentiate", "DegreeMaster", "DegreePhd" ->
          FieldCategory.THESIS;

      default -> FieldCategory.OTHER;
    };
  }
}
