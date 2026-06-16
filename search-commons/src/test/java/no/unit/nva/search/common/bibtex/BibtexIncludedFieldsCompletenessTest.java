package no.unit.nva.search.common.bibtex;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.common.bibtex.ResourceBibTexTransformer.getBibTexFields;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that {@code Constants.BIBTEX_INCLUDED_FIELDS} (the OpenSearch {@code _source} include
 * list used for BibTeX requests) covers every field the BibTeX transformation reads. The test
 * emulates {@code _source} include filtering and asserts that the filtered documents produce the
 * same BibTeX output as the full documents. If a future change reads a field outside the include
 * list, the slimmed search response would silently drop it and this test fails.
 */
class BibtexIncludedFieldsCompletenessTest implements BibtexTransformerBase {

  @ParameterizedTest(name = "should produce identical bibtex from source-filtered hits in {0}")
  @ValueSource(
      strings = {
        "bibtex_real_data.json",
        "bibtex_academic_article.json",
        "bibtex_academic_monograph.json",
        "bibtex_conference_lecture.json",
        "bibtex_degree_master.json",
        "bibtex_degree_phd.json",
        "bibtex_report_research.json"
      })
  void shouldProduceIdenticalBibtexFromSourceFilteredDocuments(String resourceFile)
      throws IOException {
    var data = BibtexTransformerBase.loadAndTransform(Path.of(resourceFile));
    var filteredHits =
        data.hits().stream().map(hit -> filterSource(hit, getBibTexFields())).toList();

    var filteredBibtex = ResourceBibTexTransformer.transform(filteredHits);

    assertThat(filteredBibtex).isNotEmpty().isEqualTo(data.bibtex());
  }

  private static JsonNode filterSource(JsonNode document, Collection<String> includedPaths) {
    var pathTree = new PathNode();
    includedPaths.forEach(pathTree::add);
    return filter(document, pathTree);
  }

  private static JsonNode filter(JsonNode node, PathNode pathTree) {
    JsonNode result;
    if (node.isArray()) {
      result = filterArray(node, pathTree);
    } else if (node.isObject()) {
      result = filterObject(node, pathTree);
    } else {
      result = node;
    }
    return result;
  }

  private static JsonNode filterArray(JsonNode node, PathNode pathTree) {
    var filteredArray = MAPPER.createArrayNode();
    node.forEach(element -> filteredArray.add(filter(element, pathTree)));
    return filteredArray;
  }

  private static JsonNode filterObject(JsonNode node, PathNode pathTree) {
    var filteredObject = MAPPER.createObjectNode();
    node.fields().forEachRemaining(entry -> copyIncludedField(filteredObject, entry, pathTree));
    return filteredObject;
  }

  private static void copyIncludedField(
      ObjectNode target, Map.Entry<String, JsonNode> entry, PathNode pathTree) {
    var child = pathTree.children.get(entry.getKey());
    var isLeafInclude = nonNull(child) && child.children.isEmpty();
    var isRecursiveInclude =
        nonNull(child) && !child.children.isEmpty() && entry.getValue().isContainerNode();
    if (isLeafInclude) {
      target.set(entry.getKey(), entry.getValue());
    } else if (isRecursiveInclude) {
      target.set(entry.getKey(), filter(entry.getValue(), child));
    }
  }

  private static final class PathNode {
    private final Map<String, PathNode> children = new HashMap<>();

    private void add(String dotPath) {
      var currentNode = this;
      for (var segment : dotPath.split("\\.")) {
        currentNode = currentNode.children.computeIfAbsent(segment, key -> new PathNode());
      }
    }
  }
}
