package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class FakeGatewayResponseTest {

    @Test
    void shouldReturnGatewayResponseWithSwsResponse() throws IOException {
        var response =
            FakeGatewayResponse.ofSwsGatewayResponse(
                getClass().getResourceAsStream("/opensearch_valid_gateway_response.json"));
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        assertNotNull(response.headers());
    }


    @Test
    void shouldReturnGatewayResponseWithErrorCode() throws IOException {
        var response =
            FakeGatewayResponse.ofSwsGatewayResponse(
                getClass().getResourceAsStream("/opensearch_invalid_gateway_response.json"));
        assertNotNull(response);
        assertEquals(400, response.statusCode());
        assertNotNull(response.body());
        assertNotNull(response.headers());
    }

}