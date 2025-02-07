package no.unit.nva.indexing.model;

import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

public record ReindexRequest(String oldIndex, String newIndex) implements JsonSerializable {

  public static ReindexRequest fromInputStream(InputStream inputStream) throws IOException {
    return JsonUtils.dtoObjectMapper.readValue(inputStream, ReindexRequest.class);
  }
}
