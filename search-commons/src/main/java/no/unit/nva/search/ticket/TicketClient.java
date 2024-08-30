package no.unit.nva.search.ticket;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.common.jwt.Tools.getCachedJwtProvider;
import static no.unit.nva.search.common.records.SwsResponse.SwsResponseBuilder.swsResponseBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.function.BinaryOperator;
import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;

/**
 * Client for Ticket.
 *
 * @author Stig Norland
 */
public class TicketClient extends OpenSearchClient<SwsResponse, TicketSearchQuery> {

  public TicketClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
    super(client, cachedJwtProvider);
  }

  @JacocoGenerated
  public static TicketClient defaultClient() {
    var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
    return new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);
  }

  @Override
  protected SwsResponse jsonToResponse(HttpResponse<String> response)
      throws JsonProcessingException {
    return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
  }

  @Override
  protected BinaryOperator<SwsResponse> responseAccumulator() {
    return (a, b) -> swsResponseBuilder().merge(a).merge(b).build();
  }

  @Override
  protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
    return result -> {
      logger.info(buildLogInfo(result));
      return result;
    };
  }
}
