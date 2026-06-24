package no.unit.nva.search.common.bibtex;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that {@link ResourceBibTexTransformer#getJsonFields()} covers every field the bibtex
 * transformation reads, by emulating OpenSearch {@code _source} include filtering and asserting
 * that the filtered documents produce the same bibtex output as the full documents.
 */
class ResourceBibTexTransformerSourceFilterTest implements BibtexTransformerBase {

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
        data.hits().stream()
            .map(hit -> filterSource(hit, ResourceBibTexTransformer.getJsonFields()))
            .toList();

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
      var filteredArray = MAPPER.createArrayNode();
      node.forEach(element -> filteredArray.add(filter(element, pathTree)));
      result = filteredArray;
    } else if (node.isObject()) {
      var filteredObject = MAPPER.createObjectNode();
      node.fields()
          .forEachRemaining(
              entry -> {
                var child = pathTree.children.get(entry.getKey());
                if (nonNull(child)) {
                  if (child.children.isEmpty()) {
                    filteredObject.set(entry.getKey(), entry.getValue());
                  } else if (entry.getValue().isContainerNode()) {
                    filteredObject.set(entry.getKey(), filter(entry.getValue(), child));
                  }
                }
              });
      result = filteredObject;
    } else {
      result = node;
    }
    return result;
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
