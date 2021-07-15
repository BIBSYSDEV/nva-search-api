package no.unit.nva.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UriRetrieverTest {

    @Test
    void retrivePublication() throws IOException, InterruptedException {
        final URI uri = URI.create("https://api.dev.nva.aws.unit.no/publication-channels/publisher/26778/2020");
        String publicationJson = UriRetriever.getRawContent(uri, "application/ld+json");
        assertNotNull(publicationJson);
        System.out.println(publicationJson);
    }

}
