package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search2.common.QueryTools.queryToMapEntries;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.type.TypeReference;
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
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ImportCandidateParameter;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ImportCandidateClientTest {

    protected static final Logger logger = LoggerFactory.getLogger(ImportCandidateClientTest.class);
    public static final String SAMPLE_IMPORT_CANDIDATES = "sample_import_candidate_search.json";
    public static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.0.0";
    public static final long DELAY_AFTER_INDEXING = 1000L;
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
        void searchWithUriReturnsCSVResponse(URI uri) throws ApiGatewayException {

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

        static Stream<URI> uriSortingProvider() {
            return Stream.of(
                URI.create(
                    "https://example.com/?category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date&order"
                    + "=desc"),
                URI.create("https://example.com/?category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
                URI.create("https://example.com/?category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
                URI.create("https://example.com/?category=AcademicArticle&size=10&from=0&sort=created_date"),
                URI.create(
                    "https://example.com/?category=AcademicArticle&orderBy=INSTANCE_TYPE:asc,PUBLICATION_YEAR:desc"),
                URI.create("https://example.com/?category=AcademicArticle&orderBy=title:asc,"
                           + "CREATED_DATE:desc&searchAfter=1241234,23412"),
                URI.create("https://example.com/?category=AcademicArticle&sort=TYPE+asc&sort=INSTANCE_TYPE+desc"));
        }

        static Stream<URI> uriProvider() {
            return Stream.of(
                URI.create("https://example.com/?size=8"),
                URI.create("https://example.com/?category=AcademicArticle&size=5"),
                URI.create("https://example.com/?CONTRIBUTOR=Andrew+Morrison&size=1"),
                URI.create("https://example.com/?CONTRIBUTOR_SHOULD=Andrew+Morrison,George+Rigos&size=2"),
                URI.create("https://example.com/?CONTRIBUTOR_NOT=George+Rigos&size=7"),
                URI.create("https://example.com/?PUBLICATION_YEAR_BEFORE=2023&size=5"),
                URI.create("https://example.com/?publication_year=2022&size=1"),
                URI.create("https://example.com/?PublicationYearBefore=2024&publication_year_since=2023&size=3"),
                URI.create("https://example.com/?title=In+reply:+Why+big+data&size=1"),
                URI.create("https://example.com/?title=chronic+diseases&size=1"),
                URI.create("https://example.com/?title_should=antibacterial+Fishing&size=2"),
                URI.create("https://example.com/?query=antibacterial&fields=category,title&size=1"),
                URI.create("https://example.com/?query=antibacterial&fields=category,title,werstfg&ID_NOT=123&size=1"),
                URI.create("https://example.com/?query=European&fields=all&size=3"));
        }

        static Stream<URI> uriInvalidProvider() {
            return Stream.of(
                URI.create("https://example.com/?categories=hello+world"),
                URI.create("https://example.com/?tittles=hello+world&modified_before=2019-01"),
                URI.create("https://example.com/?conttributors=hello+world&PUBLICATION_YEAR_BEFORE=2020-01-01"),
                URI.create("https://example.com/?category=PhdThesis&sort=beunited+asc"),
                URI.create("https://example.com/?funding=NFR,296896"),
                URI.create("https://example.com/?useers=hello+world"));
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
        var mappingsJson = stringFromResources(Path.of("test_import_candidates_mappings.json"));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
        indexingClient.createIndex(IMPORT_CANDIDATES_INDEX, mappings);
    }
}