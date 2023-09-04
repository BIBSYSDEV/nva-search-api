package no.unit.nva.search2;

import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.PER_PAGE;
import static no.unit.nva.search2.common.OpenSearchQuery.queryToMap;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceQueryTest {


    @ParameterizedTest
    @MethodSource("uriProvider")
    void buildUriFromValidUri(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceQuery
                .builder()
                .fromQueryParameters(queryToMap(uri))
                .withRequiredParameters(PAGE, PER_PAGE)
                .build();
        System.out.println(resourceParameters.toString());
        assertNotEquals(uri, resourceParameters.toURI());
    }


    @ParameterizedTest
    @MethodSource("invalidUriProvider")
    void buildUriFromInvalidUri(URI uri) {

        assertThrows(BadRequestException.class,
                     () -> ResourceQuery
                               .builder()
                               .fromQueryParameters(queryToMap(uri))
                               .withRequiredParameters(PAGE, PER_PAGE)
                               .build()
                               .toURI());
    }


    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=hello+world&page=0"),
            URI.create("https://example.com/?published_before=2020&lang=en"),
            URI.create("https://example.com/?institution=uib&funding=NFR&lang=en"));
    }


    static Stream<URI> invalidUriProvider() {
        return Stream.of(
            URI.create("https://example.com/?dcategory=hello+world&page=0"),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en"),
            URI.create("https://example.com/?institutions=uib&funding=NFR&lang=en"));
    }
}