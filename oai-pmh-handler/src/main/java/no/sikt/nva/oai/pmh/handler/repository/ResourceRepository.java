package no.sikt.nva.oai.pmh.handler.repository;

import java.util.Optional;
import java.util.Set;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.unit.nva.search.resource.response.ResourceSearchResponse;

public interface ResourceRepository {
  PagedResponse fetchInitialPage(
      OaiPmhDateTime from, OaiPmhDateTime until, SetSpec setSpec, int pageSize);

  PagedResponse fetchNextPage(String from, OaiPmhDateTime until, SetSpec setSpec, int pageSize);

  Set<String> fetchAvailableInstanceTypes();

  Optional<ResourceSearchResponse> fetchByIdentifier(String identifier);
}
