package no.unit.nva.search.importcandidate;

import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.common.jwt.Tools.getCachedJwtProvider;

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
 * ImportCandidateClient is a class that sends a request to the search index.
 *
 * @author Stig Norland
 */
public class ImportCandidateClient
    extends OpenSearchClient<SwsResponse, ImportCandidateSearchQuery> {

  public ImportCandidateClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
    super(client, cachedJwtProvider);
  }

  @JacocoGenerated
  public static ImportCandidateClient defaultClient() {
    var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
    return new ImportCandidateClient(HttpClient.newHttpClient(), cachedJwtProvider);
  }

  @Override
  protected SwsResponse jsonToResponse(HttpResponse<String> response)
      throws JsonProcessingException {
    return singleLineObjectMapper.readValue(response.body(), SwsResponse.class);
  }

  @Override
  protected BinaryOperator<SwsResponse> responseAccumulator() {
    return (a, b) -> SwsResponse.SwsResponseBuilder.swsResponseBuilder().merge(a).merge(b).build();
  }

  @Override
  protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
    return result -> {
      logger.info(buildLogInfo(result));
      return result;
    };
  }
}
