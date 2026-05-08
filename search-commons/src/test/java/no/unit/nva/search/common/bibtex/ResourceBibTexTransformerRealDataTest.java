package no.unit.nva.search.common.bibtex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResourceBibTexTransformerRealDataTest implements BibtexTransformerBase {

  public static final Path PATH = Path.of("bibtex_real_data.json");
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
  void shouldMapAllChapterTypesToInbook() {
    var lines = bibtex.lines().filter(l -> l.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(l -> l.startsWith("@inbook{")), is(true));
  }

  @Test
  void shouldExtractBooktitleFromNestedAnthologyEntityDescription() {
    assertThat(
        bibtex,
        containsString(
            "  booktitle = {Rettsvitenskap og samfunnsengasjement: Festskrift til Hans Petter"
                + " Graver}"));
  }

  @Test
  void shouldExtractPublisherFromNestedAnthologyContext() {
    assertThat(bibtex, containsString("  publisher = {Universitetsforlaget}"));
    assertThat(bibtex, containsString("  publisher = {Springer}"));
  }

  @Test
  void shouldExtractIsbnFromNestedAnthologyContext() {
    assertThat(bibtex, containsString("  isbn = {9788215075297}"));
    assertThat(bibtex, containsString("  isbn = {9783031955518}"));
  }

  @Test
  void shouldExtractPagesAsRange() {
    assertThat(bibtex, containsString("  pages = {210--230}"));
    assertThat(bibtex, containsString("  pages = {77--78}"));
  }

  @Test
  void shouldExtractDoiWhenPresent() {
    assertThat(bibtex, containsString("  doi = {10.4337/9781035313877.00016}"));
  }

  @Test
  void shouldNotIncludePagesWhenBothBoundsAbsent() {
    // hit [5] EncyclopediaChapter has Range with no begin/end
    assertThat(bibtex, not(containsString("  pages = {—")));
  }

  @Test
  void shouldExtractChapterTitle() {
    assertThat(
        bibtex,
        containsString(
            "  title = {Markedsetterforskning - en nyorientering av konkurranseretten?}"));
  }

  @Test
  void shouldExtractYear() {
    assertThat(bibtex, containsString("  year = {2025}"));
  }

  @Test
  void shouldExtractAuthor() {
    assertThat(bibtex, containsString("  author = {Erling Johan Hjelmeng}"));
  }
}
