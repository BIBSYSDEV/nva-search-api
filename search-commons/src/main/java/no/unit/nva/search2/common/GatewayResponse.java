package no.unit.nva.search2.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

public record GatewayResponse<T>(
    T body,
    int statusCode,
    Map<String, String> headers,
    URI id) {

    public GatewayResponse(T body, int statusCode) {
        this(body, statusCode, Map.of(), null);
    }

    public static <T> GatewayResponse<T>of(InputStream inputStream) throws IOException {
        var typeReference = new TypeReference<GatewayResponse<T>>() {};
        return dtoObjectMapper.readValue(inputStream,typeReference);
    }

    public static GatewayResponse<SwsOpenSearchResponse>of(OutputStream outputStream) throws IOException {
        var response = ofString(outputStream);
        var typeReference2 = new TypeReference<SwsOpenSearchResponse>() {};
        var body = dtoObjectMapper.readValue(response.body(), typeReference2);
        return new GatewayResponse<>(body, response.statusCode(), response.headers(), response.id());
    }

    public static GatewayResponse<PagedSearchResponseDto>ofPageable(OutputStream outputStream) throws IOException {
        var response = of(outputStream);
        return new GatewayResponse<>(
            response.body.toPagedSearchResponseDto(response.id),
            response.statusCode(),
            response.headers(),
            response.id());
    }


    private static GatewayResponse<String>ofString(OutputStream outputStream) throws JsonProcessingException {
        var typeReference = new TypeReference<GatewayResponse<String>>() {};
        return dtoObjectMapper.readValue(outputStream.toString(),typeReference);
    }
}
