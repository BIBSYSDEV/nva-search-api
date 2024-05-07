package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search2.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search2.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.EQUAL;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.AGGREGATION;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.COLLABORATION_TYPE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.CONTRIBUTOR;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.CREATED_DATE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.FILES;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.IMPORT_STATUS;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.PUBLICATION_YEAR;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.TYPE;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
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
import no.unit.nva.search2.importcandidate.ImportCandidateClient;
import no.unit.nva.search2.importcandidate.ImportCandidateSearchQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ImportCandidateClientTest {

    protected static final Logger logger = LoggerFactory.getLogger(ImportCandidateClientTest.class);
    public static final String SAMPLE_IMPORT_CANDIDATES = "datasource_import_candidates.json";
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    public static final String REQUEST_BASE_URL = "https://example.com/?";
    private static IndexingClient indexingClient;
    private static ImportCandidateClient importCandidateClient;

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException {
        container.start();

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
        importCandidateClient = new ImportCandidateClient(HttpClient.newHttpClient(), cachedJwtProvider);

        createIndex();
        populateIndex();
        logger.info("Waiting {} ms for indexing to complete", DELAY_AFTER_INDEXING);
        Thread.sleep(DELAY_AFTER_INDEXING);
    }

    @AfterAll
    static void afterAll() throws IOException, InterruptedException {
        logger.info("Stopping container");
        indexingClient.deleteIndex(IMPORT_CANDIDATES_INDEX);
        Thread.sleep(DELAY_AFTER_INDEXING);
        container.stop();
    }

    @Nested
    class NestedTests {

        @Test
        void shouldCheckFacets() throws BadRequestException {
            var hostAddress = URI.create(container.getHttpHostAddress());
            var uri1 = URI.create(REQUEST_BASE_URL + AGGREGATION.asCamelCase() + EQUAL + ALL);

            var candidateQuery = ImportCandidateSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri1))
                .withDockerHostUri(hostAddress)
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .build();
            var response1 = importCandidateClient.doSearch(candidateQuery);

            assertNotNull(response1);

            var aggregations = candidateQuery.toPagedResponse(response1).aggregations();

            assertFalse(aggregations.isEmpty());
            assertThat(aggregations.get(IMPORT_STATUS.asCamelCase()).size(), is(2));
            assertThat(aggregations.get(CONTRIBUTOR.asCamelCase()).size(), is(5));
            assertThat(aggregations.get(COLLABORATION_TYPE.asCamelCase()).size(), is(2));
            assertThat(aggregations.get(FILES.asCamelCase()).get(0).count(), is(7));
            assertThat(aggregations.get(PUBLICATION_YEAR.asCamelCase()).size(), is(4));
            assertThat(aggregations.get(TYPE.asCamelCase()).size(), is(4));
            assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION.asCamelCase()).size(), is(9));
            assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION.asCamelCase()).get(1).labels().get("nb"),
                is(equalTo("Universitetet i Bergen")));
        }

        @Test
        void openSearchFailedResponse() throws IOException, InterruptedException {
            HttpClient httpClient = mock(HttpClient.class);
            var response = mock(HttpResponse.class);
            when(httpClient.send(any(), any())).thenReturn(response);
            when(response.statusCode()).thenReturn(500);
            when(response.body()).thenReturn("EXPECTED ERROR");
            var toMapEntries = queryToMapEntries(URI.create(REQUEST_BASE_URL + "size=2"));
            var importCandidateClient = new ImportCandidateClient(httpClient, setupMockedCachedJwtProvider());

            assertThrows(
                RuntimeException.class,
                () -> ImportCandidateSearchQuery.builder()
                    .withRequiredParameters(SIZE, FROM)
                    .fromQueryParameters(toMapEntries)
                    .build()
                    .doSearch(importCandidateClient)
            );
        }


        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ImportCandidateSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE)
                    .build();

            var swsResponse = importCandidateClient.doSearch(query);
            var pagedResponse = query.toPagedResponse(swsResponse);

            assertNotNull(pagedResponse);
            assertThat(pagedResponse.hits().size(), is(equalTo(query.parameters().get(SIZE).as())));
            assertThat(pagedResponse.totalHits(), is(equalTo(query.parameters().get(SIZE).as())));
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        @Disabled("Does not work. When test was written it returned an empty string even if there were supposed to be"
                  + " hits. Now we throw an exception instead as the method is not implemented.")
        void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
            var csvResult = ImportCandidateSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE)
                .withMediaType(Words.TEXT_CSV)
                .build()
                .doSearch(importCandidateClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ImportCandidateSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE)
                    .build();

            var response = importCandidateClient.doSearch(query);
            var pagedResponse = query.toPagedResponse(response);
            assertNotNull(pagedResponse.id());
            assertNotNull(pagedResponse.context());
            assertTrue(pagedResponse.id().getScheme().contains("https"));
        }

        @ParameterizedTest
        @MethodSource("uriInvalidProvider")
        void failToSearchUri(URI uri) {
            assertThrows(BadRequestException.class,
                () -> ImportCandidateSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE, AGGREGATION)
                    .build()
                    .doSearch(importCandidateClient));
        }

        @ParameterizedTest
        @MethodSource("uriInvalidProvider")
        void failToSetRequired(URI uri) {
            assertThrows(BadRequestException.class,
                () -> ImportCandidateSearchQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE, CREATED_DATE)
                    .build()
                    .doSearch(importCandidateClient));
        }

        static Stream<URI> uriSortingProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL + "sort=title&sortOrder=asc&sort=created_date&order=desc"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&size=10&from=0&sort=created_date"),
                URI.create(REQUEST_BASE_URL + "orderBy=INSTANCE_TYPE:asc,PUBLICATION_YEAR:desc"),
                URI.create(REQUEST_BASE_URL + "orderBy=title:asc,CREATED_DATE:desc&searchAfter=3.34,1241234,23412"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&sort=TYPE+asc&sort=INSTANCE_TYPE+desc"));
        }

        static Stream<URI> uriProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL + "size=8"),
                URI.create(REQUEST_BASE_URL + "aggregation=ALL&size=8"),
                URI.create(REQUEST_BASE_URL + "aggregation=importStatus&size=8"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&size=5"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR=Andrew+Morrison&size=1"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR=Andrew+Morrison,Nina+Bjørnstad&size=1"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_NOT=George+Rigos&size=7"),
                URI.create(REQUEST_BASE_URL + "files=hasPublicFiles&size=7"),
                URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=IMPORTED&size=4"),
                URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=1136326@20754.0.0.0&size=2"),
                URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=20754.0.0.0&size=4"),
                URI.create(REQUEST_BASE_URL + "id=018bed744c78-f53e06f7-74da-4c91-969f-ec307a7e7816&size=1"),
                URI.create(REQUEST_BASE_URL + "MODIFIED_DATE=2023&size=8"),
                URI.create(REQUEST_BASE_URL + "MODIFIED_DATE=2023-10&size=2"),
                URI.create(REQUEST_BASE_URL + "MODIFIED_DATE=2023-05,&size=7"),
                URI.create(REQUEST_BASE_URL + "PUBLICATION_YEAR_BEFORE=2023&size=5"),
                URI.create(REQUEST_BASE_URL + "PublicationYearBefore=2024&publication_year_since=2023&size=3"),
                URI.create(REQUEST_BASE_URL + "publicationYear=2022,2022&size=1"),
                URI.create(REQUEST_BASE_URL + "publicationYear=2022&size=1"),
                URI.create(REQUEST_BASE_URL + "publicationYear=,2022&size=5"),
                URI.create(REQUEST_BASE_URL + "publicationYear=2022,&size=4"),
                URI.create(REQUEST_BASE_URL + "publicationYear=2023&size=3"),
                URI.create(REQUEST_BASE_URL + "publicationYear=2022,2023&size=4"),
                URI.create(REQUEST_BASE_URL + "title=In+reply:+Why+big+data&size=1"),
                URI.create(REQUEST_BASE_URL + "title=chronic+diseases&size=1"),
                URI.create(REQUEST_BASE_URL + "title=antibacterial,Fishing&size=2"),
                URI.create(REQUEST_BASE_URL + "query=antibacterial&fields=category,title&size=1"),
                URI.create(REQUEST_BASE_URL + "query=antibacterial&fields=category,title,werstfg&ID_NOT=123&size=1"),
                URI.create(REQUEST_BASE_URL + "query=European&fields=all&size=3"),
                URI.create(REQUEST_BASE_URL + "CRISTIN_IDENTIFIER=3212342&size=1"),
                URI.create(REQUEST_BASE_URL + "SCOPUS_IDENTIFIER=3212342&size=1"));
        }

        static Stream<URI> uriInvalidProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL + "size=7&sort="),
                URI.create(REQUEST_BASE_URL + "query=European&fields"),

                URI.create(REQUEST_BASE_URL + "feilName=epler"),
                URI.create(REQUEST_BASE_URL + "query=epler&fields=feilName"),
                URI.create(REQUEST_BASE_URL + "CREATED_DATE=epler"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY:DEdd"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORdfgY:desc"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY"),
                URI.create(REQUEST_BASE_URL + "sort=CATEGORY:asc:DEdd"),

                URI.create(REQUEST_BASE_URL + "size=8&sort=epler"),
                URI.create(REQUEST_BASE_URL + "size=8&sort=type:DEdd"),
                URI.create(REQUEST_BASE_URL + "categories=hello+world"),
                URI.create(REQUEST_BASE_URL + "tittles=hello+world&modified_before=2019-01"),
                URI.create(REQUEST_BASE_URL + "conttributors=hello+world&PUBLICATION_YEAR_BEFORE=2020-01-01"),
                URI.create(REQUEST_BASE_URL + "category=PhdThesis&sort=beunited+asc"),
                URI.create(REQUEST_BASE_URL + "funding=NFR,296896"),
                URI.create(REQUEST_BASE_URL + "useers=hello+world"));
        }
    }

    protected static void populateIndex() {
        var jsonFile = stringFromResources(Path.of(SAMPLE_IMPORT_CANDIDATES));
        var jsonNodes =
            attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

        jsonNodes.forEach(node -> {
            try {
                var attributes = new EventConsumptionAttributes(IMPORT_CANDIDATES_INDEX, SortableIdentifier.next());
                indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected static void createIndex() throws IOException {
        var mappingsJson = stringFromResources(Path.of("mapping_test_import_candidates.json"));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
        indexingClient.createIndex(IMPORT_CANDIDATES_INDEX, mappings);
    }
}