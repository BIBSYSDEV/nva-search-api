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

class ResourceBibTexTransformerDegreePhdRealDataTest {

  public static final Path PATH = Path.of("bibtex_degree_phd.json");
  private static List<JsonNode> hits;
  private static String bibtex;

  @BeforeAll
  static void loadAndTransform() throws IOException {
    var data = BibtexTransformerBase.loadAndTransform(PATH);
    hits = data.hits();
    bibtex = data.bibtext();
  }

  @Test
  void shouldProduceOneEntryPerHit() {
    var entries = bibtex.split("\n\n@");
    assertThat(entries.length, is(hits.size()));
  }

  @Test
  void shouldMapAllToPhdthesis() {
    var lines = bibtex.lines().filter(l -> l.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(l -> l.startsWith("@phdthesis{")), is(true));
  }

  @Test
  void shouldExtractSchool() {
    assertThat(bibtex, containsString("  school = {UiT Norges arktiske universitet}"));
    assertThat(bibtex, containsString("  school = {Universitetet i Bergen}"));
  }

  @Test
  void shouldExtractIsbn() {
    assertThat(bibtex, containsString("  isbn = {9788293021551}"));
    assertThat(bibtex, containsString("  isbn = {9788230879573}"));
  }

  @Test
  void shouldExtractPagesWhenPresent() {
    assertThat(bibtex, containsString("  pages = {368}"));
  }
}
