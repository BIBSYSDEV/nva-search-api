package no.unit.nva.search.ticket;

import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.EQUAL;
import static no.unit.nva.constants.Words.PUBLICATION_STATUS;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.constants.Words.TICKETS;
import static no.unit.nva.constants.Words.TYPE;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search.ticket.TicketParameter.BY_USER_PENDING;
import static no.unit.nva.search.ticket.TicketParameter.FROM;
import static no.unit.nva.search.ticket.TicketParameter.OWNER;
import static no.unit.nva.search.ticket.TicketParameter.SIZE;
import static no.unit.nva.search.ticket.TicketParameter.SORT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;

import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Testcontainers
class TicketClientTest {

    public static final String REQUEST_BASE_URL = "https://x.org/?size=22&";
    public static final int EXPECTED_NUMBER_OF_AGGREGATIONS = 4;
    public static final String CURRENT_USERNAME = "1412322@20754.0.0.0";
    public static final URI testOrganizationId =
            URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    private static final Logger logger = LoggerFactory.getLogger(TicketClientTest.class);
    private static final String TICKETS_VALID_TEST_URL_JSON = "ticket_datasource_urls.json";
    private static final RequestInfo mockedRequestInfo = mock(RequestInfo.class);
    private static TicketClient searchClient;

    @BeforeAll
    public static void setUp() throws UnauthorizedException {
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        searchClient = new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);

