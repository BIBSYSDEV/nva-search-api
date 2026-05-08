package no.unit.nva.search.common.bibtex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResourceBibTexTransformerDegreeMasterRealDataTest implements BibtexTransformerBase {

  private static final Path PATH = Path.of("bibtex_degree_master.json");
  private static List<JsonNode> hits;
  private static String bibtex;

  @BeforeAll
  static void setup() throws IOException {
    var testData = BibtexTransformerBase.loadAndTransform(PATH);
    hits = testData.hits();
    bibtex = testData.bibtext();
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
