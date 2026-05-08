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

class ResourceBibTexTransformerArticleRealDataTest implements BibtexTransformerBase {

  public static final Path PATH = Path.of("bibtex_academic_article.json");
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
  void shouldMapAllToArticle() {
    var lines = bibtex.lines().filter(l -> l.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(l -> l.startsWith("@article{")), is(true));
  }

  @Test
  void shouldExtractJournalName() {
    assertThat(bibtex, containsString("  journal = {European Business Law Review}"));
    assertThat(bibtex, containsString("  journal = {European Review of Private Law}"));
  }

  @Test
  void shouldPreferOnlineIssn() {
    assertThat(bibtex, containsString("  issn = {1875-841X}"));
    assertThat(bibtex, not(containsString("  issn = {0959-6941}")));
  }

  @Test
  void shouldExtractVolumeIssueAndPages() {
    assertThat(bibtex, containsString("  volume = {36}"));
    assertThat(bibtex, containsString("  number = {5}"));
    assertThat(bibtex, containsString("  pages = {695--714}"));
  }

  @Test
  void shouldHandleNonNumericIssue() {
    assertThat(bibtex, containsString("  number = {Special Issue}"));
  }

  @Test
  void shouldStripDoiPrefix() {
    assertThat(bibtex, containsString("  doi = {10.54648/eulr2025047}"));
  }

  @Test
  void shouldNotIncludeDoiWhenAbsent() {
    // hits [2],[3],[6],[8] have no doi — verify we don't get empty doi fields
    assertThat(bibtex, not(containsString("  doi = {}")));
  }
}
