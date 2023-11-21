package no.unit.nva.search.common;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import no.unit.nva.search2.model.opensearch.SwsResponse;
import no.unit.nva.search2.model.PagedSearchDto;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

@SuppressWarnings("PMD.ShortMethodName")
public record FakeGatewayResponse<T>(
    T body,
    int statusCode,
    Map<String, String> headers) {

    public static FakeGatewayResponse<SwsResponse> ofSwsGatewayResponse(InputStream inputStream)
        throws IOException {
        var typeReference = new TypeReference<FakeGatewayResponse<SwsResponse>>() {
        };
        return dtoObjectMapper.readValue(inputStream, typeReference);
    }

    public static FakeGatewayResponse<PagedSearchDto> of(OutputStream outputStream) throws IOException {
        var response = ofString(outputStream);
        var typeReference2 = new TypeReference<PagedSearchDto>() {
        };
        var body = dtoObjectMapper.readValue(response.body(), typeReference2);
        return new FakeGatewayResponse<>(
            body,
            response.statusCode(),
            response.headers());
    }

    private static FakeGatewayResponse<String> ofString(OutputStream outputStream) throws JsonProcessingException {
        var typeReference = new TypeReference<FakeGatewayResponse<String>>() {
        };
        return dtoObjectMapper.readValue(outputStream.toString(), typeReference);
    }
}
