package no.unit.nva.search2;

import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ResourceQueryTest {


    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void builder(URI uri) throws BadRequestException {
        var params = Arrays
                         .stream(uri.getQuery().split("&"))
                         .map(s -> s.split("="))
                         .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));
        var test =
            ResourceQuery.builder()
            .fromQueryParameters(params)
            .withRequiredParameters(ResourceParameter.PAGE, ResourceParameter.PER_PAGE)
            .build();
        var uri2 = test.toURI();
        System.out.println(test.toURI().toString());
        assertNotEquals(uri,uri2);

    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=hello+world&lang=en"),
            URI.create("https://example.com/?published_before=2020&lang=en"),
            URI.create("https://example.com/?institution=uib&funding=NFR&lang=en"));
    }
}