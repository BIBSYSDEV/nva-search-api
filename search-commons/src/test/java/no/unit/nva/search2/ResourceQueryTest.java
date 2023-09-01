package no.unit.nva.search2;

import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.PER_PAGE;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceQueryTest {


    @ParameterizedTest
    @MethodSource("uriProvider")
    void builder(URI uri) throws BadRequestException {
        var params = queryToMap(uri);
        var test =
            ResourceQuery.builder()
            .fromQueryParameters(params)
            .withRequiredParameters(PAGE, PER_PAGE)
            .build();
        var uri2 = test.toURI();
        assertNotEquals(uri,uri2);
    }

    @NotNull
    private static Map<String, String> queryToMap(URI uri) {
        return Arrays
                   .stream(uri.getQuery().split("&"))
                   .map(s -> s.split("="))
                   .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=hello+world&page=0"),
            URI.create("https://example.com/?published_before=2020&lang=en"),
            URI.create("https://example.com/?institution=uib&funding=NFR&lang=en"));
    }
}