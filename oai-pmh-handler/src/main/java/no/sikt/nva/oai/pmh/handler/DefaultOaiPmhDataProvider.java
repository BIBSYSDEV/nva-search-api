package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.datatype.DatatypeFactory;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.SetType;
import org.openarchives.oai.pmh.v2.VerbType;

public class DefaultOaiPmhDataProvider implements OaiPmhDataProvider {
  private static final String PUBLICATION_INSTANCE_TYPE_SET = "PublicationInstanceType";
  private static final String COLON = ":";
  private static final Set<String> INSTANCE_TYPES =
      Set.of(
          "Architecture",
          "ArtisticDesign",
          "MovingPicture",
          "PerformingArts",
          "AcademicArticle",
          "AcademicLiteratureReview",
          "CaseReport",
          "StudyProtocol",
          "ProfessionalArticle",
          "PopularScienceArticle",
          "JournalCorrigendum",
          "JournalLetter",
          "JournalLeader",
          "JournalReview",
          "AcademicMonograph",
          "PopularScienceMonograph",
          "Encyclopedia",
          "ExhibitionCatalog",
          "NonFictionMonograph",
          "Textbook",
          "BookAnthology",
          "DegreeBachelor",
          "DegreeMaster",
          "DegreePhd",
          "DegreeLicentiate",
          "ReportBasic",
          "ReportPolicy",
          "ReportResearch",
          "ReportWorkingPaper",
          "ConferenceReport",
          "ReportBookOfAbstract",
          "AcademicChapter",
          "EncyclopediaChapter",
          "ExhibitionCatalogChapter",
          "Introduction",
          "NonFictionChapter",
          "PopularScienceChapter",
          "TextbookChapter",
          "ChapterConferenceAbstract",
          "ChapterInReport",
          "OtherStudentWork",
          "ConferenceLecture",
          "ConferencePoster",
          "Lecture",
          "OtherPresentation",
          "JournalIssue",
          "ConferenceAbstract",
          "MediaFeatureArticle",
          "MediaBlogPost",
          "MediaInterview",
          "MediaParticipationInRadioOrTv",
          "MediaPodcast",
          "MediaReaderOpinion",
          "MusicPerformance",
          "DataManagementPlan",
          "DataSet",
          "VisualArts",
          "Map",
          "LiteraryArts",
          "ExhibitionProduction",
          "AcademicCommentary",
          "ArtisticDegreePhd");

  private final ObjectFactory objectFactory = new ObjectFactory();

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(final String verb) {
    Optional<VerbType> verbType;
    try {
      verbType = Optional.of(VerbType.fromValue(verb));
    } catch (IllegalArgumentException e) {
      verbType = Optional.empty();
    }

    return verbType
        .map(
            type ->
                switch (type) {
                  case LIST_SETS -> listSets();
                  default -> badVerb("Unsupported verb.");
                })
        .orElseGet(() -> badVerb("Unknown or no verb supplied."));
  }

  private JAXBElement<OAIPMHtype> baseResponse() {
    var responseDate =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()));
    var request = objectFactory.createRequestType();
    var oaiPmhType = objectFactory.createOAIPMHtype();
    oaiPmhType.setResponseDate(responseDate);
    oaiPmhType.setRequest(request);

    return objectFactory.createOAIPMH(oaiPmhType);
  }

  private JAXBElement<OAIPMHtype> listSets() {
    var response = baseResponse();
    var value = response.getValue();
    value.getRequest().setVerb(VerbType.LIST_SETS);

    var listSets = objectFactory.createListSetsType();
    listSets.getSet().addAll(generateSets(objectFactory));
    value.setListSets(listSets);

    return response;
  }

  private List<SetType> generateSets(ObjectFactory objectFactory) {
    var setTypes = new LinkedList<SetType>();
    var setQualifier = objectFactory.createSetType();
    setQualifier.setSetSpec(PUBLICATION_INSTANCE_TYPE_SET);
    setQualifier.setSetName(PUBLICATION_INSTANCE_TYPE_SET);

    setTypes.add(setQualifier);

    var subSetTypes = INSTANCE_TYPES.stream().map(this::wrap).toList();

    setTypes.addAll(subSetTypes);
    return setTypes;
  }

  private SetType wrap(String instanceType) {
    var setType = objectFactory.createSetType();
    setType.setSetSpec(PUBLICATION_INSTANCE_TYPE_SET + COLON + instanceType);
    setType.setSetName(instanceType);

    return setType;
  }

  private JAXBElement<OAIPMHtype> badVerb(String message) {
    var response = baseResponse();
    var value = response.getValue();
    var error = objectFactory.createOAIPMHerrorType();
    error.setCode(OAIPMHerrorcodeType.BAD_VERB);
    error.setValue(message);

    value.getError().add(error);

    return response;
  }
}
