package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search2.common.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search2.common.constant.Words.HAS_PUBLIC_FILE;
import static no.unit.nva.search2.common.constant.Words.LICENSE;
import static no.unit.nva.search2.common.constant.Words.PUBLISHER;
import static no.unit.nva.search2.common.constant.Words.RESOURCES;
import static no.unit.nva.search2.common.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.common.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.common.constant.Words.TYPE;
import static no.unit.nva.search2.common.enums.PublicationStatus.DELETED;
import static no.unit.nva.search2.common.enums.PublicationStatus.DRAFT;
import static no.unit.nva.search2.common.enums.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.search2.common.enums.PublicationStatus.NEW;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search2.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.INSTANCE_TYPE;
import static no.unit.nva.search2.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_BEFORE;
import static no.unit.nva.search2.resource.ResourceParameter.SCIENTIFIC_REPORT_PERIOD_SINCE;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import static no.unit.nva.search2.resource.ResourceParameter.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.RestHighLevelClientWrapper;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.search2.resource.ResourceQuery;
import no.unit.nva.search2.resource.UserSettingsClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
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
class ResourceClientTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceClientTest.class);
    private static final String EMPTY_USER_RESPONSE_JSON = "user_settings_empty.json";
    private static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.0.0";
    private static final String TEST_RESOURCES_MAPPINGS_JSON = "test_resources_mappings.json";
    private static final String RESOURCE_VALID_TEST_URL_JSON = "test_resource_urls.json";
    private static final String SAMPLE_RESOURCES_SEARCH_JSON = "sample_resources_search.json";
    private static final long DELAY_AFTER_INDEXING = 1500L;
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    public static final String REQUEST_BASE_URL = "https://x.org/?size=20&";
    public static final int EXPECTED_NUMBER_OF_AGGREGATIONS = 14;
    private static ResourceClient searchClient;
    private static IndexingClient indexingClient;

    @BeforeAll
    static void setUp() throws IOException, InterruptedException {
        container.start();

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);

        var mochedHttpClient = mock(HttpClient.class);
        var userSettingsClient = new UserSettingsClient(mochedHttpClient, cachedJwtProvider);
        var response = mockedHttpResponse("user_settings.json");
        when(mochedHttpClient.send(any(), any())).thenReturn(response);
        searchClient = new ResourceClient(HttpClient.newHttpClient(), userSettingsClient, cachedJwtProvider);

        createIndex();
        populateIndex();
        logger.info("Waiting {} ms for indexing to complete", DELAY_AFTER_INDEXING);
        Thread.sleep(DELAY_AFTER_INDEXING);
    }

    @AfterAll
    static void afterAll() throws IOException, InterruptedException {
        logger.info("Stopping container");
        indexingClient.deleteIndex(RESOURCES);
        Thread.sleep(DELAY_AFTER_INDEXING);
        container.stop();
    }

    @Nested
    class ResourceQueries {

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
        void shouldCheckFacets() throws BadRequestException {
            var hostAddress = URI.create(container.getHttpHostAddress());

            var uri1 = URI.create(REQUEST_BASE_URL);
            var query1 = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri1))
                .withOpensearchUri(hostAddress)
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);
            var response1 = searchClient.doSearch(query1);
            assertNotNull(response1);

            var uri2 =
                URI.create(REQUEST_BASE_URL +
                           "aggregation=entityDescription,associatedArtifacts,topLevelOrganizations,fundings,status,"
                           + "scientificIndex,hasPublicFile,license");
            var query2 = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri2))
                .withOpensearchUri(hostAddress)
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);
            var response2 = searchClient.doSearch(query2);
            assertNotNull(response2);

            assertEquals(response1.aggregations(), response2.aggregations());

            var aggregations = query1.toPagedResponse(response1).aggregations();

            assertFalse(aggregations.isEmpty());
            assertThat(aggregations.get(TYPE).size(), is(4));
            assertThat(aggregations.get(HAS_PUBLIC_FILE).get(0).count(), is(20));
            assertThat(aggregations.get(LICENSE).get(0).count(), is(15));
            assertThat(aggregations.get(FUNDING_SOURCE).size(), is(2));
            assertThat(aggregations.get(PUBLISHER).get(0).count(), is(3));
            assertThat(aggregations.get(CONTRIBUTOR).size(), is(12));
            assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION).size(), is(4));
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
            var searchClient = new ResourceClient(HttpClient.newHttpClient(), userSettingsClient,
                                                  setupMockedCachedJwtProvider());

            var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
            var response = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
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
            var searchClient = new ResourceClient(HttpClient.newHttpClient(), userSettingsClient,
                                                  setupMockedCachedJwtProvider());

            var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
            var response = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .doSearch(searchClient);

            assertNotNull(response);
        }

        @Test
        void userSettingsFailsIOException() throws IOException, InterruptedException, BadRequestException {
            var mochedHttpClient = mock(HttpClient.class);
            var userSettingsClient = new UserSettingsClient(mochedHttpClient, setupMockedCachedJwtProvider());
            when(mochedHttpClient.send(any(), any()))
                .thenThrow(new IOException("Not found"));
            var searchClient = new ResourceClient(HttpClient.newHttpClient(), userSettingsClient,
                                                  setupMockedCachedJwtProvider());

            var uri = URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254");
            var response = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE)
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .doSearch(searchClient);

            assertNotNull(response);
        }

        @Test
        void emptyResultShouldIncludeHits() throws BadRequestException {
            var uri = URI.create("https://x.org/?id=018b857b77b7");

            var pagedResult =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE)
                    .build()
                    .withRequiredStatus(NEW, DRAFT, PUBLISHED_METADATA, PUBLISHED, DELETED, UNPUBLISHED,
                                        DRAFT_FOR_DELETION)
                    .doSearch(searchClient);
            assertNotNull(pagedResult);
            assertTrue(pagedResult.contains("\"hits\":["));
        }

        @Test
        void withOrganizationDoWork() throws BadRequestException {
            var uri = URI.create("https://x.org/");
            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE)
                    .build()
                    .withRequiredStatus(PUBLISHED_METADATA, PUBLISHED)
                    .withOrganization(
                        URI.create("https://api.dev.nva.aws.unit.no/customer/bb3d0c0c-5065-4623-9b98-5810983c2478"));

            var response = searchClient.doSearch(query);
            assertNotNull(response);

            var pagedSearchResourceDto = query.toPagedResponse(response);
            assertEquals(2, pagedSearchResourceDto.totalHits());
        }

        @ParameterizedTest
        @MethodSource("uriPagingProvider")
        void searchWithUriPageableReturnsOpenSearchResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            assertThat(pagedSearchResourceDto.aggregations().size(),
                       is(equalTo(EXPECTED_NUMBER_OF_AGGREGATIONS)));
            logger.debug(pagedSearchResourceDto.id().toString());
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

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
        @MethodSource("uriProvider")
        void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
            var csvResult =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, AGGREGATION)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withMediaType(Words.TEXT_CSV)
                    .build()
                    .withRequiredStatus(PUBLISHED_METADATA)
                    .doSearch(searchClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, SORT, INSTANCE_TYPE, AGGREGATION)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            logger.info(query.getValue(SORT).toString());
            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);
            assertNotNull(pagedSearchResourceDto.id());
            assertNotNull(pagedSearchResourceDto.context());
            assertTrue(pagedSearchResourceDto.totalHits() >= 0);
        }

        @ParameterizedTest
        @MethodSource("uriInvalidProvider")
        void failToSearchUri(URI uri) {
            assertThrows(
                BadRequestException.class,
                () -> ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .doSearch(searchClient));
        }

        @Test
        void shouldReturnResourcesForScientificPeriods() throws BadRequestException {
            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(Map.of(SCIENTIFIC_REPORT_PERIOD_SINCE.fieldName(), "2019",
                                                SCIENTIFIC_REPORT_PERIOD_BEFORE.fieldName(), "2022"))
                    .withRequiredParameters(FROM, SIZE, AGGREGATION)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            logger.info(query.getValue(SORT).toString());
            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertThat(pagedSearchResourceDto.hits(), hasSize(2));
        }

        @Test
        void shouldReturnResourcesForSinglePeriods() throws BadRequestException {
            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(Map.of(SCIENTIFIC_REPORT_PERIOD_SINCE.fieldName(), "2019",
                                                SCIENTIFIC_REPORT_PERIOD_BEFORE.fieldName(), "2020"))
                    .withRequiredParameters(FROM, SIZE, AGGREGATION)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            logger.info(query.getValue(SORT).toString());
            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertThat(pagedSearchResourceDto.hits(), hasSize(1));
        }

        static Stream<Arguments> uriPagingProvider() {
            return Stream.of(
                createArgument("page=0&aggregation=all", 20),
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
                URI.create(REQUEST_BASE_URL + "category=Ma&sort=created_date&sortOrder=asc&sort=category&order=desc"),
                URI.create(REQUEST_BASE_URL + "category=Ma&sort=modified_date&sortOrder=asc&sort=category"),
                URI.create(REQUEST_BASE_URL + "category=Ma&sort=published_date&sortOrder=asc&sort=category"),
                URI.create(REQUEST_BASE_URL + "category=Ma&size=10&from=0&sort=modified_date"),
                URI.create(REQUEST_BASE_URL + "category=Ma&orderBy=UNIT_ID:asc,title:desc"),
                URI.create(REQUEST_BASE_URL
                           + "category=Ma&orderBy=created_date:asc,modifiedDate:desc&searchAfter=1241234,23412"),
                URI.create(REQUEST_BASE_URL + "category=Ma&sort=published_date+asc&sort=category+desc"));
        }

        static Stream<URI> uriInvalidProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL + "sort=epler"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY:DEdd"),
                URI.create(REQUEST_BASE_URL + "categories=hello+world&lang=en"),
                URI.create(REQUEST_BASE_URL + "tittles=hello+world&modified_before=2019-01-01"),
                URI.create(REQUEST_BASE_URL + "conttributors=hello+world&published_before=2020-01-01"),
                URI.create(REQUEST_BASE_URL + "category=PhdThesis&sort=beunited+asc"),
                URI.create(REQUEST_BASE_URL + "funding=NFR,296896"),
                URI.create(REQUEST_BASE_URL + "useers=hello+world&lang=en"));
        }

        static Stream<Arguments> uriProvider() {
            return loadMapFromResource(RESOURCE_VALID_TEST_URL_JSON).entrySet().stream()
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
        var jsonFile = stringFromResources(Path.of(SAMPLE_RESOURCES_SEARCH_JSON));
        var jsonNodes =
            attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

        jsonNodes.forEach(ResourceClientTest::addDocumentToIndex);
    }

    private static void addDocumentToIndex(JsonNode node) {
        try {
            var attributes = new EventConsumptionAttributes(RESOURCES, SortableIdentifier.next());
            indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createIndex() throws IOException {
        var mappingsJson = stringFromResources(Path.of(TEST_RESOURCES_MAPPINGS_JSON));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
        indexingClient.createIndex(RESOURCES, mappings);
    }

}