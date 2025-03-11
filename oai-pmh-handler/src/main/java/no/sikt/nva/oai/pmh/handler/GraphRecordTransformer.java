package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.nonNull;
import static org.apache.jena.riot.Lang.JSONLD;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonUtils;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.openarchives.oai.pmh.v2.HeaderType;
import org.openarchives.oai.pmh.v2.MetadataType;
import org.openarchives.oai.pmh.v2.OaiDcType;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GraphRecordTransformer implements RecordTransformer {

  private static final String SPARQL_QUERY =
      """
PREFIX : <https://nva.sikt.no/ontology/publication#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?id ?modifiedDate ?title ?date ?type ?publisher (GROUP_CONCAT(?name; separator="|") AS ?contributor) WHERE {
?id a :Publication ;
:modifiedDate ?modifiedDate ;
(:|!:)*/:mainTitle ?title ;
(:|!:)*/:publicationDate ?publicationDate ;
(:|!:)*/:publicationInstance/rdf:type ?type .

OPTIONAL { ?id (:|!:)*/:contributor/:identity/:name ?name . }

OPTIONAL { ?id (:|!:)*/:publicationContext/:type ?contextType . }
OPTIONAL { ?id (:|!:)*/:publicationContext/:name ?contextName . }

OPTIONAL { ?publicationDate :year ?year . }
OPTIONAL { ?publicationDate :month ?month . }
OPTIONAL { ?publicationDate :day ?day . }

BIND (IF(BOUND(?day) && BOUND(?month) && BOUND(?year), CONCAT(?year, CONCAT("-", CONCAT(?month, CONCAT("-", ?day)))),?year) AS ?date)
BIND(IF(BOUND(?contextType) && ?contextType = :Book, ?contextName, rdf:null) AS ?publisher)
}
GROUP BY ?id ?modifiedDate ?title ?date ?type ?publisher
""";
  private static final Logger LOGGER = LoggerFactory.getLogger(GraphRecordTransformer.class);
  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
  private static final String JSON_LD_GRAPH = "@graph";
  private static final String JSON_LD_CONTEXT = "@context";
  private static final String TITLE = "title";
  private static final String CONTRIBUTOR = "contributor";
  private static final String ID = "id";
  private static final String MODIFIED_DATE = "modifiedDate";
  private static final String PUBLISHER = "publisher";
  private static final String DATE = "date";
  private static final String TYPE = "type";

  public GraphRecordTransformer() {}

  @Override
  public List<RecordType> transform(List<JsonNode> hits) {
    LOGGER.info(String.format("Transforming %d hits!", Objects.isNull(hits) ? 0 : hits.size()));
    if (Objects.isNull(hits) || hits.isEmpty()) {
      return Collections.emptyList();
    }
    var optimized = optimizeForGraph(hits);
    return getResult(optimized);
  }

  private static JsonNode optimizeForGraph(List<JsonNode> hits) {
    var contextNode = collectContext(hits);
    var arrayNode = new ArrayNode(JsonNodeFactory.instance);
    hits.forEach(arrayNode::add);
    var jsonGraph =
        new ObjectNode(
            JsonNodeFactory.instance,
            Map.of(JSON_LD_GRAPH, arrayNode, JSON_LD_CONTEXT, contextNode));
    try {
      LOGGER.info(JsonUtils.dtoObjectMapper.writeValueAsString(jsonGraph));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return jsonGraph;
  }

  private static JsonNode collectContext(List<JsonNode> nodes) {
    JsonNode contextNode = null;
    for (var node : nodes) {
      if (node.isObject()) {
        contextNode = node.get(JSON_LD_CONTEXT);
        ((ObjectNode) node).remove(JSON_LD_CONTEXT);
      } else {
        throw new IllegalArgumentException(
            String.format("Expected only object nodes. Got %s\n", node.getNodeType().name()));
      }
    }

    return contextNode;
  }

  private static void appendContributors(QuerySolution resultItem, OaiDcType oaiDcType) {
    String[] contributors = resultItem.getLiteral(CONTRIBUTOR).getString().split("\\|");
    for (String contributor : contributors) {
      var creatorElement = OBJECT_FACTORY.createElementType();
      creatorElement.setValue(contributor);
      oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createCreator(creatorElement));
    }
  }

  private static void appendTitle(QuerySolution resultItem, OaiDcType oaiDcType) {
    var titleElement = OBJECT_FACTORY.createElementType();
    titleElement.setValue(resultItem.getLiteral(TITLE).getString());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createTitle(titleElement));
  }

  private static List<RecordType> getResult(JsonNode node) {
    var model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, new ByteArrayInputStream(node.toString().getBytes()), JSONLD);
    try (var queryExecution = QueryExecutionFactory.create(SPARQL_QUERY, model)) {
      var result = queryExecution.execSelect();

      List<RecordType> records = new ArrayList<>();
      while (result.hasNext()) {
        var resultItem = result.next();
        var record = new RecordType();
        var metadata = pupulateMetadata(resultItem);
        var headerType =
            getHeaderType(
                URI.create(resultItem.getResource(ID).getURI()),
                resultItem.getLiteral(MODIFIED_DATE).getString());
        record.setHeader(headerType);
        record.setMetadata(metadata);
        records.add(record);
      }
      return records;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static MetadataType pupulateMetadata(QuerySolution resultItem) {
    var metadata = OBJECT_FACTORY.createMetadataType();
    var oaiDcType = OBJECT_FACTORY.createOaiDcType();
    appendTitle(resultItem, oaiDcType);
    appendContributors(resultItem, oaiDcType);
    appendDate(resultItem, oaiDcType);
    appendType(resultItem, oaiDcType);
    appendPublisher(resultItem, oaiDcType);
    metadata.setAny(OBJECT_FACTORY.createDc(oaiDcType));
    return metadata;
  }

  private static void appendPublisher(QuerySolution resultItem, OaiDcType oaiDcType) {
    var publisher =
        resultItem.get(PUBLISHER).isResource()
            ? null
            : resultItem.getLiteral(PUBLISHER).getString();
    if (nonNull(publisher)) {
      var publisherElement = OBJECT_FACTORY.createElementType();
      publisherElement.setValue(publisher);
      oaiDcType
          .getTitleOrCreatorOrSubject()
          .addLast(OBJECT_FACTORY.createPublisher(publisherElement));
    }
  }

  private static void appendDate(QuerySolution resultItem, OaiDcType oaiDcType) {
    var dateElement = OBJECT_FACTORY.createElementType();
    dateElement.setValue(resultItem.getLiteral(DATE).getString());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createDate(dateElement));
  }

  private static void appendType(QuerySolution resultItem, OaiDcType oaiDcType) {
    var typeElement = OBJECT_FACTORY.createElementType();
    typeElement.setValue(resultItem.getResource(TYPE).getLocalName());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createType(typeElement));
  }

  private static HeaderType getHeaderType(URI id, String datestamp) {
    var headerType = new ObjectFactory().createHeaderType();
    headerType.setIdentifier(id.toString());
    headerType.setDatestamp(datestamp);
    return headerType;
  }
}
