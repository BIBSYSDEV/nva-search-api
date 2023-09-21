package no.unit.nva.search2;

import java.util.stream.Collectors;

import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.unit.nva.search2.model.ResourceParameterKey.DOI;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.model.OpenSearchQuery.queryToMap;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceQueryTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceQueryTest.class);

    @ParameterizedTest
    @MethodSource("uriProvider")
    void buildOpenSearchSwsUriFromGatewayUri(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceSwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(queryToMap(uri))
                .withRequiredParameters(FROM, SIZE)
                .build();
        assertNotNull(resourceParameters.getValue(FROM));
        assertNotNull(resourceParameters.getValue(SIZE));
        logger.info(resourceParameters
                        .toGateWayRequestParameter()
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(" & ")));
        assertNotEquals(uri, resourceParameters.openSearchUri());
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriParamsToResourceParams(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceSwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(queryToMap(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        assertNotNull(resourceParameters.getValue(FROM));
        assertNull(resourceParameters.getValue(PAGE));
        assertNotNull(resourceParameters.getValue(SORT));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void failToBuildOpenSearchSwsUriFromMissingRequired(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceSwsQuery.Builder
                               .queryBuilder()
                               .fromQueryParameters(queryToMap(uri))
                               .withRequiredParameters(FROM, SIZE, DOI)
                               .build()
                               .openSearchUri());
    }

    @ParameterizedTest
    @MethodSource("invalidUriProvider")
    void failToBuildOpenSearchSwsUriFromInvalidGatewayUri(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceSwsQuery.Builder
                               .queryBuilder()
                               .fromQueryParameters(queryToMap(uri))
                               .withRequiredParameters(FROM, SIZE)
                               .build()
                               .openSearchUri());
    }


    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/"),
            URI.create("https://example.com/?fields=value1,value2"),
            URI.create("https://example.com/?fields=all"),
            URI.create("https://example.com/?category=hello+world&page=1&user=12%203"),
            URI.create("https://example.com/?category=hello+world&user=12%203&page=2"),
            URI.create("https://example.com/?category=hello+world&user=12%203&offset=30"),
            URI.create("https://example.com/?category=hello+world&user=12%203&from=30&results=10"),
            URI.create("https://example.com/?published_before=2020&lang=en&user=1%2023"),
            URI.create("https://example.com/?published_since=2019&institution=uib&funding=NFR&user=Per%20Eplekjekk"));
    }

    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?sort=fieldName1&sortOrder=asc&sort=fieldName2&order=desc"),
            URI.create("https://example.com/"),
            URI.create("https://example.com/?orderBy=fieldName1:asc,fieldName2:desc"),
            URI.create("https://example.com/?sort=fieldName1+asc&sort=fieldName2+desc"));
    }


    static Stream<URI> invalidUriProvider() {
        return Stream.of(
            URI.create("https://example.com/?dcategory=hello+world&page=0"),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&user="),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&"),
            URI.create("https://example.com/?institutions=uib&funding=NFR&lang=en"));
    }
}