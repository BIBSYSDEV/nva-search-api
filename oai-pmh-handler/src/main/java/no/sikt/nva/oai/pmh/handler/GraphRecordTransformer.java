package no.sikt.nva.oai.pmh.handler;

import static org.apache.jena.riot.Lang.JSONLD;

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
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.openarchives.oai.pmh.v2.HeaderType;
import org.openarchives.oai.pmh.v2.OaiDcType;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GraphRecordTransformer implements RecordTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(GraphRecordTransformer.class);
  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
  private static final String JSON_LD_GRAPH = "@graph";
  private static final String JSON_LD_CONTEXT = "@context";

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
    return new ObjectNode(
        JsonNodeFactory.instance, Map.of(JSON_LD_GRAPH, arrayNode, JSON_LD_CONTEXT, contextNode));
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
    String[] contributors = resultItem.getLiteral("contributor").getString().split("\\|");
    for (String contributor : contributors) {
      var creatorElement = OBJECT_FACTORY.createElementType();
      creatorElement.setValue(contributor);
      oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createCreator(creatorElement));
    }
  }

  private static void appendTitle(QuerySolution resultItem, OaiDcType oaiDcType) {
    var titleElement = OBJECT_FACTORY.createElementType();
    titleElement.setValue(resultItem.getLiteral("title").getString());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createTitle(titleElement));
  }

  private static List<RecordType> getResult(JsonNode node) {
    var query =
        """
PREFIX : <https://nva.sikt.no/ontology/publication#>
SELECT ?id ?modifiedDate ?title ?date (GROUP_CONCAT(?name; separator="|") AS ?contributor) WHERE {
  ?id a :Publication ;
      :modifiedDate ?modifiedDate ;
      (:|!:)*/:mainTitle ?title ;
      (:|!:)*/:publicationDate ?publicationDate ;
      (:|!:)*/:contributor/:identity/:name ?name .

  ?publicationDate :year ?year .
  ?publicationDate :month ?month .
  ?publicationDate :day ?day .

  BIND (IF(BOUND(?day) && BOUND(?month) && BOUND(?year), CONCAT(?year, CONCAT("-", CONCAT(?month, CONCAT("-", ?day)))),?year) AS ?date)

}
GROUP BY ?id ?modifiedDate ?title ?date
""";
    var model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, new ByteArrayInputStream(node.toString().getBytes()), JSONLD);
    try (var queryExecution = QueryExecutionFactory.create(query, model)) {
      var result = queryExecution.execSelect();

      List<RecordType> records = new ArrayList<>();
      while (result.hasNext()) {
        var resultItem = result.next();
        var record = new RecordType();
        var metadata = OBJECT_FACTORY.createMetadataType();
        var oaiDcType = OBJECT_FACTORY.createOaiDcType();
        appendTitle(resultItem, oaiDcType);
        appendContributors(resultItem, oaiDcType);
        appendDate(resultItem, oaiDcType);
        metadata.setAny(OBJECT_FACTORY.createDc(oaiDcType));
        var headerType =
            getHeaderType(
                URI.create(resultItem.getResource("id").getURI()),
                resultItem.getLiteral("modifiedDate").getString());
        record.setHeader(headerType);
        record.setMetadata(metadata);
        records.add(record);
      }
      return records;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void appendDate(QuerySolution resultItem, OaiDcType oaiDcType) {
    var dateElement = OBJECT_FACTORY.createElementType();
    dateElement.setValue(resultItem.getLiteral("date").getString());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createDate(dateElement));
  }

  private static HeaderType getHeaderType(URI id, String datestamp) {
    var headerType = new ObjectFactory().createHeaderType();
    headerType.setIdentifier(id.toString());
    headerType.setDatestamp(datestamp);
    return headerType;
  }
}
