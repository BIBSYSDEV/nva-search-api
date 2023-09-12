package no.unit.nva.search2;

import java.util.stream.Collectors;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static no.unit.nva.search2.ResourceParameterKey.OFFSET;
import static no.unit.nva.search2.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.ResourceParameterKey.PER_PAGE;
import static no.unit.nva.search2.common.OpenSearchQuery.queryToMap;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceOpenSearchQueryTest {


    @ParameterizedTest
    @MethodSource("uriProvider")
    void buildOpenSearchSwsUriFromGatewayUri(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceOpenSearchQuery.Builder
                .queryBuilder()
                .fromQueryParameters(queryToMap(uri))
                .withRequiredParameters(PAGE, PER_PAGE)
                .build();
        assertNotNull(resourceParameters.getValue(OFFSET));
        assertNotNull(resourceParameters.getValue(ResourceParameterKey.USER));
        System.out.println(resourceParameters
                               .toGateWayRequestParameter()
                               .entrySet().stream()
                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                               .collect(Collectors.joining(" & ")));
        assertNotEquals(uri, resourceParameters.openSearchUri());
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void failToBuildOpenSearchSwsUriFromMissingRequired(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceOpenSearchQuery.Builder
                               .queryBuilder()
                               .fromQueryParameters(queryToMap(uri))
                               .withRequiredParameters(PAGE, PER_PAGE, ResourceParameterKey.DOI)
                               .build()
                               .openSearchUri());
    }

    @ParameterizedTest
    @MethodSource("invalidUriProvider")
    void failToBuildOpenSearchSwsUriFromInvalidGatewayUri(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceOpenSearchQuery.Builder
                               .queryBuilder()
                               .fromQueryParameters(queryToMap(uri))
                               .withRequiredParameters(OFFSET, PER_PAGE)
                               .build()
                               .openSearchUri());
    }


    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=hello+world&page=1&user=12%203"),
            URI.create("https://example.com/?category=hello+world&user=12%203&page=2"),
            URI.create("https://example.com/?category=hello+world&user=12%203&offset=30"),
            URI.create("https://example.com/?category=hello+world&user=12%203&from=30&results=10"),
            URI.create("https://example.com/?published_before=2020&lang=en&user=1%2023"),
            URI.create("https://example.com/?published_since=2019&institution=uib&funding=NFR&user=Per%20Eplekjekk"));
    }


    static Stream<URI> invalidUriProvider() {
        return Stream.of(
            URI.create("https://example.com/?dcategory=hello+world&page=0"),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&user="),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&"),
            URI.create("https://example.com/?institutions=uib&funding=NFR&lang=en"));
    }
}