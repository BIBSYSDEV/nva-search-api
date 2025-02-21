package no.sikt.nva.oai.pmh.handler;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.openarchives.oai.pmh.v2.HeaderType;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;

public class Prototype {

  public static List<RecordType> from(JsonNode node) {
    if (!node.isArray()) {
      throw new IllegalArgumentException("JSON node must be an array");
    }

    List<RecordType> records = new ArrayList<>();
    for (JsonNode _unused : node) {
      RecordType record = new RecordType();
      HeaderType headerType = getHeaderType(URI.create(""));
      record.setHeader(headerType);
      records.add(record);
    }
    return records;
  }

  private static HeaderType getHeaderType(URI id) {
    HeaderType headerType = new ObjectFactory().createHeaderType();
    headerType.setIdentifier(id.toString());
    return headerType;
  }
}
