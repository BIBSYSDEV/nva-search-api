package no.unit.nva.indexing.handlers;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.ioutils.IoUtils;

public record DeleteIndicesRequest(Collection<IndexName> indices) implements JsonSerializable {

  @SuppressWarnings("PMD.UnusedAssignment")
  public DeleteIndicesRequest {
    indices = nonNull(indices) ? indices : Collections.emptyList();
  }

  public static CreateIndexRequest fromInputStream(InputStream inputStream) {
    return attempt(() -> IoUtils.streamToString(inputStream))
        .map(value -> dtoObjectMapper.readValue(value, CreateIndexRequest.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    "Could not parse request! %s".formatted(failure.getException())));
  }
}
