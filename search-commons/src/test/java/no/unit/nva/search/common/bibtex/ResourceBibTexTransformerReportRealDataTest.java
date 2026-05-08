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

class ResourceBibTexTransformerReportRealDataTest implements BibtexTransformerBase {

  public static final Path PATH = Path.of("bibtex_report_research.json");
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
