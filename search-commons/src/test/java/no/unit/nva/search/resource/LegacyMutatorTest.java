package no.unit.nva.search.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class LegacyMutatorTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldHandleEmptyObject() {
    assertDoesNotThrow(() -> new LegacyMutator().transform(objectMapper.createObjectNode()));
  }

  @Test
  void shouldUseContributorsPreviewInPlaceOfContributorsIfNotPresent()
      throws JsonProcessingException {
    var json =
        """
{
  "entityDescription": {
    "contributorsPreview": [{"type": "Contributor", "id": "2"}]
  }
}
""";
    var mutated = new LegacyMutator().transform(new ObjectMapper().readTree(json));

    var contributors = new ArrayList<JsonNode>();
    mutated.at("/entityDescription/contributors").elements().forEachRemaining(contributors::add);

    assertThat(contributors, iterableWithSize(1));
  }
}
