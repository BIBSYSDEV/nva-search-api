package nva.search.resource;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.loadMapFromResource;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.model.enums.PublicationStatus.DELETED;
import static no.unit.nva.search.model.enums.PublicationStatus.DRAFT;
import static no.unit.nva.search.model.enums.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.search.model.enums.PublicationStatus.NEW;
import static no.unit.nva.search.model.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.model.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.model.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search.service.resource.Constants.EXCLUDED_FIELDS;
import static no.unit.nva.search.service.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.service.resource.ResourceParameter.EXCLUDE_SUBUNITS;
import static no.unit.nva.search.service.resource.ResourceParameter.FROM;
import static no.unit.nva.search.service.resource.ResourceParameter.NODES_EXCLUDED;
import static no.unit.nva.search.service.resource.ResourceParameter.NODES_INCLUDED;
import static no.unit.nva.search.service.resource.ResourceParameter.PUBLICATION_PAGES;
import static no.unit.nva.search.service.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_BEFORE;
import static no.unit.nva.search.service.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_SINCE;
import static no.unit.nva.search.service.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.service.resource.ResourceParameter.SORT;
import static no.unit.nva.search.service.resource.ResourceParameter.STATISTICS;
import static no.unit.nva.search.service.resource.ResourceParameter.UNIT;

import static no.unit.nva.search.service.resource.ResourceSort.fromSortKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.model.IndexingClient;
import no.unit.nva.search.model.records.EventConsumptionAttributes;
import no.unit.nva.search.model.records.HttpResponseFormatter;
import no.unit.nva.search.model.records.IndexDocument;
import no.unit.nva.search.model.records.RestHighLevelClientWrapper;
import no.unit.nva.search.model.constant.Words;
import no.unit.nva.search.model.csv.ResourceCsvTransformer;
import no.unit.nva.search.model.jwt.CachedJwtProvider;

import no.unit.nva.common.EntrySetTools;
import no.unit.nva.common.MockedHttpResponse;
import no.unit.nva.search.service.resource.ResourceClient;
import no.unit.nva.search.service.resource.ResourceParameter;
import no.unit.nva.search.service.resource.ResourceSearchQuery;
import no.unit.nva.search.service.resource.UserSettingsClient;
import no.unit.nva.search.service.scroll.ScrollClient;
import no.unit.nva.search.service.scroll.ScrollQuery;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.paths.UriWrapper;

import org.apache.http.HttpHost;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.opensearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Testcontainers
class ResourceClientTest {

    public static final int EXPECTED_NUMBER_OF_AGGREGATIONS = 10;
    public static final String RESOURCE_VALID_DEV_URLS_JSON = "resource_urls.json";
    public static final String USER_SETTINGS_EMPTY_JSON = "user_settings_empty.json";
    public static final String USER_SETTINGS_JSON = "user_settings.json";
    public static final String BASE_URL = "https://x.org/?size=22&";
    public static final String PROPERTIES = "properties";
    public static final String NESTED = "nested";
    public static final String NOT_FOUND = "Not found";
    public static final String NUMBER_FIVE = "5";
    public static final String ONE_MINUTE = "1m";

    static final String Y2019 = "2019";
    static final String Y2022 = "2022";
    static final String Y2020 = "2020";
    private static final Logger logger = LoggerFactory.getLogger(ResourceClientTest.class);
    private static ScrollClient scrollClient;
    private static ResourceClient searchClient;
    private static IndexingClient indexingClient;

