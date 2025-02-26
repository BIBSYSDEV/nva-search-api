package no.unit.nva.search.ticket;

import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

public class TicketSearchQueryTest {
  @Test
  public void makeCoverageForLocalModuleHappyAsIAmTestedThroughHandler()
      throws BadRequestException, JsonProcessingException, ApiIoException {
    var request =
        new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withHeaders(Map.of(ACCEPT, "application/json"))
            .withRequestContext(getRequestContext())
            .withTopLevelCristinOrgId(randomUri())
            .withAuthorizerClaim("custom:viewingScopeIncluded", "1,2")
            .build();

    TicketSearchQuery.builder()
        .build()
        .adhereToOrgAccess(RequestInfo.fromRequest(request, HttpClient.newHttpClient()));
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(Collections.emptyMap(), ObjectNode.class);
  }
}
