package no.unit.nva.search.common.bibtex;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResourceBibTexTransformerDegreeMasterRealDataTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static List<JsonNode> hits;
  private static String bibtex;

  @BeforeAll
  static void loadAndTransform() throws IOException {
    var json = stringFromResources(Path.of("bibtex_degree_master.json"));
    var root = MAPPER.readTree(json);
    hits = StreamSupport.stream(root.path("hits").spliterator(), false).toList();
    bibtex = ResourceBibTexTransformer.transform(hits);
  }

  @Test
  void shouldProduceOneEntryPerHit() {
    var entries = bibtex.split("\n\n@");
    assertThat(entries.length, is(hits.size()));
  }

  @Test
  void shouldMapAllToMastersthesis() {
    var lines = bibtex.lines().filter(l -> l.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(l -> l.startsWith("@mastersthesis{")), is(true));
  }

  @Test
  void shouldExtractSchool() {
    assertThat(bibtex, containsString("  school = {Universitetet i Oslo}"));
  }
}
