package no.unit.nva.search2.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

@SuppressWarnings("PMD.ShortMethodName")
public record TestGatewayResponse<T>(
    T body,
    int statusCode,
    Map<String, String> headers) {

    public static TestGatewayResponse<OpenSearchSwsResponse> ofSwsGatewayResponse(InputStream inputStream)
        throws IOException {
        var typeReference = new TypeReference<TestGatewayResponse<OpenSearchSwsResponse>>() {
        };
        return dtoObjectMapper.readValue(inputStream, typeReference);
    }

    public static TestGatewayResponse<ResourcePagedSearchResponseDto> of(OutputStream outputStream) throws IOException {
        var response = ofString(outputStream);
        var typeReference2 = new TypeReference<ResourcePagedSearchResponseDto>() {
        };
        var body = dtoObjectMapper.readValue(response.body(), typeReference2);
        return new TestGatewayResponse<>(
            body,
            response.statusCode(),
            response.headers());
    }

    private static TestGatewayResponse<String> ofString(OutputStream outputStream) throws JsonProcessingException {
        var typeReference = new TypeReference<TestGatewayResponse<String>>() {
        };
        return dtoObjectMapper.readValue(outputStream.toString(), typeReference);
    }
}
