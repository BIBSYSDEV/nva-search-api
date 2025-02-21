package no.sikt.nva.oai.pmh.handler;

import static org.apache.jena.riot.Lang.JSONLD;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.openarchives.oai.pmh.v2.HeaderType;
import org.openarchives.oai.pmh.v2.OaiDcType;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;

public class Prototype {
  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  public static List<RecordType> from(JsonNode node) {
    if (!node.isArray()) {
      throw new IllegalArgumentException("JSON node must be an array");
    }

    return getResult(node);
  }

  private static void appendContributors(QuerySolution resultItem, OaiDcType oaiDcType) {
    var creatorElement = OBJECT_FACTORY.createElementType();
    creatorElement.setValue(resultItem.getLiteral("contributor").getString());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createCreator(creatorElement));
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
SELECT * WHERE {
  ?id a :Publication ;
      :modifiedDate ?modifiedDate ;
      (:|!:)*/:mainTitle ?title .
}
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
        // appendContributors(resultItem, oaiDcType);
        metadata.setAny(oaiDcType);
        var headerType =
            getHeaderType(
                URI.create(resultItem.getResource("id").getURI()),
                resultItem.getLiteral("modifiedDate").getString());
        record.setHeader(headerType);
        record.setMetadata(metadata);
        records.add(record);
      }
      return records;
    }
  }

  private static HeaderType getHeaderType(URI id, String datestamp) {
    var headerType = new ObjectFactory().createHeaderType();
    headerType.setIdentifier(id.toString());
    headerType.setDatestamp(datestamp);
    return headerType;
  }
}
