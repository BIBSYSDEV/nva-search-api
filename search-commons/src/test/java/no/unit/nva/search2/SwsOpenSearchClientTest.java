package no.unit.nva.search2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class SwsOpenSearchClientTest {

    private SwsOpenSearchClient swsOpenSearchClient;

    @BeforeAll
    void setUp() {
        swsOpenSearchClient = SwsOpenSearchClient.defaultSwsClient();
    }

    @Test
    @ParameterizedTest
    void doSearch(URI uri) {
        var response = swsOpenSearchClient.doSearch(uri);
        assertNotNull(response);
    }
}