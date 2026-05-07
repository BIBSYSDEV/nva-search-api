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

class ResourceBibTexTransformerReportRealDataTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static List<JsonNode> hits;
  private static String bibtex;

  @BeforeAll
  static void loadAndTransform() throws IOException {
    var json = stringFromResources(Path.of("bibtex_report_research.json"));
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
  void shouldMapAllToTechreport() {
    var lines = bibtex.lines().filter(l -> l.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(l -> l.startsWith("@techreport{")), is(true));
  }

  @Test
  void shouldExtractInstitution() {
    assertThat(bibtex, containsString("  institution = {NIBIO}"));
  }

  @Test
  void shouldExtractIsbn() {
    assertThat(bibtex, containsString("  isbn = {9788217040064}"));
    assertThat(bibtex, containsString("  isbn = {9788217040088}"));
  }

  @Test
  void shouldExtractSeries() {
    assertThat(bibtex, containsString("  series = {NIBIO Rapport}"));
    assertThat(bibtex, containsString("  series = {Arkeologiske avhandlinger og rapporter}"));
  }

  @Test
  void shouldExtractPagesWhenPresent() {
    assertThat(bibtex, containsString("  pages = {24}"));
  }
}
