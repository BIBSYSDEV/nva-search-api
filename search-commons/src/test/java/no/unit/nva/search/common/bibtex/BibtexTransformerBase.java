package no.unit.nva.search.common.bibtex;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import no.unit.nva.commons.json.JsonUtils;

public interface BibtexTransformerBase {
  ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

  static Pair loadAndTransform(Path resources) throws IOException {
    var json = stringFromResources(resources);
    var root = MAPPER.readTree(json);
    var hits = StreamSupport.stream(root.path("hits").spliterator(), false).toList();
    var bibtex = ResourceBibTexTransformer.transform(hits);
    return new Pair(hits, bibtex);
  }

  record Pair(List<JsonNode> hits, String bibtext) {}
}
