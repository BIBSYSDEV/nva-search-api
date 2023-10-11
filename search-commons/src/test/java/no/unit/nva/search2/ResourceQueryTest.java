package no.unit.nva.search2;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.model.ResourceParameterKey.CATEGORY;
import static no.unit.nva.search2.model.ResourceParameterKey.CREATED_BEFORE;
import static no.unit.nva.search2.model.ResourceParameterKey.DOI;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.MODIFIED_BEFORE;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.PUBLISHED_BEFORE;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.model.OpenSearchQuery;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.joda.time.DateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResourceQueryTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceQueryTest.class);

    @ParameterizedTest
    @MethodSource("uriProvider")
    void buildOpenSearchSwsUriFromGatewayUri(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceAwsQuery.builder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        assertNotNull(resourceParameters.getValue(FROM).as());
        assertNotNull(resourceParameters.getValue(SIZE).as());
        logger.info(resourceParameters
                        .toGateWayRequestParameter()
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(" & ")));
        assertNotEquals(uri, resourceParameters.openSearchUri());
    }


    @ParameterizedTest
    @MethodSource("uriDatesProvider")
    void uriParamsDateToResourceParams(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceAwsQuery.builder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        var modified =
            resourceParameters
                .getValue(MODIFIED_BEFORE)
                .<DateTime>as();
        var publishedBefore =
            resourceParameters
                .getValue(PUBLISHED_BEFORE)
                .<Integer>as();
        var created =
            resourceParameters
                .getValue(CREATED_BEFORE)
                .<DateTime>as();
        var category =
            resourceParameters
                .getValue(CATEGORY)
                .<String>as();

        if (nonNull(modified)) {
            logger.info("modified: {}", modified);
        } else if (nonNull(publishedBefore)) {
            logger.info("publishedBefore: {}", publishedBefore);
        } else if (nonNull(created)) {
            logger.info("created: {}", created);
        } else if (nonNull(category)) {
            logger.info("category: {}", category);
        } else {
            fail("No date found");
        }
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriParamsToResourceParams(URI uri) throws BadRequestException {
        var resourceParameters =
            ResourceAwsQuery.builder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        assertNotNull(resourceParameters.getValue(FROM).<Long>as());
        assertNull(resourceParameters.getValue(PAGE).<Long>as());
        assertNotNull(resourceParameters.getValue(SORT).as());

    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void failToBuildOpenSearchSwsUriFromMissingRequired(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceAwsQuery.builder()
                               .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                               .withRequiredParameters(FROM, SIZE, DOI)
                               .build()
                               .openSearchUri());
    }



    @ParameterizedTest
    @MethodSource("invalidUriProvider")
    void failToBuildOpenSearchSwsUriFromInvalidGatewayUri(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceAwsQuery.builder()
                               .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                               .withRequiredParameters(FROM, SIZE)
                               .build()
                               .openSearchUri());
    }


    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/"),
            URI.create("https://example.com/?fields=category,title,created_date"),
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
            URI.create("https://example.com/?sort=category&sortOrder=asc&sort=created_date&order=desc"),
            URI.create("https://example.com/"),
            URI.create("https://example.com/?orderBy=category:asc,created_date:desc"),
            URI.create("https://example.com/?sort=category+asc&sort=created_date+desc"));
    }

    static Stream<URI> uriDatesProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=hello&modified_before=2020-01-01&modified_since=2019-01-01"),
            URI.create("https://example.com/?published_before=2020&published_since=2019"),
            URI.create("https://example.com/?created_before=2020-01-01T23:59:59&created_since=2019-01-01"));
    }

    static Stream<URI> invalidUriProvider() {
        return Stream.of(
            URI.create("https://example.com/?dcategory=hello+world&page=0"),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&user="),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&"),
            URI.create("https://example.com/?institutions=uib&funding=NFR&lang=en"));
    }
}