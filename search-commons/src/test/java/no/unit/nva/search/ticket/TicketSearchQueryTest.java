package no.unit.nva.search.ticket;

import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

public class TicketSearchQueryTest {
  @Test
  public void makeCoverageForLocalModuleHappyAsIAmTestedThroughHandler()
      throws BadRequestException, JsonProcessingException, ApiIoException, UnauthorizedException {
    var firstViewingScope = randomUri();
    var secondViewingScope = randomUri();
    var viewingScopes =
        String.join(",", Set.of(firstViewingScope.toString(), secondViewingScope.toString()));
    var request =
        new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withHeaders(Map.of(ACCEPT, "application/json"))
            .withRequestContext(getRequestContext())
            .withTopLevelCristinOrgId(randomUri())
            .withAuthorizerClaim("custom:viewingScopeIncluded", viewingScopes)
            .withUserName(randomString())
            .build();

    TicketSearchQuery.builder()
        .build()
        .withFilter()
        .fromRequestInfo(RequestInfo.fromRequest(request, HttpClient.newHttpClient()))
        .hasOrganization(firstViewingScope);
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(Collections.emptyMap(), ObjectNode.class);
  }
}
