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

class ResourceBibTexTransformerMonographRealDataTest implements BibtexTransformerBase {

  public static final Path PATH = Path.of("bibtex_academic_monograph.json");
  private static List<JsonNode> hits;
  private static String bibtex;

  @BeforeAll
  static void loadAndTransform() throws IOException {
    var data = BibtexTransformerBase.loadAndTransform(PATH);
    hits = data.hits();
    bibtex = data.bibtex();
  }

  @Test
  void shouldProduceOneEntryPerHit() {
    var entries = bibtex.split("\n\n@");
    assertThat(entries.length, is(hits.size()));
  }

  @Test
  void shouldMapAllToBook() {
    assertTypeMatch(bibtex, "@book{");
  }

  @Test
  void shouldExtractIsbn() {
    assertThat(bibtex, containsString("  isbn = {9781032431253}"));
    assertThat(bibtex, containsString("  isbn = {9788867193684}"));
  }

  @Test
  void shouldExtractPublisher() {
    assertThat(bibtex, containsString("  publisher = {Routledge}"));
    assertThat(bibtex, containsString("  publisher = {Wolters Kluwer}"));
  }

  @Test
  void shouldExtractSeriesWhenPresent() {
    assertThat(bibtex, containsString("  series = {Serie Egittologica}"));
  }

  @Test
  void shouldExtractPagesWhenPresent() {
    assertThat(bibtex, containsString("  pages = {301}"));
    assertThat(bibtex, containsString("  pages = {166}"));
  }
}
