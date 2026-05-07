package no.unit.nva.search.common.bibtex;

import nva.commons.core.JacocoGenerated;

public class BibtexType {

  @JacocoGenerated
  public BibtexType() {}

  public static BibtexConstants toBibtexType(String nvaType) {
    return switch (nvaType) {
      case "AcademicArticle",
          "AcademicCommentary",
          "AcademicLiteratureReview",
          "JournalCorrigendum",
          "JournalLeader",
          "JournalLetter",
          "JournalReview",
          "MediaFeatureArticle",
          "MediaReaderOpinion",
          "PopularScienceArticle",
          "ProfessionalArticle",
          "StudyProtocol" ->
          BibtexConstants.ARTICLE;

      case "AcademicMonograph",
          "Architecture",
          "ArtisticDesign",
          "BookAnthology",
          "Encyclopedia",
          "ExhibitionCatalog",
          "LiteraryArts",
          "MusicPerformance",
          "NonFictionMonograph",
          "OtherArtisticOutput",
          "PopularScienceMonograph",
          "Textbook" ->
          BibtexConstants.BOOK;

      case "EncyclopediaChapter",
          "ExhibitionCatalogChapter",
          "Introduction",
          "NonFictionChapter",
          "PopularScienceChapter",
          "TextbookChapter",
          "AcademicChapter",
          "ChapterInReport" ->
          BibtexConstants.INBOOK;

      case "ConferenceAbstract",
          "ChapterConferenceAbstract",
          "ConferenceLecture",
          "ConferencePoster" ->
          BibtexConstants.INPROCEEDINGS;

      case "ConferenceReport",
          "CaseReport",
          "ReportBasic",
          "ReportBookOfAbstract",
          "ReportPolicy",
          "ReportResearch",
          "ReportWorkingPaper" ->
          BibtexConstants.TECHREPORT;

      case "DegreeBachelor", "DegreeLicentiate", "DegreeMaster" -> BibtexConstants.MASTERSTHESIS;

      case "ArtisticDegreePhd", "DegreePhd" -> BibtexConstants.PHDTHESIS;

      default -> BibtexConstants.MISC;
    };
  }
}
