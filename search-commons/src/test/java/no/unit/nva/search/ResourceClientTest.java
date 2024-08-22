package no.unit.nva.search;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.common.Containers.container;
import static no.unit.nva.search.common.Containers.indexingClient;
import static no.unit.nva.search.common.Containers.loadMapFromResource;
import static no.unit.nva.search.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search.common.MockedHttpResponse.mockedFutureFailed;
import static no.unit.nva.search.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.search.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.search.common.constant.Words.ALL;
import static no.unit.nva.search.common.constant.Words.COLON;
import static no.unit.nva.search.common.constant.Words.COMMA;
import static no.unit.nva.search.common.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search.common.constant.Words.DOT;
import static no.unit.nva.search.common.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.common.constant.Words.EQUAL;
import static no.unit.nva.search.common.constant.Words.FILES;
import static no.unit.nva.search.common.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search.common.constant.Words.KEYWORD;
import static no.unit.nva.search.common.constant.Words.LICENSE;
import static no.unit.nva.search.common.constant.Words.NONE;
import static no.unit.nva.search.common.constant.Words.PAGES;
import static no.unit.nva.search.common.constant.Words.PIPE;
import static no.unit.nva.search.common.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search.common.constant.Words.PUBLISHER;
import static no.unit.nva.search.common.constant.Words.RESOURCES;
import static no.unit.nva.search.common.constant.Words.REFERENCE;
import static no.unit.nva.search.common.constant.Words.SLASH;
import static no.unit.nva.search.common.constant.Words.SPACE;
import static no.unit.nva.search.common.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search.common.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search.common.constant.Words.TYPE;
import static no.unit.nva.search.common.constant.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.DELETED;
import static no.unit.nva.search.common.enums.PublicationStatus.DRAFT;
import static no.unit.nva.search.common.enums.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.search.common.enums.PublicationStatus.NEW;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.NODES_EXCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.NODES_INCLUDED;
import static no.unit.nva.search.resource.ResourceParameter.PUBLICATION_BOOK_PAGES;
import static no.unit.nva.search.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_BEFORE;
import static no.unit.nva.search.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_SINCE;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;
import static no.unit.nva.search.resource.ResourceParameter.UNIT;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Words;
import no.unit.nva.search.common.csv.ResourceCsvTransformer;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.resource.UserSettingsClient;
import no.unit.nva.search.scroll.ScrollClient;
import no.unit.nva.search.scroll.ScrollQuery;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ResourceClientTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceClientTest.class);
    private static final String EMPTY_USER_RESPONSE_JSON = "user_settings_empty.json";
    private static final String RESOURCE_VALID_DEV_URLS_JSON = "resource_urls.json";
    public static final String REQUEST_BASE_URL = "https://x.org/?size=20&";
    public static final int EXPECTED_NUMBER_OF_AGGREGATIONS = 10;
    private static ScrollClient scrollClient;
    private static ResourceClient searchClient;

    @BeforeAll
    static void setUp() {
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, cachedJwtProvider);
        var response = mockedFutureHttpResponse(Path.of("user_settings.json"));
        when(mochedHttpClient.sendAsync(any(), any()))
            .thenReturn(response)
            .thenReturn(mockedFutureHttpResponse(""))
            .thenReturn(mockedFutureFailed());
        searchClient = new ResourceClient(HttpClient.newHttpClient(), cachedJwtProvider, userSettingsClient);
        scrollClient = new ScrollClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }


    @Test
    void shouldCheckMapping() {

        var mapping = indexingClient.getMapping(RESOURCES);
        assertThat(mapping, is(notNullValue()));
        var topLevelOrgType = mapping.path("properties")
            .path(TOP_LEVEL_ORGANIZATIONS)
            .path(TYPE).textValue();
        assertThat(topLevelOrgType, is(equalTo("nested")));
        logger.info(mapping.toString());
    }

    @Test
    void testingFromRequestInfoSuccessful() throws UnauthorizedException, BadRequestException {
        AtomicReference<URI> uri = new AtomicReference<>();
        uriSortingProvider().findFirst().ifPresent(uri::set);
        var mockedRequestInfoLocal = mock(RequestInfo.class);
        when(mockedRequestInfoLocal.getPersonAffiliation()).thenReturn(randomUri());
        var result = ResourceSearchQuery
            .builder()
            .fromQueryParameters(queryToMapEntries(uri.get()))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .fromRequestInfo(mockedRequestInfoLocal)
            .doSearch(searchClient);
        assertThat(result.toPagedResponse().hits().size(), is(0));
    }

    @Test
    void shouldCheckFacets() throws BadRequestException {
        var hostAddress = URI.create(container.getHttpHostAddress());
        var uri1 = URI.create(REQUEST_BASE_URL + AGGREGATION.asCamelCase() + EQUAL + ALL + "&query=EntityDescription");

        var response1 = ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri1))
            .withDockerHostUri(hostAddress)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
            .doSearch(searchClient);

        assertNotNull(response1);

        var aggregations = response1.toPagedResponse().aggregations();

        assertFalse(aggregations.isEmpty());
        assertThat(aggregations.get(TYPE).size(), is(5));
        assertThat(aggregations.get(FILES).get(0).count(), is(19));
        assertThat(aggregations.get(LICENSE).get(0).count(), is(11));
        assertThat(aggregations.get(FUNDING_SOURCE).size(), is(2));
        assertThat(aggregations.get(PUBLISHER).get(0).count(), is(3));
        assertThat(aggregations.get(CONTRIBUTOR).size(), is(13));
        assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION).size(), is(11));
        assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION).get(1).labels().get("nb"),
            is(equalTo("Sikt – Kunnskapssektorens tjenesteleverandør")));
    }


    @Test
    void userSettingsNotFoundReturn200() throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        var mockedResponse = mockedHttpResponse(EMPTY_USER_RESPONSE_JSON, 200);
        when(mochedHttpClient.send(any(), any()))
            .thenReturn(mockedResponse);
        var searchClient = new ResourceClient(HttpClient.newHttpClient(), setupMockedCachedJwtProvider(),
            userSettingsClient
        );

        var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response = ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
            .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void userSettingsNotFoundReturn404() throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        var mockedResponse = mockedHttpResponse(EMPTY_USER_RESPONSE_JSON, 404);
        when(mochedHttpClient.send(any(), any()))
            .thenReturn(mockedResponse);
        var searchClient = new ResourceClient(HttpClient.newHttpClient(), setupMockedCachedJwtProvider(),
            userSettingsClient
        );

        var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response = ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE, SORT)
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
            .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void userSettingsFailsIOException() throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        when(mochedHttpClient.send(any(), any()))
            .thenThrow(new IOException("Not found"));
        var searchClient =
            new ResourceClient(HttpClient.newHttpClient(), setupMockedCachedJwtProvider(), userSettingsClient);

        var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response = ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
            .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void userSettingsFailswithWrongFormat() throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        when(mochedHttpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse("user_settings_empty.json", 200));
        var searchClient =
            new ResourceClient(HttpClient.newHttpClient(), setupMockedCachedJwtProvider(), userSettingsClient);

        var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response = ResourceSearchQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
            .doSearch(searchClient);

        assertNotNull(response);
    }


    @Test
    void emptyResultShouldIncludeHits() throws BadRequestException {
        var uri = URI.create("https://x.org/?id=018b857b77b7&from=10");

        var pagedResult =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withFilter()
                .requiredStatus(NEW, DRAFT, PUBLISHED_METADATA, PUBLISHED, DELETED, UNPUBLISHED,
                    DRAFT_FOR_DELETION).apply()
                .doSearch(searchClient).toString();
        assertNotNull(pagedResult);
        assertTrue(pagedResult.contains("\"hits\":["));
    }

    @Test
    void searchAfterAndSortByRelevanceException() {
        var uri = URI.create("https://x.org/?id=018b857b77b7&from=10&searchAfter=12&sort=relevance");
        assertThrows(
            BadRequestException.class,
            () -> ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .doSearch(searchClient));
    }


    @ParameterizedTest
    @CsvSource({
        "FYS5960,1",
        "fys5960,1",
        "fys596,1",
        "fys5961,0"})
    void shouldReturnCaseInsensitiveCourses(String searchValue, int expectedHits) throws BadRequestException {
        var uri = URI.create("https://x.org/?course=" + searchValue);

        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withFilter()
                .requiredStatus(NEW, DRAFT, PUBLISHED_METADATA, PUBLISHED, DELETED, UNPUBLISHED,
                    DRAFT_FOR_DELETION).apply()
                .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertEquals(expectedHits, pagedSearchResourceDto.totalHits());
    }

    @Test
    void withOrganizationDoWork() throws BadRequestException, UnauthorizedException {
        var uri = URI.create("https://x.org/");
        var requestInfo = mock(RequestInfo.class);
        when(requestInfo.getCurrentCustomer())
            .thenReturn(URI.create("https://api.dev.nva.aws.unit.no/customer/bb3d0c0c-5065-4623-9b98-5810983c2478"));
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withParameter(NODES_EXCLUDED, "metaInfo")
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED_METADATA, PUBLISHED)
                .organization(requestInfo)
                .apply()
                .doSearch(searchClient);

        assertNotNull(response);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertEquals(3, pagedSearchResourceDto.totalHits());
    }

    @Test
    void scrollClientExceuteOK() throws BadRequestException {
        var includedNodes = String.join(COMMA, ResourceCsvTransformer.getJsonFields());
        var firstResponse = ResourceSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withParameter(FROM, ZERO)
            .withParameter(SIZE, "5")
            .withParameter(AGGREGATION, NONE)
            .withParameter(NODES_INCLUDED, includedNodes)
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED_METADATA, PUBLISHED).apply()
            .withScrollTime("1m")
            .doSearch(searchClient)
            .swsResponse();

        var response =
            ScrollQuery.builder()
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withInitialResponse(firstResponse)
                .withScrollTime("1m")
                .build()
                .doSearch(scrollClient)
                .toCsvText();
        assertNotNull(response);
        logger.info(response);

    }

    @ParameterizedTest
    @MethodSource("uriPagingProvider")
    void searchWithUriPageableReturnsOpenSearchResponse(URI uri, int expectedCount) throws ApiGatewayException {

        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .validate()
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        assertNotNull(pagedSearchResourceDto);
        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
        assertThat(pagedSearchResourceDto.aggregations().size(),
            is(equalTo(EXPECTED_NUMBER_OF_AGGREGATIONS)));
        logger.debug(pagedSearchResourceDto.id().toString());
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri, int expectedCount) throws ApiGatewayException {

        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .doSearch(searchClient);


        var pagedSearchResourceDto = response.toPagedResponse();

        assertNotNull(pagedSearchResourceDto);
        if (expectedCount == 0) {
            logger.info(pagedSearchResourceDto.toJsonString());
        } else {
            logger.debug(pagedSearchResourceDto.toString());
        }

        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedCount)));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
        var csvResult =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withMediaType(Words.TEXT_CSV)
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED_METADATA).apply()
                .doSearch(searchClient).toString();
        assertNotNull(csvResult);
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .doSearch(searchClient);


        var pagedSearchResourceDto = response.toPagedResponse();
        assertNotNull(pagedSearchResourceDto.id());
        var searchName = response.parameters().get(SORT).split(COMMA)[0].split(COLON)[0];
        var searchFieldName = ResourceSort.fromSortKey(searchName)
            .jsonPaths()
            .findFirst()
            .map(path -> path.contains(KEYWORD) ? path.substring(0, path.indexOf(KEYWORD) - 1) : path)
            .map(path -> SLASH + path.replace(DOT, SLASH))
            .orElseThrow();


        var logInfo = response.swsResponse().hits().hits().stream()
            .map(item -> item._score() + " + " + searchFieldName)
            .collect(Collectors.joining(SPACE + PIPE + SPACE));
        logger.info(logInfo);
        assertNotNull(pagedSearchResourceDto.context());
        assertTrue(pagedSearchResourceDto.totalHits() >= 0);
    }

    @ParameterizedTest
    @MethodSource("uriInvalidProvider")
    void failToSearchUri(URI uri) {
        assertThrows(
            BadRequestException.class,
            () -> ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .doSearch(searchClient));
    }

    @Test
    void shouldReturnResourcesForScientificPeriods() throws BadRequestException {
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(Map.of(SCIENTIFIC_REPORT_PERIOD_SINCE.asCamelCase(), "2019",
                    SCIENTIFIC_REPORT_PERIOD_BEFORE.asCamelCase(), "2022"))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertThat(pagedSearchResourceDto.hits(), hasSize(2));
    }

    @Test
    void shouldReturnResourcesForSinglePeriods() throws BadRequestException {
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(Map.of(SCIENTIFIC_REPORT_PERIOD_SINCE.asCamelCase(), "2019",
                    SCIENTIFIC_REPORT_PERIOD_BEFORE.asCamelCase(), "2020"))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .doSearch(searchClient);
        var pagedSearchResourceDto = response.toPagedResponse();
        assertThat(pagedSearchResourceDto.hits(), hasSize(1));
    }


    @Test
    void shouldNotReturnResourcesContainingAffiliationThatShouldBeExcludedWhenIsSubunitOfRequestedViewingScopeI()
        throws BadRequestException {
        var viewingScope = URLEncoder.encode("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0",
            StandardCharsets.UTF_8);
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(Map.of(UNIT.asCamelCase(), viewingScope,
                    EXCLUDE_SUBUNITS.asCamelCase(), Boolean.TRUE.toString()))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply()
                .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        var excludedSubunit = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.1.0";

        assertThat(pagedSearchResourceDto.toJsonString(), not(containsString(excludedSubunit)));
        assertThat(pagedSearchResourceDto.hits(), hasSize(1));
    }


    @Test
    void shouldReturnResourcesWithSubunitsWhenExcludedSubunitsNotProvided() throws BadRequestException {
        var unit = URLEncoder.encode("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0",
            StandardCharsets.UTF_8);
        var topLevelOrg = URLEncoder.encode("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0",
            StandardCharsets.UTF_8);
        var response =
            ResourceSearchQuery.builder()
                .fromQueryParameters(Map.of(UNIT.asCamelCase(), unit, TOP_LEVEL_ORGANIZATION, topLevelOrg))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA, DELETED).apply()
                .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        var includedSubunitI = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.1.0";
        var includedSubunitII = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.1.1";

        assertThat(pagedSearchResourceDto.toJsonString(), containsString(includedSubunitI));
        assertThat(pagedSearchResourceDto.toJsonString(), containsString(includedSubunitII));
        assertThat(pagedSearchResourceDto.hits(), hasSize(3));
    }


    @ParameterizedTest
    @MethodSource("provideValidPageRanges")
    void shouldFilterByPageCount(int min, int max, int expectedResultCount) throws BadRequestException {
        var pageRange = String.format("%d,%d", min, max);
        var response = ResourceSearchQuery.builder()
                .fromQueryParameters(Map.of(PUBLICATION_BOOK_PAGES.asCamelCase(), pageRange))
                .withRequiredParameters(FROM, SIZE)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .apply()
                .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        var pageCounts = pagedSearchResourceDto.hits().stream()
                .map(hit -> hit.get(ENTITY_DESCRIPTION).get(REFERENCE).get(PUBLICATION_INSTANCE).get(PAGES).get(PAGES).asInt())
                .collect(Collectors.toList());

        assertThat("Number of hits", pagedSearchResourceDto.hits(), hasSize(expectedResultCount));
        assertThat("All page counts are within the specified range",
                pageCounts, everyItem(allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max))));
    }

    static Stream<Arguments> uriPagingProvider() {
        return Stream.of(
            createArgument("page=0&aggregation=all", 20),
            createArgument("page=1&size=10&aggregation=all&sort=modifiedDate:asc", 10),
            createArgument("page=3&aggregation=all&sort=modifiedDate:asc", 0),
            createArgument("page=1&aggregation=all&size=1", 1),
            createArgument("page=2&aggregation=all&size=1", 1),
            createArgument("page=3&aggregation=all&size=1", 1),
            createArgument("page=0&aggregation=all&size=0", 0),
            createArgument("offset=15&aggregation=all&size=2", 2),
            createArgument("offset=15&aggregation=all&limit=2", 2),
            createArgument("offset=15&aggregation=all&results=2", 2),
            createArgument("offset=15&aggregation=all&per_page=2", 2),
            createArgument("OFFSET=15&aggregation=all&PER_PAGE=2", 2),
            createArgument("offset=15&aggregation=all&perPage=2", 2)
        );
    }

    static Stream<URI> uriSortingProvider() {

        return Stream.of(
            URI.create(REQUEST_BASE_URL + "status=PUBLISHED&sort=relevance,createdDate"),
            URI.create(REQUEST_BASE_URL + "query=year+project&sort=RELEVANCE,modifiedDate"),
            URI.create(REQUEST_BASE_URL + "status=PUBLISHED&sort=unitId"),
            URI.create(REQUEST_BASE_URL + "query=PublishedFile&sort=unitId"),
            URI.create(REQUEST_BASE_URL + "query=research&orderBy=UNIT_ID:asc,title:desc"),
            URI.create(REQUEST_BASE_URL
                + "query=year+project,PublishedFile&sort=created_date&sortOrder=asc&sort=category&order=desc"),
            URI.create(REQUEST_BASE_URL + "query=project,PublishedFile&sort=modified_date&sortOrder=asc&sort=category"),
            URI.create(REQUEST_BASE_URL + "query=PublishedFile&sort=published_date&sortOrder=asc&sort=category"),
            URI.create(REQUEST_BASE_URL + "query=PublishedFile&sort=published_date:desc"),
            URI.create(REQUEST_BASE_URL + "query=PublishedFile&size=10&from=0&sort=modified_date"),
            URI.create(REQUEST_BASE_URL + "query=infrastructure&sort=instanceType"),
            URI.create(REQUEST_BASE_URL + "status=PUBLISHED&sort=createdDate"),
            URI.create(REQUEST_BASE_URL + "status=PUBLISHED&sort=modifiedDate"),
            URI.create(REQUEST_BASE_URL + "status=PUBLISHED&sort=publishedDate"),
            URI.create(REQUEST_BASE_URL + "status=PUBLISHED&sort=publicationDate"),
            URI.create(REQUEST_BASE_URL + "query=PublishedFile&sort=title"),
            URI.create(REQUEST_BASE_URL + "query=PublishedFile&sort=user"),
            URI.create(REQUEST_BASE_URL
                + "query=year+project&orderBy=created_date:asc,modifiedDate:desc"),
            URI.create(REQUEST_BASE_URL
                + "query=year+project&orderBy=RELEVANCE,created_date:asc,modifiedDate:desc&searchAfter=3.4478912,1241234,23412"),
            URI.create(REQUEST_BASE_URL + "query=year+project&sort=published_date+asc&sort=category+desc"));
    }

    static Stream<URI> uriInvalidProvider() {
        return Stream.of(
            URI.create(REQUEST_BASE_URL + "sort=epler"),
            URI.create(REQUEST_BASE_URL + "sort=CATEGORY:DEdd"),
            URI.create(REQUEST_BASE_URL + "sort=CATEGORY:desc:asc"),
            URI.create(REQUEST_BASE_URL + "categories=hello+world&lang=en"),
            URI.create(REQUEST_BASE_URL + "tittles=hello+world&modified_before=2019-01-01"),
            URI.create(REQUEST_BASE_URL + "conttributors=hello+world&published_before=2020-01-01"),
            URI.create(REQUEST_BASE_URL + "category=PhdThesis&sort=beunited+asc"),
            URI.create(REQUEST_BASE_URL + "funding=NFR,296896"),
            URI.create(REQUEST_BASE_URL + "useers=hello+world&lang=en"));
    }

    /**
     * Provides a stream of valid page ranges for parameterized tests.
     * Each argument consists of a minimum and maximum page count, and the expected number of results.
     *
     * @return a stream of arguments where each argument is a tuple of (min, max, expectedResultCount)
     */
    private static Stream<Arguments> provideValidPageRanges() {
        return Stream.of(
                Arguments.of(1, 100, 8),
                Arguments.of(37, 39, 1),
                Arguments.of(38, 38, 1),
                Arguments.of(17, 20, 3)
        );
    }

    static Stream<Arguments> uriProvider() {
        return loadMapFromResource(RESOURCE_VALID_DEV_URLS_JSON).entrySet().stream()
            .map(entry -> createArgument(entry.getKey(), (Integer) entry.getValue()));
    }


    private static Arguments createArgument(String searchUri, int expectedCount) {
        return Arguments.of(URI.create(REQUEST_BASE_URL + searchUri), expectedCount);
    }

}