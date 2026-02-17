package no.unit.nva.indexing.handlers;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElse;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

import java.io.InputStream;
import java.util.Collection;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.ioutils.IoUtils;

public record CreateIndexRequest(Collection<IndexName> indices) implements JsonSerializable {

  public CreateIndexRequest {
    indices = requireNonNullElse(indices, emptyList());
  }

  public static CreateIndexRequest fromInputStream(InputStream inputStream) {
    return attempt(() -> IoUtils.streamToString(inputStream))
        .map(value -> dtoObjectMapper.readValue(value, CreateIndexRequest.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException("Could not parse request!", failure.getException()));
  }
}
