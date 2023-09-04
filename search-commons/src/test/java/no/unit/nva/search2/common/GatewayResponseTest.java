package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GatewayResponseTest {

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException {
        var response =
            GatewayResponse.of(
                getClass().getResourceAsStream("/sample_gateway_opensearch_response.json"));
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        assertNotNull(response.headers());
    }

//    @Test
//    void shouldReturnSearchResultsWhenQueryIsMultipleTerms() throws IOException {
//        GatewayResponse<PagedSearchResponseDto> response;
//        var path = Path.of("sample_gateway_opensearch_response.json");
//        try (var output = Files.newOutputStream(path)) {
//            response = GatewayResponse.of(output);
//        }
//        assertNotNull(response);
//        assertEquals(200, response.statusCode());
//        assertNotNull(response.body());
//        assertNotNull(response.headers());
//    }

}