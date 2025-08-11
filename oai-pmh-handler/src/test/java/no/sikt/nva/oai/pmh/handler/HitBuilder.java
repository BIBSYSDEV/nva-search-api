package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HitBuilder {

  private final ObjectNode referenceNode;
  private final int port;
  private String identifier;
  private String title;
  private URI language;
  private URI nvaDoi;
  private String[] contributors = new String[] {};
  private ObjectNode publicationDateNode;
  private boolean publicationDatePresent = true;
  private final Set<String> scopusIdentifiers = new HashSet<>();
  private final Set<String> cristinIdentifiers = new HashSet<>();
  private final Set<String> handleIdentifiers = new HashSet<>();
  private final Set<String> untypedIdentifiers = new HashSet<>();

  private HitBuilder(int port, ObjectNode referenceNode) {
    this.port = port;
    this.referenceNode = referenceNode;
  }

  public static HitBuilder academicArticle(int port, String journalName) {
    var referenceNode = academicArticleReferenceNode(journalName);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder academicArticleWithMissingJournalInformation(int port) {
    var referenceNode = academicArticleReferenceNode(null);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder reportBasicWithMissingChannelName(int port) {
    var referenceNode = reportBasicReferenceNode(null, null);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder reportBasic(int port, String publisherName, String seriesName) {
    var referenceNode = reportBasicReferenceNode(publisherName, seriesName);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder bookAnthology(
      int port,
      String publisherName,
      String seriesName,
      String seriesPrintIssn,
      String seriesOnlineIssn,
      Set<String> isbnList,
      URI doi) {
    var referenceNode =
        bookAnthologyReferenceNode(
            publisherName, seriesName, seriesPrintIssn, seriesOnlineIssn, isbnList, doi);
    return new HitBuilder(port, referenceNode);
  }

  public HitBuilder withIdentifier(String identifier) {
    this.identifier = identifier;
    return this;
  }

  public HitBuilder withOtherIdentifier(String identifierType, String identifier) {
    switch (identifierType) {
      case "cristin":
        cristinIdentifiers.add(identifier);
        break;
      case "handle":
        handleIdentifiers.add(identifier);
        break;
      case "scopus":
        scopusIdentifiers.add(identifier);
        break;
      case "untyped":
        untypedIdentifiers.add(identifier);
        break;
      default:
        throw new IllegalArgumentException(
            "Identifier type " + identifierType + " is not supported");
    }
    return this;
  }

  public HitBuilder withTitle(String title) {
    this.title = title;
    return this;
  }

  public HitBuilder withLanguage(URI language) {
    this.language = language;
    return this;
  }

  public HitBuilder withContributors(String... contributors) {
    this.contributors = contributors;
    return this;
  }

  public HitBuilder withEmptyPublicationDate() {
    publicationDateNode = new ObjectNode(JsonNodeFactory.instance);
    return this;
  }

  public HitBuilder withNoPublicationDate() {
    this.publicationDatePresent = false;
    return this;
  }

  public HitBuilder withNvaDoi(URI nvaDoi) {
    this.nvaDoi = nvaDoi;
    return this;
  }

  public ObjectNode build() {
    var rootNode = new ObjectNode(JsonNodeFactory.instance);
    rootNode.put("type", "Publication");
    rootNode.put("@context", "https://localhost:" + port + "/publication/context");
    rootNode.put("id", "https://localhost/publication/" + this.identifier);
    rootNode.put("identifier", this.identifier);
    if (nonNull(nvaDoi)) {
      rootNode.put("doi", nvaDoi.toString());
    }
    var publicationDateNodeToUse = resolvePublicationDateNode();
    var contributorsPreviewNode = new ArrayNode(JsonNodeFactory.instance);
    Arrays.stream(contributors)
        .forEach(contributor -> contributorsPreviewNode.add(contributorNode(contributor)));
    rootNode.set(
        "entityDescription",
        entityDescriptionNode(
            title, referenceNode, language, publicationDateNodeToUse, contributorsPreviewNode));
    rootNode.set(
        "additionalIdentifiers",
        additionalIdentifiersNode(
            cristinIdentifiers, handleIdentifiers, scopusIdentifiers, untypedIdentifiers));
    rootNode.put("modifiedDate", "2023-01-01T01:02:03.123456789Z");
    return rootNode;
  }

  private ObjectNode resolvePublicationDateNode() {
    ObjectNode publicationDateNodeToUse;
    if (publicationDatePresent && isNull(publicationDateNode)) {
      publicationDateNodeToUse = new ObjectNode(JsonNodeFactory.instance);
      publicationDateNodeToUse.put("year", "2020");
      publicationDateNodeToUse.put("month", "01");
      publicationDateNodeToUse.put("day", "01");
    } else if (publicationDatePresent) {
      publicationDateNodeToUse = publicationDateNode;
    } else {
      publicationDateNodeToUse = null;
    }
    return publicationDateNodeToUse;
  }

  private static ObjectNode contributorNode(String contributor) {
    var contributorNode = new ObjectNode(JsonNodeFactory.instance);
    contributorNode.set("identity", identityNode(contributor));
    return contributorNode;
  }

  private static ObjectNode identityNode(String contributor) {
    var identityNode = new ObjectNode(JsonNodeFactory.instance);
    identityNode.put("name", contributor);
    return identityNode;
  }

  private static ObjectNode entityDescriptionNode(
      String title,
      ObjectNode referenceNode,
      URI language,
      ObjectNode publicationDateNode,
      ArrayNode contributorsPreviewNode) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.set("reference", referenceNode);
    if (nonNull(publicationDateNode)) {
      node.set("publicationDate", publicationDateNode);
    }
    if (nonNull(language)) {
      node.put("language", language.toString());
    }
    node.put("mainTitle", title);
    node.set("contributorsPreview", contributorsPreviewNode);
    return node;
  }

  private static ArrayNode additionalIdentifiersNode(
      Set<String> cristinIdentifiers,
      Set<String> handleIdentifiers,
      Set<String> scopusIdentifiers,
      Set<String> untypedIdentifiers) {

    var additionalIdentifiersNode = new ArrayNode(JsonNodeFactory.instance);

    cristinIdentifiers.forEach(
        cristinIdentifier -> {
          var identifierNode = buildIdentifier("CristinIdentifier", cristinIdentifier);
          additionalIdentifiersNode.add(identifierNode);
        });

    handleIdentifiers.forEach(
        handleIdentifier -> {
          var identifierNode = buildIdentifier("HandleIdentifier", handleIdentifier);
          additionalIdentifiersNode.add(identifierNode);
        });

    scopusIdentifiers.forEach(
        scopusIdentifier -> {
          var identifierNode = buildIdentifier("ScopusIdentifier", scopusIdentifier);
          additionalIdentifiersNode.add(identifierNode);
        });

    untypedIdentifiers.forEach(
        untypedIdentifier -> {
          var identifierNode = buildIdentifier("AdditionalIdentifier", untypedIdentifier);
          additionalIdentifiersNode.add(identifierNode);
        });

    return additionalIdentifiersNode;
  }

  private static ObjectNode buildIdentifier(String type, String cristinIdentifier) {
    var identifierNode = new ObjectNode(JsonNodeFactory.instance);
    identifierNode.put("type", type);
    identifierNode.put("value", cristinIdentifier);

    return identifierNode;
  }

  private static ObjectNode academicArticleReferenceNode(String journalName) {
    var publicationInstance = new ObjectNode(JsonNodeFactory.instance);
    publicationInstance.put("type", "AcademicArticle");
    var publicationContext = new ObjectNode(JsonNodeFactory.instance);
    publicationContext.put("type", "Journal");
    if (nonNull(journalName)) {
      publicationContext.put("name", journalName);
    }

    var referenceNode = new ObjectNode(JsonNodeFactory.instance);
    referenceNode.put("type", "Reference");
    referenceNode.set("publicationInstance", publicationInstance);
    referenceNode.set("publicationContext", publicationContext);

    return referenceNode;
  }

  private static ObjectNode reportBasicReferenceNode(String publisherName, String seriesName) {
    var publicationInstance = new ObjectNode(JsonNodeFactory.instance);
    publicationInstance.put("type", "ReportBasic");
    var publicationContext = new ObjectNode(JsonNodeFactory.instance);
    publicationContext.put("type", "Report");
    publicationContext.set("publisher", publisherNode(publisherName));
    publicationContext.set("series", seriesNode(seriesName, null, null));

    var referenceNode = new ObjectNode(JsonNodeFactory.instance);
    referenceNode.put("type", "Reference");
    referenceNode.set("publicationInstance", publicationInstance);
    referenceNode.set("publicationContext", publicationContext);

    return referenceNode;
  }

  /*
   "reference" : {
     "type" : "Reference",
     "publicationContext" : {
       "type" : "Book",
       "series" : {
         "type" : "Series",
         "id" : "https://api.dev.nva.aws.unit.no/publication-channels/0714a3cc-73b5-4aae-b422-3eb5fee32c61"
       },
       "seriesNumber" : "Lk10fDCnWtnTE3klO",
       "publisher" : {
         "type" : "Publisher",
         "id" : "https://api.dev.nva.aws.unit.no/publication-channels/825d68f5-cf52-4eb4-b7bb-7d8e34c28ac3",
         "valid" : true
       },
       "isbnList" : [ "9781897852323", "9780972665414" ],
       "revision" : "Unrevised",
       "additionalIdentifiers" : [ {
         "type" : "AdditionalIdentifier",
         "sourceName" : "ISBN",
         "value" : "0972665412"
       } ]
     },
     "doi" : "https://www.example.org/e99df1d0-38a5-49b3-82a8-f529d300e638",
     "publicationInstance" : {
       "type" : "BookAnthology",
       "pages" : {
         "type" : "MonographPages",
         "introduction" : {
           "type" : "Range",
           "begin" : "1a4pzdWtTFKPBXse",
           "end" : "B5zJ5p0X0qGZGVp"
         },
         "pages" : "CXok9z6Il94Gkm4",
         "illustrated" : false
       }
     }
   },
  */
  private static ObjectNode bookAnthologyReferenceNode(
      String publisherName,
      String seriesName,
      String seriesPrintIssn,
      String seriesOnlineIssn,
      Set<String> isbnList,
      URI doi) {
    var publicationInstance = new ObjectNode(JsonNodeFactory.instance);
    publicationInstance.put("type", "BookAnthology");
    var publicationContext = new ObjectNode(JsonNodeFactory.instance);
    publicationContext.put("type", "Book");
    publicationContext.set("publisher", publisherNode(publisherName));
    publicationContext.set("series", seriesNode(seriesName, seriesPrintIssn, seriesOnlineIssn));
    publicationContext.set("isbnList", toArrayNodeOfStrings(isbnList));

    var referenceNode = new ObjectNode(JsonNodeFactory.instance);
    referenceNode.put("type", "Reference");
    referenceNode.set("publicationInstance", publicationInstance);
    referenceNode.set("publicationContext", publicationContext);
    if (nonNull(doi)) {
      referenceNode.put("doi", doi.toString());
    }

    return referenceNode;
  }

  private static ArrayNode toArrayNodeOfStrings(Collection<String> elements) {
    var arrayNode = new ArrayNode(JsonNodeFactory.instance);
    elements.forEach(arrayNode::add);
    return arrayNode;
  }

  private static ObjectNode publisherNode(String publisherName) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    if (nonNull(publisherName)) {
      node.put("name", publisherName);
    }
    return node;
  }

  private static ObjectNode seriesNode(String seriesName, String printIssn, String onlineIssn) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    if (nonNull(seriesName)) {
      node.put("name", seriesName);
    }
    if (nonNull(printIssn)) {
      node.put("printIssn", printIssn);
    }
    if (nonNull(onlineIssn)) {
      node.put("onlineIssn", onlineIssn);
    }
    return node;
  }
}
