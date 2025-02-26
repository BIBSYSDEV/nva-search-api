package no.unit.nva.search;

import java.net.URI;
import no.unit.nva.search.ticket.TicketParameter;
import no.unit.nva.search.ticket.TicketSearchQuery;
import org.mockito.ArgumentMatcher;

public class TicketSearchQueryArgumentMatcher implements ArgumentMatcher<TicketSearchQuery> {

  private final URI organizationId;

  public TicketSearchQueryArgumentMatcher(URI organizationId) {

    this.organizationId = organizationId;
  }

  @Override
  public boolean matches(TicketSearchQuery query) {
    return query
        .parameters()
        .get(TicketParameter.ORGANIZATION_ID)
        .toString()
        .equals(organizationId.toString());
  }
}
