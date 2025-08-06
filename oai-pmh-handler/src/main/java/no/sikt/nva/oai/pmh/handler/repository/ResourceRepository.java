package no.sikt.nva.oai.pmh.handler.repository;

import java.util.Set;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;

public interface ResourceRepository {
  PagedResponse fetchInitialPage(
      OaiPmhDateTime from, OaiPmhDateTime until, SetSpec setSpec, int pageSize);

  PagedResponse fetchNextPage(String from, OaiPmhDateTime until, SetSpec setSpec, int pageSize);

  Set<String> fetchAvailableInstanceTypes();
}
