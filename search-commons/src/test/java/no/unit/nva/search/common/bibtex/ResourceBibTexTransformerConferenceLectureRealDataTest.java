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

class ResourceBibTexTransformerConferenceLectureRealDataTest implements BibtexTransformerBase {

  public static final Path PATH = Path.of("bibtex_conference_lecture.json");
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
  void shouldMapAllToInproceedings() {
    var lines = bibtex.lines().filter(l -> l.startsWith("@")).toList();
    assertThat(lines.stream().allMatch(l -> l.startsWith("@inproceedings{")), is(true));
  }

  @Test
  void shouldExtractConferenceNameAsBooktitle() {
    assertThat(bibtex, containsString("  booktitle = {Fagdag migrasjonshelse}"));
  }

  @Test
  void shouldNotIncludePagesForNullPages() {
    assertThat(bibtex, not(containsString("  pages =")));
  }
}
