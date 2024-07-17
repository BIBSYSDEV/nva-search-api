package no.unit.nva.search;

import static java.util.Objects.nonNull;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.search.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search.common.constant.Words.ALL;
import static no.unit.nva.search.common.constant.Words.EQUAL;
import static no.unit.nva.search.common.constant.Words.STATUS;
import static no.unit.nva.search.common.constant.Words.TICKETS;
import static no.unit.nva.search.common.constant.Words.TYPE;
import static no.unit.nva.search.ticket.Constants.PUBLICATION_STATUS;
import static no.unit.nva.search.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search.ticket.TicketParameter.BY_USER_PENDING;
import static no.unit.nva.search.ticket.TicketParameter.FROM;
import static no.unit.nva.search.ticket.TicketParameter.SIZE;
import static no.unit.nva.search.ticket.TicketParameter.SORT;
import static no.unit.nva.search.ticket.TicketType.DOI_REQUEST;
import static no.unit.nva.search.ticket.TicketType.GENERAL_SUPPORT_CASE;
import static no.unit.nva.search.ticket.TicketType.PUBLISHING_REQUEST;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.search.common.constant.Words;
import no.unit.nva.search.ticket.TicketClient;
import no.unit.nva.search.ticket.TicketSearchQuery;
import no.unit.nva.search.ticket.TicketStatus;
import no.unit.nva.search.ticket.TicketType;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TicketClientTest {

    private static final Logger logger = LoggerFactory.getLogger(TicketClientTest.class);
    private static final String TEST_TICKETS_MAPPINGS_JSON = "mapping_test_tickets.json";
    private static final String TICKETS_VALID_TEST_URL_JSON = "datasource_urls_ticket.json";
    private static final String SAMPLE_TICKETS_SEARCH_JSON = "datasource_tickets.json";
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    public static final String REQUEST_BASE_URL = "https://x.org/?size=21&";
    public static final int EXPECTED_NUMBER_OF_AGGREGATIONS = 4;
    public static final String CURRENT_USERNAME = "1412322@20754.0.0.0";
    public static final URI testOrganizationId =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

    private static TicketClient searchClient;
    private static IndexingClient indexingClient;

    private static final RequestInfo mockedRequestInfo = mock(RequestInfo.class);

    @BeforeAll
    static void setUp() throws IOException, InterruptedException, UnauthorizedException {
        container.start();

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
        searchClient = new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);

        when(mockedRequestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(testOrganizationId));
        when(mockedRequestInfo.getUserName()).thenReturn(CURRENT_USERNAME);
        when(mockedRequestInfo.getHeaders()).thenReturn(Map.of(ACCEPT, Words.TEXT_CSV));
        createIndex();
        populateIndex();
        logger.info("Waiting {} ms for indexing to complete", DELAY_AFTER_INDEXING);
        Thread.sleep(DELAY_AFTER_INDEXING);
    }

    @AfterAll
    static void afterAll() throws IOException, InterruptedException {
        logger.info("Stopping container");
        indexingClient.deleteIndex(TICKETS);
        Thread.sleep(DELAY_AFTER_INDEXING);
        container.stop();
    }

    @Nested
    class Queries {

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
        void openSearchFailedResponse() throws IOException, InterruptedException {
            HttpClient httpClient = mock(HttpClient.class);
            var response = mock(HttpResponse.class);
            when(httpClient.send(any(), any())).thenReturn(response);
            when(response.statusCode()).thenReturn(500);
            when(response.body()).thenReturn("EXPECTED ERROR");
            var toMapEntries = queryToMapEntries(URI.create("https://example.com/?size=2"));
            var resourceClient2 = new TicketClient(httpClient, setupMockedCachedJwtProvider());
            assertThrows(
                RuntimeException.class,
                () -> TicketSearchQuery.builder()
                    .withRequiredParameters(SIZE, FROM)
                    .fromQueryParameters(toMapEntries)
                    .build()
                    .withFilter()
                    .organization(testOrganizationId).apply()
                    .doSearch(resourceClient2)
            );
        }

        @Test
        void shouldCheckFacets() throws BadRequestException {
            var hostAddress = URI.create(container.getHttpHostAddress());
            var uri1 = URI.create(REQUEST_BASE_URL + AGGREGATION.name() + EQUAL + ALL);
            var response1 = TicketSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri1))
                .withDockerHostUri(hostAddress)
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withFilter()
                .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                .apply()
                .doSearch(searchClient);

            assertNotNull(response1);

            var aggregations = response1.toPagedResponse().aggregations();

            assertFalse(aggregations.isEmpty());
            assertThat(aggregations.size(), is(equalTo(EXPECTED_NUMBER_OF_AGGREGATIONS)));

            assertThat(aggregations.get(TYPE).size(), is(3));
            assertThat(aggregations.get(STATUS).get(0).count(), is(12));
            assertThat(aggregations.get(BY_USER_PENDING.asCamelCase()).size(), is(2));
            assertThat(aggregations.get(PUBLICATION_STATUS).size(), is(3));

            assertNotNull(FROM.asLowerCase());
            assertEquals(TicketStatus.fromString("ewrdfg"), TicketStatus.NONE);
            assertEquals(TicketType.fromString("wre"), TicketType.NONE);

        }

        @Test
        void emptyResultShouldIncludeHits() throws BadRequestException {
            var uri = URI.create("https://x.org/?id=018b857b77b7");

            var pagedResult =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE, SORT)
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId).apply()
                    .doSearch(searchClient);
            assertNotNull(pagedResult.swsResponse());
            assertTrue(pagedResult.toString().contains("\"hits\":["));
        }

        @Test
        void shouldReturnNewTicketsWhenSearchingForNewTicketsOnlyAndTypes() throws BadRequestException {
            var uri = URI.create("https://x.org/?status=New&size=500&type=doiRequest,generalSupportCase,publishingRequest&from=0");

            var pagedResult =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId).apply()
                    .doSearch(searchClient)
                    .toPagedResponse();

            assertEquals(7, pagedResult.hits().size());
        }

        @Test
        void shouldReturnNewTicketsWhenSearchingForNewTicketsOnly() throws BadRequestException {
            var uri = URI.create("https://x.org/?status=New&size=500&from=0");

            var pagedResult =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId).apply()
                    .doSearch(searchClient)
                    .toPagedResponse();

            assertEquals(7, pagedResult.hits().size());
        }

        @Test
        void shouldReturnNewAndPendingTicketsWithAssigneeWhenSearchingForTicketsWithStatusNewAndPendingAndAssignee()
            throws BadRequestException {
            var uri = URI.create("https://x.org/?status=New,Pending&assignee=1412322@20754.0.0.0&size=10&from=0");

            var pagedResult =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId).apply()
                    .doSearch(searchClient)
                    .toPagedResponse();

            assertEquals(9, pagedResult.hits().size());
        }

        @ParameterizedTest
        @MethodSource("uriPagingProvider")
        void uriRequestPageableReturnsSuccessfulResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var response =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId).apply()
                    .doSearch(searchClient);

            var pagedSearchResourceDto = response.toPagedResponse();

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            logger.debug(pagedSearchResourceDto.id().toString());
        }

        @ParameterizedTest
        @MethodSource("uriProviderAsAdmin")
        void uriRequestReturnsSuccessfulResponseAsAdmin(URI uri, int expectedCount) throws ApiGatewayException {

            var response =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId).apply().doSearch(searchClient);

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
        void uriRequestReturnsSuccessfulResponseAsUser(URI uri, Integer expectedCount, String userName, AccessRight... accessRights) throws ApiGatewayException {

            final var accessRightList = nonNull(accessRights)
                ? Arrays.asList(accessRights)
                : List.<AccessRight>of();

            var mockedRequestInfoLocal = mock(RequestInfo.class);
            when(mockedRequestInfoLocal.getUserName())
                .thenReturn(userName);
            when(mockedRequestInfoLocal.getTopLevelOrgCristinId())
                .thenReturn(Optional.of(testOrganizationId));

            when(mockedRequestInfoLocal.getAccessRights())
                .thenReturn(accessRightList);


            var response =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
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


        @ParameterizedTest
        @MethodSource("uriProviderAsAdmin")
        @Disabled("Does not work. When test was written it returned an empty string even if there were supposed to be"
                  + " hits. Now we throw an exception instead as the method is not implemented.")
        void uriRequestReturnsCsvResponse(URI uri) throws ApiGatewayException {
            var query =
                queryToMapEntries(uri).stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            when(mockedRequestInfo.getQueryParameters())
                .thenReturn(query);

            var csvResult =
                TicketSearchQuery.builder()
                    .fromRequestInfo(mockedRequestInfo)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE)
                    .build()
                    .withFilter().fromRequestInfo(mockedRequestInfo)
                    .doSearch(searchClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void uriRequestWithSortingReturnsSuccessfulResponse(URI uri) throws ApiGatewayException {
            var response =
                TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, SORT, AGGREGATION)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .userAndTicketTypes(CURRENT_USERNAME, DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .organization(testOrganizationId)
                    .apply().doSearch(searchClient);

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
                () -> TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
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
                () -> TicketSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri.get()))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilter()
                    .fromRequestInfo(mockedRequestInfoLocal)
                    .doSearch(searchClient));
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
                createArgument("offset=15&perPage=2", 2)
            );
        }

        static Stream<Arguments> uriAccessRights() {
            return Stream.of(
                createAccessRightArgument("", 16, "1412322@20754.0.0.0"),
                createAccessRightArgument("", 2, "1492596@20754.0.0.0"),
                createAccessRightArgument("", 3, "1492596@20754.0.0.0", MANAGE_DOI),
                createAccessRightArgument("", 7, "1492596@20754.0.0.0", AccessRight.SUPPORT),
                createAccessRightArgument("", 14, "1492596@20754.0.0.0", MANAGE_PUBLISHING_REQUESTS),
                createAccessRightArgument("", 0, "1485369@5923.0.0.0"),
                createAccessRightArgument("", 1, "1485369@5923.0.0.0", MANAGE_DOI),
                createAccessRightArgument("", 6, "1485369@5923.0.0.0", AccessRight.SUPPORT),
                createAccessRightArgument("", 13, "1485369@5923.0.0.0", MANAGE_PUBLISHING_REQUESTS),
                createAccessRightArgument("", 7, "1485369@5923.0.0.0", MANAGE_DOI, AccessRight.SUPPORT),
                createAccessRightArgument("", 20, "1485369@5923.0.0.0", MANAGE_DOI, AccessRight.SUPPORT, MANAGE_PUBLISHING_REQUESTS),
                createAccessRightArgument("", 20, "1412322@20754.0.0.0", MANAGE_DOI, AccessRight.SUPPORT, MANAGE_PUBLISHING_REQUESTS)
            );
        }


        static Stream<URI> uriSortingProvider() {

            return Stream.of(
                URI.create(REQUEST_BASE_URL + "sort=status&sortOrder=asc&sort=created_date&order=desc"),
                URI.create(REQUEST_BASE_URL + "orderBy=status:asc,created_date:desc"),
                URI.create(REQUEST_BASE_URL + "sort=status+asc&sort=created_date+desc"),
                URI.create(REQUEST_BASE_URL + "sort=created_date&sortOrder=asc&sort=status&order=desc"),
                URI.create(REQUEST_BASE_URL + "sort=modified_date+asc&sort=type+desc"),
                URI.create(REQUEST_BASE_URL + "sort=relevance,modified_date+asc")
            );
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

        private static Arguments createAccessRightArgument(String searchUri, int expectedCount, String userName, AccessRight... accessRights) {
            return Arguments.of(URI.create(REQUEST_BASE_URL + searchUri), expectedCount, userName, accessRights);
        }
    }


    private static Arguments createArgument(String searchUri, int expectedCount) {
        return Arguments.of(URI.create(REQUEST_BASE_URL + searchUri), expectedCount);
    }

    private static Map<String, Integer> loadMapFromResource(String resource) {
        var mappingsJson = stringFromResources(Path.of(resource));
        var type = new TypeReference<Map<String, Integer>>() {
        };
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
    }

    private static void populateIndex() {
        var jsonFile = stringFromResources(Path.of(SAMPLE_TICKETS_SEARCH_JSON));
        var jsonNodes =
            attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

        jsonNodes.forEach(TicketClientTest::addDocumentToIndex);
    }

    private static void addDocumentToIndex(JsonNode node) {
        try {
            var attributes = new EventConsumptionAttributes(TICKETS, SortableIdentifier.next());
            indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createIndex() throws IOException {
        var mappingsJson = stringFromResources(Path.of(TEST_TICKETS_MAPPINGS_JSON));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
        indexingClient.createIndex(TICKETS, mappings);
    }
}