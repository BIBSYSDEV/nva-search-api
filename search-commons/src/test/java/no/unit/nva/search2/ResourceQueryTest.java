package no.unit.nva.search2;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.model.ResourceParameterKey.CATEGORY;
import static no.unit.nva.search2.model.ResourceParameterKey.CREATED_BEFORE;
import static no.unit.nva.search2.model.ResourceParameterKey.DOI;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.MODIFIED_BEFORE;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.PUBLISHED_BEFORE;
import static no.unit.nva.search2.model.ResourceParameterKey.PUBLISHED_SINCE;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.model.OpenSearchQuery;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
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
        var uri2 =
            UriWrapper.fromUri(resourceParameters.getNvaSearchApiUri())
                .addQueryParameters(resourceParameters.toNvaSearchApiRequestParameter()).getUri();

        logger.info(resourceParameters
            .toNvaSearchApiRequestParameter()
            .entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&")));
        logger.info(uri2.toString());
        assertNotEquals(uri, resourceParameters.getNvaSearchApiUri());
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
            resourceParameters.getValue(MODIFIED_BEFORE).<DateTime>as();
        if (nonNull(modified)) {
            logger.info("modified: {}", modified);
        }

        var publishedBefore =
            resourceParameters.isPresent(PUBLISHED_BEFORE)
                ? resourceParameters.getValue(PUBLISHED_BEFORE).<DateTime>as()
                : null;
        if (nonNull(publishedBefore)) {
            logger.info("publishedBefore: {}", publishedBefore);
        }

        var publishedSince =
            resourceParameters.isPresent(PUBLISHED_SINCE)
                ? resourceParameters.getValue(PUBLISHED_SINCE).<DateTime>as()
                : null;
        if (nonNull(publishedSince)) {
            logger.info("publishedSince: {}", publishedSince);
        }

        var created =
            resourceParameters.getValue(CREATED_BEFORE).<DateTime>as();
        if (nonNull(created)) {
            logger.info("created: {}", created);
        }

        var category =
            resourceParameters.getValue(CATEGORY).<String>as();
        if (nonNull(category)) {
            logger.info("category: {}", category);
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
            URI.create("https://example.com/?query=Muhammad+Yahya&fields=CONTRIBUTOR"),
            URI.create("https://example.com/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254"),
            URI.create("https://example.com/?CONTRIBUTOR_SHOULD="
                + "https://api.dev.nva.aws.unit.no/cristin/person/1136254+"
                + "https://api.dev.nva.aws.unit.no/cristin/person/1135555"),
            URI.create("https://example.com/?CONTRIBUTOR_NOT="
                + "https://api.dev.nva.aws.unit.no/cristin/person/1136254+"
                + "https://api.dev.nva.aws.unit.no/cristin/person/1135555"),
            URI.create("https://example.com/?fields=all"),
            URI.create("https://example.com/?category=hello+world&page=1&user=12%203"),
            URI.create("https://example.com/?category=hello+world&sort=created_date&order=asc"),
            URI.create("https://example.com/?category=hello+world&sort=created_date:ASC"),
            URI.create("https://example.com/?category=hello+world&sort=created_date"),
            URI.create("https://example.com/?category=hello+world&user=12%203&page=2"),
            URI.create("https://example.com/?category=hello+world&user=12%203&offset=30"),
            URI.create("https://example.com/?category=hello+world&user=12%203&from=30&results=10"),
            URI.create("https://example.com/?published_before=2020-01-01&lang=en&user=1%2023"),
            URI.create("https://example.com/?published_since=2019-01-01&institution=uib&funding_source=NFR&user=Per"
                + "%20Eplekjekk"));
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
            URI.create("https://example.com/?published_before=2020-01-02&published_since=2019-01-02"),
            URI.create("https://example.com/?published_before=2020&published_since=2019"),
            URI.create("https://example.com/?created_before=2020-01-01T23:59:59&created_since=2019-01-01"));
    }

    static Stream<URI> invalidUriProvider() {
        return Stream.of(
            URI.create("https://example.com/?dcategory=hello+world&page=0"),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&user="),
            URI.create("https://example.com/?publishedbefore=202t0&lang=en&"),
            URI.create("https://example.com/?publishedbefore=2020&sort=category:BESC"),
            URI.create("https://example.com/?publishedbefore=2020&sort=category:BESC:AS"),
            URI.create("https://example.com/?institutions=uib&funding=NFR&lang=en"));
    }
}