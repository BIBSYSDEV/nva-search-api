package no.unit.nva.search2;

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
import no.unit.nva.search2.importcandidate.ImportCandidateParameter;
import no.unit.nva.search2.importcandidate.ImportCandidateQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.CREATED_DATE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportCandidateClientTest {

    protected static final Logger logger = LoggerFactory.getLogger(ImportCandidateClientTest.class);
    public static final String SAMPLE_IMPORT_CANDIDATES = "datasource_import_candidates.json";
    public static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.0.0";
    public static final long DELAY_AFTER_INDEXING = 1000L;
    private static final String REQUEST_BASE_URL = "https://x.org/?";
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
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
    class ImportCandidateTest {

        @Test
        void openSearchFailedResponse() throws IOException, InterruptedException {
            HttpClient httpClient = mock(HttpClient.class);
            var response = mock(HttpResponse.class);
            when(httpClient.send(any(), any())).thenReturn(response);
            when(response.statusCode()).thenReturn(500);
            when(response.body()).thenReturn("EXPECTED ERROR");
            var toMapEntries = queryToMapEntries(URI.create("https://example.com/?size=2"));
            var importCandidateClient = new ImportCandidateClient(httpClient, setupMockedCachedJwtProvider());

            assertThrows(
                RuntimeException.class,
                () -> ImportCandidateQuery.builder()
                    .withRequiredParameters(SIZE, FROM)
                    .fromQueryParameters(toMapEntries).build()
                    .doSearch(importCandidateClient)
            );
        }



        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ImportCandidateQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE, SORT)
                    .build();

            var swsResponse = importCandidateClient.doSearch(query);
            var pagedResponse = query.toPagedResponse(swsResponse);

            assertNotNull(pagedResponse);
            assertThat(pagedResponse.hits().size(), is(equalTo(query.getValue(ImportCandidateParameter.SIZE).as())));
            assertThat(pagedResponse.totalHits(), is(equalTo(query.getValue(ImportCandidateParameter.SIZE).as())));
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
            var csvResult = ImportCandidateQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE, SORT)
                .withMediaType(Words.TEXT_CSV)
                .build()
                .doSearch(importCandidateClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ImportCandidateQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withRequiredParameters(FROM, SIZE, SORT)
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
                         () -> ImportCandidateQuery.builder()
                             .fromQueryParameters(queryToMapEntries(uri))
                             .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                             .withRequiredParameters(FROM, SIZE)
                             .build()
                             .doSearch(importCandidateClient));
        }

        @ParameterizedTest
        @MethodSource("uriInvalidProvider")
        void failToSetREQUIRED(URI uri) {
            assertThrows(BadRequestException.class,
                         () -> ImportCandidateQuery.builder()
                             .fromQueryParameters(queryToMapEntries(uri))
                             .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                             .withRequiredParameters(FROM, SIZE, CREATED_DATE)
                             .build()
                             .doSearch(importCandidateClient));
        }

        static Stream<URI> uriSortingProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL
                           + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date&order=desc"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&size=10&from=0&sort=created_date"),
                URI.create(REQUEST_BASE_URL
                           + "category=AcademicArticle&orderBy=INSTANCE_TYPE:asc,PUBLICATION_YEAR:desc"),
                URI.create(REQUEST_BASE_URL
                           + "category=AcademicArticle&orderBy=title:asc,CREATED_DATE:desc&searchAfter=1241234,23412"),
                URI.create(REQUEST_BASE_URL + "category=AcademicArticle&sort=TYPE+asc&sort=INSTANCE_TYPE+desc"));
        }

        static Stream<URI> uriProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL + "ADDITIONAL_IDENTIFIERS=2-s2.0-85160834649&size=1"),
                URI.create(REQUEST_BASE_URL + "ADDITIONAL_IDENTIFIERS_NOT=2-s2.0-85160834649&size=7"),
                URI.create(REQUEST_BASE_URL +
                           "ADDITIONAL_IDENTIFIERS_SHOULD=2-s2.0-85160834649,2-s2.0-85168575107&size=2"),
                URI.create(REQUEST_BASE_URL + "CATEGORY=AcademicArticle&size=5"),
                URI.create(REQUEST_BASE_URL + "CATEGORY_NOT=AcademicArticle&size=3"),
                URI.create(REQUEST_BASE_URL + "CATEGORY_SHOULD=AcademicArticle&size=5"),
                URI.create(REQUEST_BASE_URL + "COLLABORATION_TYPE=Collaborative&size=3"),
                URI.create(REQUEST_BASE_URL + "COLLABORATION_TYPE_NOT=Collaborative&size=5"),
                URI.create(REQUEST_BASE_URL + "COLLABORATION_TYPE_SHOULD=Collaborative&size=3"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR=Andrew+Morrison&size=1"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_NOT=George+Rigos&size=7"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_SHOULD=Andrew+Morrison,George+Rigos&size=2"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_NAME=Andrew+Morrison&size=1"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_NAME_NOT=George+Rigos&size=7"),
                URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_NAME_SHOULD=Andrew+Morrison,George+Rigos&size=2"),
                URI.create(REQUEST_BASE_URL + "CREATED_DATE=2023-11-20&size=3"),
                URI.create(REQUEST_BASE_URL + "CRISTIN_IDENTIFIER=3212342&size=1"),
                URI.create(REQUEST_BASE_URL + "DOI=https://doi.org/10.1117/1.OE.60.3.036102&size=1"),
                URI.create(REQUEST_BASE_URL + "DOI_NOT=https://doi.org/10.1117/1.OE.60.3.036102&size=7"),
                URI.create(REQUEST_BASE_URL + "DOI_SHOULD=https://doi.org/10.1117/1.OE.60.3.036102&size=1"),
                // URI.create(REQUEST_BASE_URL + "ID=018bee3ddae4-653812a8-ed19-469b-8078-c3b488f71f74&size=1"),
                URI.create(REQUEST_BASE_URL +
                           "ID=https://api.dev.nva.aws.unit.no/publication/import-candidate/018bee3ddae4-653812a8"
                           + "-ed19-469b-8078-c3b488f71f74&size=1"),
                URI.create(REQUEST_BASE_URL +
                           "ID_NOT=https://api.dev.nva.aws.unit.no/publication/import-candidate/018bee3ddae4-653812a8"
                           + "-ed19-469b-8078-c3b488f71f74&size=7"),
                //                URI.create(REQUEST_BASE_URL +
                //                "id_should=018bee3ddae4-653812a8-ed19-469b-8078-c3b488f71f74&&size=1"),
                URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=NOT_IMPORTED&size=4"),
                URI.create(REQUEST_BASE_URL + "IMPORT_STATUS_NOT=NOT_IMPORTED&size=4"),
                URI.create(REQUEST_BASE_URL + "IMPORT_STATUS_SHOULD=NOT_IMPORTED&size=4"),
                URI.create(REQUEST_BASE_URL + "INSTANCE_TYPE=AcademicArticle&size=5"),
                URI.create(REQUEST_BASE_URL + "INSTANCE_TYPE_NOT=AcademicArticle&size=3"),
                URI.create(REQUEST_BASE_URL + "INSTANCE_TYPE_SHOULD=AcademicArticle,AcademicLiteratureReview&size=6"),
                URI.create(REQUEST_BASE_URL + "PUBLICATION_YEAR=2022&size=1"),
                URI.create(REQUEST_BASE_URL + "PublicationYearBefore=2024&publication_year_since=2023&size=3"),
                URI.create(REQUEST_BASE_URL + "PUBLICATION_YEAR_BEFORE=2023&size=5"),
                URI.create(REQUEST_BASE_URL + "PUBLICATION_YEAR_SINCE=2023&size=3"),
                URI.create(REQUEST_BASE_URL + "query=antibacterial&fields=category,TITLE&size=1"),
                URI.create(REQUEST_BASE_URL + "query=antibacterial&fields=category,TITLE,werstfg&ID_NOT=123&size=1"),
                URI.create(REQUEST_BASE_URL + "query=European&fields=all&size=3"),
                URI.create(REQUEST_BASE_URL + "SCOPUS_IDENTIFIER=3212342&size=1"),
                URI.create(REQUEST_BASE_URL + "TITLE=chronic+diseases&size=1"),
                URI.create(REQUEST_BASE_URL + "TITLE=In+reply:+Why+big+data&size=1"),
                URI.create(REQUEST_BASE_URL + "TITLE_NOT=antibacterial,Fishing&size=6"),
                URI.create(REQUEST_BASE_URL + "TITLE_SHOULD=antibacterial,Fishing&size=2"),
                URI.create(REQUEST_BASE_URL + "TYPE=ImportCandidateSummary&size=8")
            );
        }

        static Stream<URI> uriInvalidProvider() {
            return Stream.of(
                URI.create(REQUEST_BASE_URL + "size=7&sort="),
                URI.create(REQUEST_BASE_URL + "query=European&fields"),
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
        var mappingsJson = stringFromResources(Path.of("opensearch_test_mapping_import_candidates.json"));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
        indexingClient.createIndex(IMPORT_CANDIDATES_INDEX, mappings);
    }
}