        when(mockedRequestInfo.getTopLevelOrgCristinId())
                .thenReturn(Optional.of(testOrganizationId));
        when(mockedRequestInfo.getUserName()).thenReturn(CURRENT_USERNAME);
        when(mockedRequestInfo.getHeaders()).thenReturn(Map.of(ACCEPT, Words.TEXT_CSV));
    }

    static Stream<Arguments> uriPagingProvider() {
        return Stream.of(
                createArgument("page=0&aggregation=all,the,best,", 20),
                createArgument("page=1&aggregation=all&size=1", 1),
                createArgument("page=2&aggregation=all&size=1", 1),
                createArgument("page=3&aggregation=all&size=1", 1),
                createArgument("page=0&aggregation=all&size=0", 0),
                createArgument("offset=15&aggregation=all&size=2", 2),
                createArgument("offset=15&aggregation=all&limit=2", 2),
                createArgument("offset=15&aggregation=all&results=2", 2),
                createArgument("offset=15&aggregation=all&per_page=2", 2),
                createArgument("OFFSET=15&aggregation=all&PER_PAGE=2", 2),
                createArgument("offset=15&perPage=2", 2));
    }

    static Stream<Arguments> uriAccessRights() {
        return Stream.of(
                createAccessRightArgument("owner=1412322@20754.0.0.0", 16, "1412322@20754.0.0.0"),
                createAccessRightArgument("owner=1492596@20754.0.0.0", 3, "1492596@20754.0.0.0"),
                createAccessRightArgument(
                        "STATISTICS=true", 22, "1492596@20754.0.0.0", MANAGE_CUSTOMERS),
                createAccessRightArgument("", 4, "1492596@20754.0.0.0", MANAGE_DOI),
                createAccessRightArgument("", 8, "1492596@20754.0.0.0", SUPPORT),
                createAccessRightArgument(
                        "", 15, "1492596@20754.0.0.0", MANAGE_PUBLISHING_REQUESTS),
                createAccessRightArgument("", 1, "1485369@5923.0.0.0", MANAGE_DOI),
                createAccessRightArgument("", 6, "1485369@5923.0.0.0", SUPPORT),
                createAccessRightArgument("", 13, "1485369@5923.0.0.0", MANAGE_PUBLISHING_REQUESTS),
                createAccessRightArgument("", 7, "1485369@5923.0.0.0", MANAGE_DOI, SUPPORT),
                createAccessRightArgument(
                        "",
                        20,
                        "1485369@5923.0.0.0",
                        MANAGE_DOI,
                        SUPPORT,
                        MANAGE_PUBLISHING_REQUESTS),
                createAccessRightArgument(
                        "",
                        20,
                        "1412322@20754.0.0.0",
                        MANAGE_DOI,
                        SUPPORT,
                        MANAGE_PUBLISHING_REQUESTS));
    }

    static Stream<URI> uriSortingProvider() {

        return Stream.of(
                URI.create(
                        REQUEST_BASE_URL
                                + "sort=status&sortOrder=asc&sort=created_date&order=desc"),
                URI.create(REQUEST_BASE_URL + "orderBy=status:asc,created_date:desc"),
                URI.create(REQUEST_BASE_URL + "sort=status+asc&sort=created_date+desc"),
                URI.create(
                        REQUEST_BASE_URL
                                + "sort=created_date&sortOrder=asc&sort=status&order=desc"),
                URI.create(REQUEST_BASE_URL + "sort=modified_date+asc&sort=type+desc"),
                URI.create(REQUEST_BASE_URL + "sort=relevance,modified_date+asc"));
    }

    static Stream<URI> uriInvalidProvider() {
        return Stream.of(
                URI.create(REQUEST_BASE_URL + "feilName=epler"),
                URI.create(REQUEST_BASE_URL + "query=epler&fields=feilName"),
                URI.create(REQUEST_BASE_URL + "CREATED_DATE=epler"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY:DEdd"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORdfgY:desc"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY:asc:DEdd"),
                URI.create(REQUEST_BASE_URL + "categories=hello+world&lang=en"),
                URI.create(REQUEST_BASE_URL + "tittles=hello+world&modified_before=2019-01-01"),
                URI.create(REQUEST_BASE_URL + "useers=hello+world&lang=en"));
    }

    static Stream<Arguments> uriProviderAsAdmin() {
        return loadMapFromResource(TICKETS_VALID_TEST_URL_JSON).entrySet().stream()
                .map(entry -> createArgument(entry.getKey(), entry.getValue()));
    }

    private static Arguments createAccessRightArgument(
            String searchUri, int expectedCount, String userName, AccessRight... accessRights) {
        return Arguments.of(
                URI.create(REQUEST_BASE_URL + searchUri), expectedCount, userName, accessRights);
    }

    private static Arguments createArgument(String searchUri, int expectedCount) {
        return Arguments.of(URI.create(REQUEST_BASE_URL + searchUri), expectedCount);
    }

    private static Map<String, Integer> loadMapFromResource(String resource) {
        var mappingsJson = stringFromResources(Path.of(resource));
        var type = new TypeReference<Map<String, Integer>>() {};
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
    }

    @Test
    void shouldCheckMapping() {

        var mapping = indexingClient.getMapping(TICKETS);
        assertThat(mapping, is(notNullValue()));
        //            var topLevelOrgType = mapping.path("properties")
        //                .path(PUBLICATION)
        //                .path(CONTRIBUTORS)
        //                .path(AFFILIATIONS)
        //                .;
        //            assertThat(topLevelOrgType, is(equalTo("<MISSING>")));
        logger.info(mapping.toString());
    }

    @Test
    void openSearchFailedResponse() {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.sendAsync(any(), any()))
                .thenReturn(mockedFutureHttpResponse((String) null));
        var toMapEntries = queryToMapEntries(URI.create("https://example.com/?size=2"));
        var resourceClient2 = new TicketClient(httpClient, setupMockedCachedJwtProvider());
        assertThrows(
                RuntimeException.class,
                () ->
                        TicketSearchQuery.builder()
                                .withRequiredParameters(SIZE, FROM)
                                .fromTestQueryParameters(toMapEntries)
                                .build()
                                .doSearch(resourceClient2));
    }

    @Test
    void shouldCheckFacets() throws BadRequestException, UnauthorizedException {
        var hostAddress = URI.create(container.getHttpHostAddress());
        var uri1 = URI.create(REQUEST_BASE_URL + AGGREGATION.name() + EQUAL + ALL);
        var response1 =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri1))
                        .withDockerHostUri(hostAddress)
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .organization(testOrganizationId)
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .apply()
                        .doSearch(searchClient);

        assertNotNull(response1);

        var aggregations = response1.toPagedResponse().aggregations();

        assertFalse(aggregations.isEmpty());
        assertThat(aggregations.size(), is(equalTo(EXPECTED_NUMBER_OF_AGGREGATIONS)));

        assertThat(aggregations.get(TYPE).size(), is(3));
        assertThat(aggregations.get(STATUS).getFirst().count(), is(11));
        assertThat(aggregations.get(BY_USER_PENDING.asCamelCase()).size(), is(2));
        assertThat(aggregations.get(PUBLICATION_STATUS).size(), is(3));

        assertNotNull(FROM.asLowerCase());
        assertEquals(TicketStatus.NONE, TicketStatus.fromString("ewrdfg"));
        assertEquals(TicketType.NONE, TicketType.fromString("wre"));
    }

    @Test
    void emptyResultShouldIncludeHits() throws BadRequestException, UnauthorizedException {
        var uri = URI.create("https://x.org/?id=018b857b77b7");

        var pagedResult =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE, SORT)
                        .build()
                        .withFilter()
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .organization(testOrganizationId)
                        .apply()
                        .doSearch(searchClient);
        assertNotNull(pagedResult.swsResponse());
        assertTrue(pagedResult.toString().contains("\"hits\":["));
    }

    @Test
    void shouldReturnNewTicketsWhenSearchingForNewTicketsOnlyAndTypes()
            throws BadRequestException, UnauthorizedException {
        var uri =
                URI.create(
                        "https://x.org/?status=New&size=500&type=doiRequest,generalSupportCase,publishingRequest&from=0");

        var pagedResult =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .organization(testOrganizationId)
                        .apply()
                        .doSearch(searchClient)
                        .toPagedResponse();

        assertEquals(7, pagedResult.hits().size());
    }

    @Test
    void shouldReturnNewTicketsWhenSearchingForNewTicketsOnly()
            throws BadRequestException, UnauthorizedException {
        var uri = URI.create("https://x.org/?status=New&size=500&from=0");

        var pagedResult =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .organization(testOrganizationId)
                        .apply()
                        .doSearch(searchClient)
                        .toPagedResponse();

        assertEquals(7, pagedResult.hits().size());
    }

    @Test
    void
            shouldReturnNewAndPendingTicketsWithAssigneeWhenSearchingForTicketsWithStatusNewAndPendingAndAssignee()
                    throws BadRequestException, UnauthorizedException {
        var uri =
                URI.create(
                        "https://x.org/?status=New,Pending&assignee=1412322@20754.0.0.0&size=10&from=0");

        var pagedResult =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .organization(testOrganizationId)
                        .apply()
                        .doSearch(searchClient)
                        .toPagedResponse();

        assertEquals(9, pagedResult.hits().size());
    }

    @ParameterizedTest
    @MethodSource("uriPagingProvider")
    void uriRequestPageableReturnsSuccessfulResponse(URI uri, int expectedCount)
            throws ApiGatewayException {

        var response =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .organization(testOrganizationId)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        assertNotNull(pagedSearchResourceDto);
        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
        logger.debug(pagedSearchResourceDto.id().toString());
    }

    @ParameterizedTest
    @MethodSource("uriProviderAsAdmin")
    void uriRequestReturnsSuccessfulResponseAsAdmin(URI uri, int expectedCount)
            throws ApiGatewayException {

        var response =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .user(CURRENT_USERNAME)
                        .accessRights(
                                MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT, MANAGE_CUSTOMERS)
                        .organization(testOrganizationId)
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

    @ParameterizedTest
    @MethodSource("uriAccessRights")
    void uriRequestReturnsSuccessfulResponseAsUser(
            URI uri, Integer expectedCount, String userName, AccessRight... accessRights)
            throws ApiGatewayException {

        var accessRightList =
                new java.util.ArrayList<>(
                        nonNull(accessRights) ? Arrays.asList(accessRights) : List.of());

        var mockedRequestInfoLocal = mock(RequestInfo.class);
        when(mockedRequestInfoLocal.getUserName()).thenReturn(userName);
        when(mockedRequestInfoLocal.getTopLevelOrgCristinId())
                .thenReturn(Optional.of(testOrganizationId));
        when(mockedRequestInfoLocal.getAccessRights()).thenReturn(accessRightList);

        var response =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .fromRequestInfo(mockedRequestInfoLocal)
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();

        assertNotNull(pagedSearchResourceDto);
        assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
        assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedCount)));
    }

    @Test
    void shouldThrowUnauthorizedWhenRequestingTicketsForOwnerWhichIsNotCurrentCustomer()
            throws UnauthorizedException {
        var currentUserUsername = randomString();
        var ownerToSearch = randomString();

        var mockedRequestInfoLocal = mock(RequestInfo.class);
        when(mockedRequestInfoLocal.getUserName()).thenReturn(currentUserUsername);
        when(mockedRequestInfoLocal.getTopLevelOrgCristinId())
                .thenReturn(Optional.of(testOrganizationId));

        assertThrows(
                UnauthorizedException.class,
                () ->
                        TicketSearchQuery.builder()
                                .withParameter(OWNER, ownerToSearch)
                                .withRequiredParameters(FROM, SIZE)
                                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                                .build()
                                .withFilter()
                                .fromRequestInfo(mockedRequestInfoLocal)
                                .doSearch(searchClient));
    }

    @ParameterizedTest
    @MethodSource("uriProviderAsAdmin")
    @Disabled(
            "Does not work. When test was written it returned an empty string even if there were"
                    + " supposed to be hits. Now we throw an exception instead as the method is not"
                    + " implemented.")
    void uriRequestReturnsCsvResponse(URI uri) throws ApiGatewayException {
        var query =
                queryToMapEntries(uri).stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        when(mockedRequestInfo.getQueryParameters()).thenReturn(query);

        var csvResult =
                TicketSearchQuery.builder()
                        .fromRequestInfo(mockedRequestInfo)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .withRequiredParameters(FROM, SIZE)
                        .build()
                        .withFilter()
                        .fromRequestInfo(mockedRequestInfo)
                        .doSearch(searchClient);
        assertNotNull(csvResult);
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriRequestWithSortingReturnsSuccessfulResponse(URI uri) throws ApiGatewayException {
        var response =
                TicketSearchQuery.builder()
                        .fromTestQueryParameters(queryToMapEntries(uri))
                        .withRequiredParameters(FROM, SIZE, SORT, AGGREGATION)
                        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                        .build()
                        .withFilter()
                        .organization(testOrganizationId)
                        .user(CURRENT_USERNAME)
                        .accessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT)
                        .apply()
                        .doSearch(searchClient);

        var pagedSearchResourceDto = response.toPagedResponse();
        assertNotNull(pagedSearchResourceDto.id());
        assertNotNull(pagedSearchResourceDto.context());
        assertTrue(pagedSearchResourceDto.totalHits() >= 0);
    }

    @ParameterizedTest
    @MethodSource("uriInvalidProvider")
    void uriRequestReturnsBadRequest(URI uri) {
        assertThrows(
                BadRequestException.class,
                () ->
                        TicketSearchQuery.builder()
                                .fromTestQueryParameters(queryToMapEntries(uri))
                                .withRequiredParameters(FROM, SIZE)
                                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                                .build()
                                .doSearch(searchClient));
    }

    @Test
    void uriRequestReturnsUnauthorized() throws UnauthorizedException {
        AtomicReference<URI> uri = new AtomicReference<>();
        uriSortingProvider().findFirst().ifPresent(uri::set);
        var mockedRequestInfoLocal = mock(RequestInfo.class);
        when(mockedRequestInfoLocal.getAccessRights()).thenReturn(List.of());
        when(mockedRequestInfoLocal.getCurrentCustomer()).thenReturn(null);
        assertThrows(
                UnauthorizedException.class,
                () ->
                        TicketSearchQuery.builder()
                                .fromTestQueryParameters(queryToMapEntries(uri.get()))
                                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                                .build()
                                .withFilter()
                                .fromRequestInfo(mockedRequestInfoLocal)
                                .doSearch(searchClient));
    }
}
