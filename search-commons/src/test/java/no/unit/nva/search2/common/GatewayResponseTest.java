package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;

import no.unit.nva.search2.model.TestGatewayResponse;
import org.junit.jupiter.api.Test;

class GatewayResponseTest {

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException {
        var response =
            TestGatewayResponse.ofSwsGatewayResponse(
                getClass().getResourceAsStream("/sample_gateway_opensearch_response.json"));
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        assertNotNull(response.headers());
    }


    @Test
    void shouldReturnSearchResultsWhenQu22eryIsSingleTerm() throws IOException {
        var response =
            TestGatewayResponse.ofSwsGatewayResponse(
                getClass().getResourceAsStream("/sample_invalid_gateway_opensearch_response.json"));
        assertNotNull(response);
        assertEquals(400, response.statusCode());
        assertNotNull(response.body());
        assertNotNull(response.headers());
    }

}