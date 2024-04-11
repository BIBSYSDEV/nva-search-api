package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.utils.UriRetriever.ACCEPT;
import static no.unit.nva.search2.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search2.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.EQUAL;
import static no.unit.nva.search2.common.constant.Words.NOTIFICATIONS;
import static no.unit.nva.search2.common.constant.Words.STATUS;
import static no.unit.nva.search2.common.constant.Words.TICKETS;
import static no.unit.nva.search2.common.constant.Words.TYPE;
import static no.unit.nva.search2.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;
import static no.unit.nva.search2.ticket.TicketParameter.SORT;
import static no.unit.nva.search2.ticket.TicketType.DOI_REQUEST;
import static no.unit.nva.search2.ticket.TicketType.GENERAL_SUPPORT_CASE;
import static no.unit.nva.search2.ticket.TicketType.PUBLISHING_REQUEST;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.RestHighLevelClientWrapper;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.ticket.TicketClient;
import no.unit.nva.search2.ticket.TicketQuery;
import no.unit.nva.search2.ticket.TicketStatus;
import no.unit.nva.search2.ticket.TicketType;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
    private static final String USER_TICKETS_VALID_TEST_URL_JSON = "datasource_urls_user_ticket.json";
    private static final String SAMPLE_TICKETS_SEARCH_JSON = "datasource_tickets.json";
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    public static final String REQUEST_BASE_URL = "https://x.org/?size=21&";
    public static final int EXPECTED_NUMBER_OF_AGGREGATIONS = 5;
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
        when(mockedRequestInfo.userIsAuthorized(AccessRight.SUPPORT))
            .thenReturn(Boolean.TRUE)
            .thenReturn(Boolean.FALSE);
        when(mockedRequestInfo.userIsAuthorized(AccessRight.MANAGE_DOI))
            .thenReturn(Boolean.FALSE)
            .thenReturn(Boolean.TRUE);
        when(mockedRequestInfo.userIsAuthorized(AccessRight.MANAGE_PUBLISHING_REQUESTS))
            .thenReturn(Boolean.FALSE)
            .thenReturn(Boolean.TRUE);
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
                () -> TicketQuery.builder()
                    .withRequiredParameters(SIZE, FROM)
                    .fromQueryParameters(toMapEntries)
                    .build()
                    .withFilterOrganization(testOrganizationId)
                    .doSearch(resourceClient2)
            );
        }

        @Test
        void shouldCheckFacets() throws BadRequestException {
            var hostAddress = URI.create(container.getHttpHostAddress());
            var uri1 = URI.create(REQUEST_BASE_URL + AGGREGATION.name() + EQUAL + ALL);
            var query1 = TicketQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri1))
                .withDockerHostUri(hostAddress)
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withFilterOrganization(testOrganizationId)
                .withFilterTicketType(DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                .withCurrentUser(CURRENT_USERNAME);

            var response1 = searchClient.doSearch(query1);
            assertNotNull(response1);

            var aggregations = query1.toPagedResponse(response1).aggregations();

            assertFalse(aggregations.isEmpty());
            assertThat(aggregations.size(), is(equalTo(EXPECTED_NUMBER_OF_AGGREGATIONS)));

            assertThat(aggregations.get(TYPE).size(), is(3));
            assertThat(aggregations.get(STATUS).get(0).count(), is(11));
            assertThat(aggregations.get(NOTIFICATIONS).size(), is(5));
            assertNotNull(FROM.asLowerCase());
            assertEquals(TicketStatus.fromString("ewrdfg"), TicketStatus.NONE);
            assertEquals(TicketType.fromString("wre"), TicketType.NONE);

        }

        @Test
        void emptyResultShouldIncludeHits() throws BadRequestException {
            var uri = URI.create("https://x.org/?id=018b857b77b7");

            var pagedResult =
                TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE, SORT)
                    .build()
                    .withFilterOrganization(testOrganizationId)
                    .withFilterTicketType(DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .withCurrentUser(CURRENT_USERNAME)
                    .doSearch(searchClient);
            assertNotNull(pagedResult);
            assertTrue(pagedResult.contains("\"hits\":["));
        }

        @ParameterizedTest
        @MethodSource("uriPagingProvider")
        void uriRequestPageableReturnsSuccessfulResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilterOrganization(testOrganizationId)
                    .withFilterTicketType(DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .withCurrentUser(CURRENT_USERNAME);

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            logger.debug(pagedSearchResourceDto.id().toString());
        }

        @ParameterizedTest
        @MethodSource("uriProviderAsAdmin")
        void uriRequestReturnsSuccessfulResponseAsAdmin(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilterOrganization(testOrganizationId)
                    .withFilterTicketType(DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE);

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

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
        @MethodSource("uriProviderAsUser")
        void uriRequestReturnsSuccessfulResponseAsUser(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilterOrganization(testOrganizationId)
                    .withCurrentUser(CURRENT_USERNAME)
                    .applyFilterCurrentUser();

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedCount)));
        }


        @ParameterizedTest
        @MethodSource("uriProviderAsAdmin")
        void uriRequestReturnsCsvResponse(URI uri) throws ApiGatewayException {
            var query =
                queryToMapEntries(uri).stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            when(mockedRequestInfo.getQueryParameters())
                .thenReturn(query);

            var csvResult =
                TicketQuery.builder()
                    .fromRequestInfo(mockedRequestInfo)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE)
                    .build()
                    .applyContextAndAuthorize(mockedRequestInfo)
                    .doSearch(searchClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void uriRequestWithSortingReturnsSuccessfulResponse(URI uri) throws ApiGatewayException {
            var query =
                TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, SORT, AGGREGATION)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withFilterOrganization(testOrganizationId)
                    .withFilterTicketType(DOI_REQUEST, PUBLISHING_REQUEST, GENERAL_SUPPORT_CASE)
                    .withCurrentUser(CURRENT_USERNAME);

            logger.info(query.parameters().get(SORT).toString());
            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);
            assertNotNull(pagedSearchResourceDto.id());
            assertNotNull(pagedSearchResourceDto.context());
            assertTrue(pagedSearchResourceDto.totalHits() >= 0);
        }

        @ParameterizedTest
        @MethodSource("uriInvalidProvider")
        void uriRequestReturnsBadRequest(URI uri) {
            assertThrows(
                BadRequestException.class,
                () -> TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .doSearch(searchClient));
        }

        @Test
        void uriRequestReturnsUnauthorized() {
            AtomicReference<URI> uri = new AtomicReference<>();
            uriSortingProvider().findFirst().ifPresent(uri::set);
            var mockedRequestInfoLocal = mock(RequestInfo.class);
            assertThrows(
                UnauthorizedException.class,
                () -> TicketQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri.get()))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .applyContextAndAuthorize(mockedRequestInfoLocal)
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

        static Stream<URI> uriSortingProvider() {

            return Stream.of(
                URI.create(REQUEST_BASE_URL + "sort=status&sortOrder=asc&sort=created_date&order=desc"),
                URI.create(REQUEST_BASE_URL + "orderBy=status:asc,created_date:desc"),
                URI.create(REQUEST_BASE_URL + "sort=status+asc&sort=created_date+desc"),
                URI.create(REQUEST_BASE_URL + "sort=created_date&sortOrder=asc&sort=status&order=desc"),
                URI.create(REQUEST_BASE_URL + "sort=modified_date+asc&sort=type+desc"),
                URI.create(REQUEST_BASE_URL + "sort=modified_date+asc&SEARCH_AFTER=12312")

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

        static Stream<Arguments> uriProviderAsUser() {
            return loadMapFromResource(USER_TICKETS_VALID_TEST_URL_JSON).entrySet().stream()
                .map(entry -> createArgument(entry.getKey(), entry.getValue()));
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