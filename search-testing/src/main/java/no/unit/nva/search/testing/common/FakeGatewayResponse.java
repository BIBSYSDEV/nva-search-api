package no.unit.nva.search.testing.common;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import no.unit.nva.search.common.records.PagedSearch;

@SuppressWarnings("PMD.ShortMethodName")
public record FakeGatewayResponse<T>(T body, int statusCode, Map<String, String> headers) {

  public static FakeGatewayResponse<PagedSearch> of(OutputStream outputStream) throws IOException {
    var response = ofString(outputStream);
    var typeReference2 = new TypeReference<PagedSearch>() {};
    var body = dtoObjectMapper.readValue(response.body(), typeReference2);
    return new FakeGatewayResponse<>(body, response.statusCode(), response.headers());
  }

  private static FakeGatewayResponse<String> ofString(OutputStream outputStream)
      throws JsonProcessingException {
    var typeReference = new TypeReference<FakeGatewayResponse<String>>() {};
    return dtoObjectMapper.readValue(outputStream.toString(), typeReference);
  }
}
