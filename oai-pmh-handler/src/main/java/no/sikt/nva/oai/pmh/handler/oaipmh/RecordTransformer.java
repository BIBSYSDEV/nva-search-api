package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.List;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import org.openarchives.oai.pmh.v2.RecordType;

public interface RecordTransformer {
  RecordType transform(ResourceSearchResponse resourceSearchResponse);

  List<RecordType> transform(List<ResourceSearchResponse> hits);
}
