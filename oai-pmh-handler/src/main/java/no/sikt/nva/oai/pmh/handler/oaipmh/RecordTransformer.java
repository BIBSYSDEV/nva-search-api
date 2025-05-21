package no.sikt.nva.oai.pmh.handler.oaipmh;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.openarchives.oai.pmh.v2.RecordType;

public interface RecordTransformer {
  List<RecordType> transform(List<JsonNode> hits);
}
