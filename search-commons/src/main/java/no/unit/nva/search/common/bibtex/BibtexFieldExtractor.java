package no.unit.nva.search.common.bibtex;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

@FunctionalInterface
public interface BibtexFieldExtractor {
  Optional<BibtexField> extract(JsonNode doc);
}