    @BeforeAll
    static void setUp() {
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, cachedJwtProvider);
        var response = MockedHttpResponse.mockedFutureHttpResponse(Path.of(USER_SETTINGS_JSON));
        when(mochedHttpClient.sendAsync(any(), any()))
                .thenReturn(response)
                .thenReturn(MockedHttpResponse.mockedFutureHttpResponse(""))
                .thenReturn(MockedHttpResponse.mockedFutureFailed());
        searchClient =
                new ResourceClient(
                        HttpClient.newHttpClient(), cachedJwtProvider, userSettingsClient);
        scrollClient = new ScrollClient(HttpClient.newHttpClient(), cachedJwtProvider);
        indexingClient = initiateIndexingClient(cachedJwtProvider);
    }

    private static IndexingClient initiateIndexingClient(CachedJwtProvider cachedJwtProvider) {
        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        return new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
    }

    static Stream<Arguments> uriPagingProvider() {
        return Stream.of(
                createArgument("page=0&aggregation=all", 22),
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
                createArgument("offset=15&aggregation=all&perPage=2", 2));
    }

    private static Arguments createArgument(String searchUri, int expectedCount) {
        return Arguments.of(URI.create(BASE_URL + searchUri), expectedCount);
    }

    static Stream<URI> uriInvalidProvider() {
        return Stream.of(
                URI.create(BASE_URL + "sort=epler"),
                URI.create(BASE_URL + "sort=CATEGORY:DEdd"),
                URI.create(BASE_URL + "sort=CATEGORY:desc:asc"),
                URI.create(BASE_URL + "categories=hello+world&lang=en"),
                URI.create(BASE_URL + "tittles=hello+world&modified_before=2019-01-01"),
                URI.create(BASE_URL + "conttributors=hello+world&published_before=2020-01-01"),
                URI.create(BASE_URL + "category=PhdThesis&sort=beunited+asc"),
                URI.create(BASE_URL + "funding=NFR,296896"),
                URI.create(BASE_URL + "useers=hello+world&lang=en"));
    }

    /**
     * Provides a stream of valid page ranges for parameterized tests. Each argument consists of a
     * minimum and maximum page count, and the expected number of results.
     *
     * @return a stream of arguments where each argument is a tuple of (min, max,
     *     expectedResultCount)
     */
    static Stream<Arguments> provideValidPageRanges() {
        return Stream.of(
                Arguments.of(1, 100, 8),
                Arguments.of(37, 39, 1),
                Arguments.of(38, 38, 1),
                Arguments.of(17, 20, 3));
    }

    static Stream<Arguments> uriProvider() {
        return loadMapFromResource(RESOURCE_VALID_DEV_URLS_JSON).entrySet().stream()
                .map(entry -> createArgument(entry.getKey(), (Integer) entry.getValue()));
    }

    static Stream<Arguments> roleProvider() {
        return Stream.of(
                Arguments.of(5, List.of(AccessRight.MANAGE_RESOURCES_ALL)),
                Arguments.of(22, List.of(AccessRight.MANAGE_CUSTOMERS)),
                Arguments.of(22, List.of(AccessRight.MANAGE_CUSTOMERS, AccessRight.MANAGE_RESOURCES_ALL)));
    }

    @Test
    void shouldCheckMapping() {

        var mapping = indexingClient.getMapping(Words.RESOURCES);
        assertThat(mapping, is(notNullValue()));
        var topLevelOrgType =
                mapping.path(PROPERTIES).path(Words.TOP_LEVEL_ORGANIZATIONS).path(Words.TYPE).textValue();
        assertThat(topLevelOrgType, is(equalTo(NESTED)));
        logger.info(mapping.toString());
    }

    @Test
    void testingFromRequestInfoSuccessful() throws UnauthorizedException, BadRequestException {
        AtomicReference<URI> uri = new AtomicReference<>();
        uriSortingProvider().findFirst().ifPresent(uri::set);

        var accessRights = List.of(AccessRight.MANAGE_CUSTOMERS);
        var mockedRequestInfoLocal = Mockito.mock(RequestInfo.class);
        when(mockedRequestInfoLocal.getPersonAffiliation())
                .thenReturn(
                        URI.create(
                                "https://api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0"));
        when(mockedRequestInfoLocal.getAccessRights()).thenReturn(accessRights);

        var result =
                ResourceSearchQuery.builder()
                        .fromRequestInfo(mockedRequestInfoLocal)
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri.get()))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .fromRequestInfo(mockedRequestInfoLocal)
                        .doSearch(searchClient);
        assertThat(result.toPagedResponse().hits().size(), is(2));
    }

    static Stream<URI> uriSortingProvider() {

        return Stream.of(
                URI.create(BASE_URL + "status=PUBLISHED&sort=relevance,createdDate"),
                URI.create(BASE_URL + "query=year+project&sort=RELEVANCE,modifiedDate"),
                URI.create(BASE_URL + "status=PUBLISHED&sort=unitId"),
                URI.create(BASE_URL + "query=PublishedFile&sort=unitId"),
                URI.create(BASE_URL + "query=research&orderBy=UNIT_ID:asc,title:desc"),
                URI.create(
                        BASE_URL
                                + "query=year+project,PublishedFile&sort=created_date&sortOrder=asc&sort=category&order=desc"),
                URI.create(
                        BASE_URL
                                + "query=project,PublishedFile&sort=modified_date&sortOrder=asc&sort=category"),
                URI.create(
                        BASE_URL
                                + "query=PublishedFile&sort=published_date&sortOrder=asc&sort=category"),
                URI.create(BASE_URL + "query=PublishedFile&sort=published_date:desc"),
                URI.create(BASE_URL + "query=PublishedFile&size=10&from=0&sort=modified_date"),
                URI.create(BASE_URL + "query=infrastructure&sort=instanceType"),
                URI.create(BASE_URL + "status=PUBLISHED&sort=createdDate"),
                URI.create(BASE_URL + "status=PUBLISHED&sort=modifiedDate"),
                URI.create(BASE_URL + "status=PUBLISHED&sort=publishedDate"),
                URI.create(BASE_URL + "status=PUBLISHED&sort=publicationDate"),
                URI.create(BASE_URL + "query=PublishedFile&sort=title"),
                URI.create(BASE_URL + "query=PublishedFile&sort=user"),
                URI.create(
                        BASE_URL + "query=year+project&orderBy=created_date:asc,modifiedDate:desc"),
                URI.create(
                        BASE_URL
                                + "query=year+project&orderBy=RELEVANCE,created_date:asc,modifiedDate:desc"
                                + "&searchAfter=3.4478912,1241234,23412"),
                URI.create(
                        BASE_URL
                                + "query=year+project&sort=published_date+asc&sort=category+desc"));
    }

    @Test
    void shouldCheckFacets() throws BadRequestException {
        var hostAddress = URI.create(container.getHttpHostAddress());
        var uri1 =
                URI.create(
                        BASE_URL
                                + AGGREGATION.asCamelCase()
                                + Words.EQUAL
                                + Words.ALL
                                + "&query=EntityDescription");

        var response1 =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri1))
                        .withDockerHostUri(hostAddress)
                        .withRequiredParameters(FROM, SIZE, AGGREGATION)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        assertNotNull(response1);

        var aggregations = response1.toPagedResponse().aggregations();

        assertFalse(aggregations.isEmpty());
        assertThat(aggregations.get(Words.TYPE).size(), is(6));
        assertThat(aggregations.get(Words.FILES).getFirst().count(), is(17));
        assertThat(aggregations.get(Words.LICENSE).getFirst().count(), is(10));
        assertThat(aggregations.get(Words.FUNDING_SOURCE).size(), is(1));
        assertThat(aggregations.get(Words.PUBLISHER).getFirst().count(), is(2));
        assertThat(aggregations.get(Words.CONTRIBUTOR).size(), is(17));
        assertThat(aggregations.get(Words.TOP_LEVEL_ORGANIZATION).size(), is(11));
        assertThat(
                aggregations.get(Words.TOP_LEVEL_ORGANIZATION).get(1).labels().get("nb"),
                is(equalTo("Sikt – Kunnskapssektorens tjenesteleverandør")));
    }

    @Test
    void userSettingsNotFoundReturn200()
            throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient =
                new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        var mockedResponse = MockedHttpResponse.mockedHttpResponse(USER_SETTINGS_EMPTY_JSON, 200);
        when(mochedHttpClient.send(any(), any())).thenReturn(mockedResponse);
        var searchClient =
                new ResourceClient(
                        HttpClient.newHttpClient(),
                        setupMockedCachedJwtProvider(),
                        userSettingsClient);

        var uri =
                URI.create(
                        "https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void userSettingsNotFoundReturn404()
            throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient =
                new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        var mockedResponse = MockedHttpResponse.mockedHttpResponse(USER_SETTINGS_EMPTY_JSON, 404);
        when(mochedHttpClient.send(any(), any())).thenReturn(mockedResponse);
        var searchClient =
                new ResourceClient(
                        HttpClient.newHttpClient(),
                        setupMockedCachedJwtProvider(),
                        userSettingsClient);

        var uri =
                URI.create(
                        "https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE, SORT)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void userSettingsFailsIOException()
            throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient =
                new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        when(mochedHttpClient.send(any(), any())).thenThrow(new IOException(NOT_FOUND));
        var searchClient =
                new ResourceClient(
                        HttpClient.newHttpClient(),
                        setupMockedCachedJwtProvider(),
                        userSettingsClient);

        var uri =
                URI.create(
                        "https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void userSettingsFailswithWrongFormat()
            throws IOException, InterruptedException, BadRequestException {
        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient =
                new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
        when(mochedHttpClient.send(any(), any()))
                .thenReturn(MockedHttpResponse.mockedHttpResponse(USER_SETTINGS_EMPTY_JSON, 200));
        var searchClient =
                new ResourceClient(
                        HttpClient.newHttpClient(),
                        setupMockedCachedJwtProvider(),
                        userSettingsClient);

        var uri =
                URI.create(
                        "https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        assertNotNull(response);
    }

    @Test
    void emptyResultShouldIncludeHits() throws BadRequestException {
        var uri = URI.create("https://x.org/?id=018b857b77b7&from=10");

        var pagedResult =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .requiredStatus(
                                NEW,
                                DRAFT,
                                PUBLISHED_METADATA,
                                PUBLISHED,
                                DELETED,
                                UNPUBLISHED,
                                DRAFT_FOR_DELETION)
                        .apply()
                        .doSearch(searchClient)
                        .toString();
        assertNotNull(pagedResult);
        assertTrue(pagedResult.contains("\"hits\":["));
    }

    @Test
    void searchAfterAndSortByRelevanceException() {
        var uri =
                URI.create("https://x.org/?id=018b857b77b7&from=10&searchAfter=12&sort=relevance");
        assertThrows(
                BadRequestException.class,
                () ->
                        ResourceSearchQuery.builder()
                                .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                                .withRequiredParameters(FROM, SIZE)
                                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                                .build()
                                .doSearch(searchClient));
    }

    @ParameterizedTest
    @CsvSource({"FYS5960,1", "fys5960,1", "fys596,1", "fys5961,0"})
    void shouldReturnCaseInsensitiveCourses(String searchValue, int expectedHits)
            throws BadRequestException {
        var uri = URI.create("https://x.org/?course=" + searchValue);

        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .requiredStatus(
                                NEW,
                                DRAFT,
                                PUBLISHED_METADATA,
                                PUBLISHED,
                                DELETED,
                                UNPUBLISHED,
                                DRAFT_FOR_DELETION)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertEquals(expectedHits, pagedSearchResourceDto.totalHits());
    }

    @ParameterizedTest
    @MethodSource("roleProvider")
    void isSearchingForAllPublicationsAsRoleWork(int expectedHits, List<AccessRight> accessRights)
            throws UnauthorizedException, BadRequestException {
        var requestInfo = Mockito.mock(RequestInfo.class);
        when(requestInfo.getAccessRights()).thenReturn(accessRights);
        when(requestInfo.getPersonAffiliation())
                .thenReturn(
                        URI.create(
                                "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0"));

        var response =
                ResourceSearchQuery.builder()
                        .fromRequestInfo(requestInfo)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withParameter(NODES_EXCLUDED, Words.META_INFO)
                        .withParameter(STATISTICS, Boolean.TRUE.toString())
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .fromRequestInfo(requestInfo)
                        .doSearch(searchClient);

        assertNotNull(response);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertEquals(expectedHits, pagedSearchResourceDto.totalHits());
    }

    @Test
    void isSearchingWithWrongAccessFails() throws UnauthorizedException {
        var requestInfo = Mockito.mock(RequestInfo.class);
        when(requestInfo.getAccessRights()).thenReturn(List.of(AccessRight.MANAGE_DEGREE));
        when(requestInfo.getPersonAffiliation())
                .thenReturn(URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/"));

        assertThrows(
                UnauthorizedException.class,
                () ->
                        ResourceSearchQuery.builder()
                                .fromRequestInfo(requestInfo)
                                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                                .withParameter(NODES_EXCLUDED, Words.META_INFO)
                                .withParameter(STATISTICS, Boolean.TRUE.toString())
                                .withRequiredParameters(FROM, SIZE)
                                .build()
                                .withFilter()
                                .fromRequestInfo(requestInfo)
                                .doSearch(searchClient));
    }

    @Test
    void withOrganizationDoWork() throws BadRequestException, UnauthorizedException {
        var requestInfo = Mockito.mock(RequestInfo.class);
        when(requestInfo.getAccessRights()).thenReturn(List.of(AccessRight.MANAGE_CUSTOMERS));
        when(requestInfo.getTopLevelOrgCristinId())
                .thenReturn(
                        Optional.of(
                                URI.create(
                                        "https://api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0")));
        var response =
                ResourceSearchQuery.builder()
                        .fromRequestInfo(requestInfo)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withParameter(NODES_EXCLUDED, Words.META_INFO)
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .fromRequestInfo(requestInfo)
                        .doSearch(searchClient);

        assertNotNull(response);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertEquals(3, pagedSearchResourceDto.totalHits());
    }

    @Test
    void scrollClientExceuteOK() throws BadRequestException {
        var includedNodes = String.join(Words.COMMA, ResourceCsvTransformer.getJsonFields());
        var firstResponse =
                ResourceSearchQuery.builder()
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withParameter(FROM, Words.ZERO)
                        .withParameter(SIZE, NUMBER_FIVE)
                        .withParameter(AGGREGATION, Words.NONE)
                        .withParameter(NODES_INCLUDED, includedNodes)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED_METADATA, PUBLISHED)
                        .apply()
                        .withScrollTime(ONE_MINUTE)
                        .doSearch(searchClient)
                        .swsResponse();

        var response =
                ScrollQuery.builder()
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withInitialResponse(firstResponse)
                        .withScrollTime(ONE_MINUTE)
                        .build()
                        .doSearch(scrollClient)
                        .toCsvText();
        assertNotNull(response);
    }

    @ParameterizedTest
    @MethodSource("uriPagingProvider")
    void searchWithUriPageableReturnsOpenSearchResponse(URI uri, int expectedCount)
            throws ApiGatewayException {
        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .validate()
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        assertNotNull(pagedSearchResourceDto);
        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
        assertThat(
                pagedSearchResourceDto.aggregations().size(),
                is(equalTo(EXPECTED_NUMBER_OF_AGGREGATIONS)));
        logger.debug(pagedSearchResourceDto.id().toString());
    }

    // TODO: Remove duplicate test?
    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri, int expectedCount)
            throws ApiGatewayException {

        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        assertNotNull(pagedSearchResourceDto);
        if (expectedCount == 0) {
            logger.debug(pagedSearchResourceDto.toJsonString());
        } else {
            logger.debug(pagedSearchResourceDto.toString());
        }

        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedCount)));
    }

    @Test
    void shouldFindAnthologyWithChapters() throws ApiGatewayException {
        // Given that a parent document exists of type BookAnthology,
        // and the parent document has two children of type AcademicChapter,
        // when a query filters by hasParts of type AcademicChapter,
        // then one document should be returned,
        // and the returned document should be of type BookAnthology,
        // and the returned document should have the ID of the parent document.

        var uri =
                UriWrapper.fromUri(
                                "https://x.org/?instanceTypeofChild=AcademicChapter&from=0&size=20")
                        .getUri();
        var expectedHits = 1;
        var expectedParentIdSuffix = "01905518408c-dba987f7-0d84-4519-a625-89605672afc8";

        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        var document = pagedSearchResourceDto.hits().getFirst();
        var actualId = document.get(Words.IDENTIFIER).asText();

        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedHits)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedHits)));
        assertThat(actualId, is(equalTo(expectedParentIdSuffix)));
    }

    @Test
    void shouldFindDocumentsWithParent() throws ApiGatewayException {
        // Given that a parent document exists,
        // and the parent document has two children,
        // when a query filters by HAS_PARENT=true,
        // then two child documents should be returned,
        // and the parent ID should be found in each child document.

        var uri = UriWrapper.fromUri("https://x.org/?HAS_PARENT=true&from=0&size=20").getUri();
        var expectedHits = 2;
        var expectedParentIdSuffix = "0190554a46d9-41780102-675d-43ba-81df-17168d78fa22";

        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        var document = pagedSearchResourceDto.hits().getFirst();
        var actualId = document.get(Words.IDENTIFIER).asText();

        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedHits)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedHits)));
        assertThat(actualId, is(equalTo(expectedParentIdSuffix)));
    }

    @Test
    void shouldFindDocumentsWithChildren() throws ApiGatewayException {
        // Given that a parent document with two children exists,
        // and the parent document has two children,
        // when a query filters by HAS_CHILDREN=true,
        // then one document should be returned,
        // and the returned document should have the ID of the parent document.

        var uri = UriWrapper.fromUri("https://x.org/?HAS_PARENT=true&from=0&size=20").getUri();
        var expectedHits = 2;
        var expectedParentIdSuffix = "01905518408c-dba987f7-0d84-4519-a625-89605672afc8";

        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        var documents = pagedSearchResourceDto.hits();

        for (var document : documents) {
            var actualParentId =
                    document.get(Words.ENTITY_DESCRIPTION)
                            .get(Words.REFERENCE)
                            .get(Words.PUBLICATION_CONTEXT)
                            .get(Words.ID)
                            .asText();

            assertThat(actualParentId, containsString(expectedParentIdSuffix));
        }

        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedHits)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedHits)));
    }

    @Test
    void shouldFindChaptersOfBookAnthology() throws ApiGatewayException {
        // Given that a parent document exists of type BookAnthology,
        // and the parent document has two children of type AcademicChapter,
        // when a query filters by partOf type BookAnthology,
        // then two child documents should be returned,
        // and the parent ID should be found in each child document.

        var uri =
                UriWrapper.fromUri(
                                "https://x.org/?instanceTypeOfParent=BookAnthology&from=0&size=20")
                        .getUri();
        var expectedHits = 2;
        var expectedParentIdSuffix = "01905518408c-dba987f7-0d84-4519-a625-89605672afc8";

        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        var documents = pagedSearchResourceDto.hits();

        for (var document : documents) {
            var actualParentId =
                    document.get(Words.ENTITY_DESCRIPTION)
                            .get(Words.REFERENCE)
                            .get(Words.PUBLICATION_CONTEXT)
                            .get(Words.ID)
                            .asText();

            var actualInstanceType =
                    document.get(Words.ENTITY_DESCRIPTION)
                            .get(Words.REFERENCE)
                            .get(Words.PUBLICATION_INSTANCE)
                            .get(Words.TYPE)
                            .asText();

            assertThat(actualParentId, containsString(expectedParentIdSuffix));
            assertThat(actualInstanceType, is(equalTo("AcademicChapter")));
        }

        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedHits)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedHits)));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
        var csvResult =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE, AGGREGATION)
                        .withAlwaysExcludedFields(EXCLUDED_FIELDS)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withMediaType(Words.TEXT_CSV)
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient)
                        .toString();
        assertNotNull(csvResult);
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var response =
                ResourceSearchQuery.builder()
                        .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE, SORT)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertNotNull(pagedSearchResourceDto.id());
        var searchName = response.parameters().get(SORT).split(Words.COMMA)[0].split(Words.COLON)[0];
        var searchFieldName =
                fromSortKey(searchName)
                        .jsonPaths()
                        .findFirst()
                        .map(path -> path.contains(Words.KEYWORD) ? trimKeyword(path) : path)
                        .map(path -> Words.SLASH + path.replace(Words.DOT, Words.SLASH))
                        .orElseThrow();

        var logInfo =
                response.swsResponse().hits().hits().stream()
                        .map(item -> item._score() + " + " + searchFieldName)
                        .collect(Collectors.joining(Words.SPACE + Words.PIPE + Words.SPACE));
        logger.debug(logInfo);
        assertNotNull(pagedSearchResourceDto.context());
        assertTrue(pagedSearchResourceDto.totalHits() >= 0);
    }

    private String trimKeyword(String path) {
        return path.substring(0, path.indexOf(Words.KEYWORD) - 1);
    }

    @ParameterizedTest
    @MethodSource("uriInvalidProvider")
    void failToSearchUri(URI uri) {
        assertThrows(
                BadRequestException.class,
                () ->
                        ResourceSearchQuery.builder()
                                .fromTestQueryParameters(EntrySetTools.queryToMapEntries(uri))
                                .withRequiredParameters(FROM, SIZE)
                                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                                .build()
                                .doSearch(searchClient));
    }

    @Test
    void shouldReturnResourcesForScientificPeriods() throws BadRequestException {
        var response =
                ResourceSearchQuery.builder()
                        .fromTestParameterMap(
                                Map.of(
                                        SCIENTIFIC_REPORT_PERIOD_SINCE.asCamelCase(),
                                        Y2019,
                                        SCIENTIFIC_REPORT_PERIOD_BEFORE.asCamelCase(),
                                        Y2022))
                        .withRequiredParameters(FROM, SIZE, AGGREGATION)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertThat(pagedSearchResourceDto.hits(), hasSize(2));
    }

    @Test
    void shouldReturnResourcesForSinglePeriods() throws BadRequestException {
        var response =
                ResourceSearchQuery.builder()
                        .fromTestParameterMap(
                                Map.of(
                                        SCIENTIFIC_REPORT_PERIOD_SINCE.asCamelCase(),
                                        Y2019,
                                        SCIENTIFIC_REPORT_PERIOD_BEFORE.asCamelCase(),
                                        Y2020))
                        .withRequiredParameters(FROM, SIZE, AGGREGATION)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);
        var pagedSearchResourceDto = response.toPagedResponse();
        assertThat(pagedSearchResourceDto.hits(), hasSize(1));
    }

    @Test
    void
            shouldNotReturnResourcesContainingAffiliationThatShouldBeExcludedWhenIsSubunitOfRequestedViewingScopeI()
                    throws BadRequestException {
        var viewingScope =
                URLEncoder.encode(
                        "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0",
                        StandardCharsets.UTF_8);
        var response =
                ResourceSearchQuery.builder()
                        .fromTestParameterMap(
                                Map.of(
                                        UNIT.asCamelCase(),
                                        viewingScope,
                                        EXCLUDE_SUBUNITS.asCamelCase(),
                                        Boolean.TRUE.toString()))
                        .withRequiredParameters(FROM, SIZE, AGGREGATION)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        var excludedSubunit = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.1.0";

        assertThat(pagedSearchResourceDto.toJsonString(), not(containsString(excludedSubunit)));
        assertThat(pagedSearchResourceDto.hits(), hasSize(1));
    }

    @Test
    void shouldReturnResourcesWithSubunitsWhenExcludedSubunitsNotProvided()
            throws BadRequestException {
        var unit =
                URLEncoder.encode(
                        "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0",
                        StandardCharsets.UTF_8);
        var topLevelOrg =
                URLEncoder.encode(
                        "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0",
                        StandardCharsets.UTF_8);
        var response =
                ResourceSearchQuery.builder()
                        .fromTestParameterMap(
                                Map.of(
                                        UNIT.asCamelCase(),
                                        unit,
                                        Words.TOP_LEVEL_ORGANIZATION,
                                        topLevelOrg))
                        .withRequiredParameters(FROM, SIZE, AGGREGATION)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA, DELETED)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        var includedSubunitI = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.1.0";
        var includedSubunitII = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.1.1";

        assertThat(pagedSearchResourceDto.toJsonString(), containsString(includedSubunitI));
        assertThat(pagedSearchResourceDto.toJsonString(), containsString(includedSubunitII));
        assertThat(pagedSearchResourceDto.hits(), hasSize(2));
    }

    @ParameterizedTest
    @MethodSource("provideValidPageRanges")
    void shouldFilterByPageCount(int min, int max, int expectedResultCount)
            throws BadRequestException {
        var pageRange = String.format("%d,%d", min, max);
        var response =
                ResourceSearchQuery.builder()
                        .fromTestParameterMap(Map.of(PUBLICATION_PAGES.asCamelCase(), pageRange))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        var pageCounts =
                pagedSearchResourceDto.hits().stream()
                        .map(ResourceClientTest::pageNodeToInt)
                        .toList();

        assertThat("Number of hits", pagedSearchResourceDto.hits(), hasSize(expectedResultCount));
        assertThat(
                "All page counts are within the specified range",
                pageCounts,
                everyItem(allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max))));
    }

    private static int pageNodeToInt(JsonNode hit) {
        return hit.at(
                        String.join(
                                Words.SLASH,
                                Words.SLASH + Words.ENTITY_DESCRIPTION,
                                Words.REFERENCE,
                                Words.PUBLICATION_INSTANCE,
                                Words.PAGES,
                                Words.PAGES))
                .asInt();
    }

    @Test
    void shouldRemoveDocumentFromIndexWithShards()
            throws BadRequestException, IOException, InterruptedException {
        var indexDocument = indexDocumentWithIdentifier();
        indexingClient.addDocumentToIndex(indexDocument);
        Thread.sleep(1000);
        var response = fetchDocumentWithId(indexDocument);

        var pagedSearchResourceDto = response.toPagedResponse();

        MatcherAssert.assertThat(pagedSearchResourceDto.hits(), hasSize(1));

        indexingClient.removeDocumentFromResourcesIndex(indexDocument.getDocumentIdentifier());
        Thread.sleep(1000);
        var responseAfterDeletion = fetchDocumentWithId(indexDocument);

        MatcherAssert.assertThat(responseAfterDeletion.toPagedResponse().hits(), is(emptyIterable()));
    }

    private static HttpResponseFormatter<ResourceParameter> fetchDocumentWithId(
            IndexDocument indexDocument) throws BadRequestException {
        return ResourceSearchQuery.builder()
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .fromTestParameterMap(Map.of(Words.ID, indexDocument.getDocumentIdentifier()))
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .apply()
                .doSearch(searchClient);
    }

    private static IndexDocument indexDocumentWithIdentifier() throws JsonProcessingException {
        var identifier = SortableIdentifier.next();
        var document =
                """
                {
                     "type": "Publication",
                     "status": "PUBLISHED",
                     "identifier": "__ID__"
                }
                """
                        .replace("__ID__", identifier.toString());
        var jsonNode = JsonUtils.dtoObjectMapper.readTree(document);
        return new IndexDocument(new EventConsumptionAttributes(Words.RESOURCES, identifier), jsonNode);
    }
}
