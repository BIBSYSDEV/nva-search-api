package no.unit.nva.search2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SwsOpenSearchClientTest {

    private SwsOpenSearchClient swsOpenSearchClient;

    @BeforeEach
    void setUp() {
        swsOpenSearchClient = SwsOpenSearchClient.defaultSwsClient();
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void testWithExplicitLocalMethodSource(URI uri) {
        var response = swsOpenSearchClient.doSearch(uri);
        assertNotNull(response);
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?name=hello+world&lang=en"),
            URI.create("https://example.com/?q=hello+world&lang=en"),
            URI.create("https://example.com/?q=hello+world&lang=en"));
    }
}