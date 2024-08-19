package no.unit.nva.search;

import static java.util.Objects.nonNull;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.common.Containers.container;
import static no.unit.nva.search.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.search.resource.ResourceParameter.ABSTRACT;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.DOI;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.FUNDING;
import static no.unit.nva.search.resource.ResourceParameter.MODIFIED_BEFORE;
import static no.unit.nva.search.resource.ResourceParameter.PAGE;
import static no.unit.nva.search.resource.ResourceParameter.PUBLISHED_BEFORE;
import static no.unit.nva.search.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_BEFORE;
import static no.unit.nva.search.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_SINCE;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.unit.nva.search.resource.ResourceSearchQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.unit.nva.search.common.records.PagedSearch;
import no.unit.nva.search.resource.ResourceClient;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResourceSearchQueryTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceSearchQueryTest.class);

    @Test
    void emptyPagesearch() {
        var page = new PagedSearch(null, 0, null, null, null, null, null);
        assertEquals(page.aggregations(), Map.of());
    }

    @Test
    void removeKeySuccessfully() throws BadRequestException {
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(Map.of(SCIENTIFIC_REPORT_PERIOD_SINCE.asCamelCase(), "2019",
                    SCIENTIFIC_REPORT_PERIOD_BEFORE.asCamelCase(), "2020"))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build();

        var key = response.parameters().remove(SCIENTIFIC_REPORT_PERIOD_SINCE);

        assertEquals(SCIENTIFIC_REPORT_PERIOD_SINCE, key.getKey());
    }

    @Test
    void openSearchFailedResponse()  {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.sendAsync(any(), any()))
            .thenReturn(mockedFutureHttpResponse((String)null));
        var toMapEntries = queryToMapEntries(URI.create("https://example.com/?size=2"));
        var resourceClient = new ResourceClient(httpClient, setupMockedCachedJwtProvider());
        assertThrows(
            RuntimeException.class,
            () -> ResourceSearchQuery.builder()
                .withRequiredParameters(SIZE, FROM)
                .fromQueryParameters(toMapEntries)
                .build()
                .doSearch(resourceClient)
        );
    }

    @Test
    void missingRequiredException() {
        var toMapEntries =
            queryToMapEntries(URI.create("https://example.com/?doi=2&Title=wqerasdfg"));
        assertThrows(
            BadRequestException.class,
            () -> ResourceSearchQuery.builder()
                .withRequiredParameters(ABSTRACT, FUNDING)
                .fromQueryParameters(toMapEntries)
                .validate()
                .build()
        );
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void buildOpenSearchSwsUriFromGatewayUri(URI uri) throws BadRequestException {
        var resource =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .build();
        assertNotNull(resource.parameters().get(FROM).as());
        assertNotNull(resource.parameters().get(SIZE).as());
        var uri2 =
            UriWrapper.fromUri(resource.getNvaSearchApiUri())
                .addQueryParameters(resource.parameters().asMap()).getUri();

        logger.info(
            resource.parameters().asMap()
                .entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&")));
        logger.info(uri2.toString());
        assertNotEquals(uri, resource.getNvaSearchApiUri());
    }

    @ParameterizedTest
    @MethodSource("uriDatesProvider")
    void uriParamsDateToResourceParams(URI uri) throws BadRequestException {
        var query =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .build();

        query.parameters().getSearchKeys()
            .forEach(key -> logger.info("{} : {}", key.asCamelCase(), query.parameters().get(key).as()));

        // two ways to access keys

        var modified = query.parameters().get(MODIFIED_BEFORE);
        if (!modified.isEmpty()) {
            logger.info("Modified-1: {}", modified.asDateTime());
        }
        var published = query.parameters().ifPresent(PUBLISHED_BEFORE);
        if (nonNull(published)) {
            logger.info("Published-1: {}", published.<DateTime>as());
        }

    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriParamsToResourceParams(URI uri) throws BadRequestException {
        var resource = ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withRequiredParameters(FROM, SIZE)
            .build();
        assertNotNull(resource.parameters().get(FROM).<Long>as());
        assertNull(resource.parameters().get(PAGE).<Long>as());
        assertNotNull(resource.parameters().get(SORT).as());
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void failToBuildOpenSearchSwsUriFromMissingRequired(URI uri) {
        assertThrows(BadRequestException.class, () -> ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withRequiredParameters(FROM, SIZE, DOI)
            .validate()
            .build()
            .openSearchUri());
    }

    @ParameterizedTest
    @MethodSource("invalidUriProvider")
    void failToBuildOpenSearchSwsUriFromInvalidGatewayUri(URI uri) {
        assertThrows(BadRequestException.class, () -> ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
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
            URI.create("https://example.com/?CONTRIBUTOR_NOT="
                + "https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                + "https://api.dev.nva.aws.unit.no/cristin/person/1135555"),
            URI.create("https://example.com/?fields=all"),
            URI.create("https://example.com/?category=hello+world&page=1&user=12%203"),
            URI.create("https://example.com/?category=hello+world&sort=created_date&order=asc"),
            URI.create("https://example.com/?category=hello+world&sort=created_date:ASC"),
            URI.create("https://example.com/?category=hello+world&sort=created_date"),
            URI.create("https://example.com/?category=hello+world&user=12%203&page=2"),
            URI.create("https://example.com/?category=hello+world&user=12%203&offset=30"),
            URI.create("https://example.com/?category=hello+world&user=12%203&from=30&results=10"),
            URI.create(
                "https://example.com/?PARENT_PUBLICATION=https://api.dev.nva.aws.unit"
                    + ".no/publication/018b80c90f4a-75942f6d-544e-4d5b-8129-7b81b957678c"),
            URI.create("https://example.com/?published_before=2020-01-01&user=1%2023"),
            URI.create("https://example.com/?published_since=2019-01-01&institution=uib&funding_source=NFR&user=Per"
                + "%20Eplekjekk"));
    }

    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?sort=category&sortOrder=asc&sort=created_date&order=desc"),
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