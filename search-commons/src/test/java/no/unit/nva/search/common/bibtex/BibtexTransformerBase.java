package no.unit.nva.search.common.bibtex;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

  default void assertTypeMatch(String bibtex, String matcher) {
    var lines = bibtex.lines().filter(line -> line.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(line -> line.startsWith(matcher)), is(true));
  }

  record Pair(List<JsonNode> hits, String bibtex) {}
}
