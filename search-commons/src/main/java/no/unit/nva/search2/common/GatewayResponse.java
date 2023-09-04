package no.unit.nva.search2.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

public record GatewayResponse<T>(
    T body,
    int statusCode,
    Map<String, String> headers) {

    public static <T> GatewayResponse<T> of(InputStream inputStream) throws IOException {
        var typeReference = new TypeReference<GatewayResponse<T>>() {
        };
        return dtoObjectMapper.readValue(inputStream, typeReference);
    }

//    public static GatewayResponse<SwsOpenSearchResponse> of(OutputStream outputStream) throws IOException {
//        var response = ofString(outputStream);
//        var typeReference2 = new TypeReference<SwsOpenSearchResponse>() {
//        };
//        var body = dtoObjectMapper.readValue(response.body(), typeReference2);
//        return new GatewayResponse<>(body, response.statusCode(), response.headers(), response.id());
//    }

    public static GatewayResponse<PagedSearchResponseDto> of(OutputStream outputStream) throws IOException {
        var response = ofString(outputStream);
        var typeReference2 = new TypeReference<PagedSearchResponseDto>() {
        };
        var body = dtoObjectMapper.readValue(response.body(), typeReference2);
        return new GatewayResponse<>(
            body,
            response.statusCode(),
            response.headers());
    }

    private static GatewayResponse<String> ofString(OutputStream outputStream) throws JsonProcessingException {
        var typeReference = new TypeReference<GatewayResponse<String>>() {
        };
        return dtoObjectMapper.readValue(outputStream.toString(), typeReference);
    }
}
