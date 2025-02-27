package no.unit.nva.search;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.search.ticket.TicketSearchQuery;
import org.mockito.ArgumentMatcher;

public class TicketSearchQueryArgumentMatcher implements ArgumentMatcher<TicketSearchQuery> {

  private final Set<URI> organizationIds;

  public TicketSearchQueryArgumentMatcher(URI... organizationIds) {
    this.organizationIds = Arrays.stream(organizationIds).collect(Collectors.toSet());
  }

  @Override
  public boolean matches(TicketSearchQuery query) {
    var matches = true;
    for (var id : organizationIds) {
      if (!query.hasOrganization(id)) {
        matches = false;
        break;
      }
    }
    return matches;
  }
}
