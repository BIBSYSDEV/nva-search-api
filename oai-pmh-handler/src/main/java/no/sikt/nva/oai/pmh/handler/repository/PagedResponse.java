package no.sikt.nva.oai.pmh.handler.repository;

import java.util.List;
import no.unit.nva.search.resource.response.ResourceSearchResponse;

public record PagedResponse(int totalSize, List<ResourceSearchResponse> hits) {}
