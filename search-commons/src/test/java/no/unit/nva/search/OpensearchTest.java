package no.unit.nva.search;

import static java.util.Objects.isNull;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.SearchClient.DOCUMENT_TYPE;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.ID_FIELD;
import static no.unit.nva.search.SearchClient.ORGANIZATION_FIELD;
import static no.unit.nva.search.SearchClient.PART_OF_FIELD;
import static no.unit.nva.search.SearchClient.TICKET_STATUS;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search2.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.opensearch.search.sort.SortOrder.DESC;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.RestClient;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OpensearchTest {

    public static final String STATUS_TO_INCLUDE_IN_RESULT = "Pending";
    public static final String TEST_RESOURCES_MAPPINGS = "mapping_test_resources.json";
    public static final String TEST_IMPORT_CANDIDATES_MAPPINGS = "mapping_test_import_candidates.json";
    private static final int SAMPLE_NUMBER_OF_RESULTS = 7;
    private static final int SAMPLE_FROM = 0;
    private static final String SAMPLE_ORDERBY = "orderByField";
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    private static final String SEARCH_ALL = "*";
    private static SearchClient searchClient;
    private static IndexingClient indexingClient;
    private static String indexName;

    @BeforeAll
    static void setUp() {
        container.start();

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);

        var cachedJwtProvider = setupMockedCachedJwtProvider();

        searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
    }

    @AfterAll
    static void afterAll() {
        container.stop();
    }


    void assertAggregation(JsonNode aggregationNode, String key, int expectedDocCount) {
        AtomicBoolean found = new AtomicBoolean(false);
        aggregationNode.forEach(
            bucketNode -> {
                if (bucketNode.get("key").asText().equals(key)) {
                    found.set(true);
                    assertThat(bucketNode.get("docCount").asInt(), is(equalTo(expectedDocCount)));
                }
            }
        );
        assertThat(found.get(), is(equalTo(true)));
    }

    private String generateIndexName() {
        return RandomDataGenerator.randomString().toLowerCase();
    }

    private IndexDocument getTicketIndexDocument(String indexName, URI organization, List<URI> partOf) {
        return getTicketIndexDocument(indexName, organization, partOf, STATUS_TO_INCLUDE_IN_RESULT);
    }

    private IndexDocument getTicketIndexDocument(String indexName, URI organization, List<URI> partOf, String status) {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
            indexName,
            SortableIdentifier.next()
        );
        Map<String, Object> map = Map.of(
            ORGANIZATION_FIELD, Map.of(
                ID_FIELD, isNull(organization) ? "" : organization.toString(),
                "test", "test2",
                PART_OF_FIELD, isNull(partOf) ? List.of() : partOf.stream().map(URI::toString).toList()
            ),
            DOCUMENT_TYPE, DOI_REQUEST,
            TICKET_STATUS, status
        );
        var jsonNode = objectMapperWithEmpty.convertValue(map, JsonNode.class);
        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }

    private IndexDocument crateSampleIndexDocument(String indexName, String jsonFile) throws IOException {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
            indexName,
            SortableIdentifier.next()
        );
        var jsonNode = objectMapperWithEmpty
            .readValue(inputStreamFromResources(jsonFile), JsonNode.class);

        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }

    private void addDocumentsToIndex(String... files) throws InterruptedException {
        Stream.of(files)
            .forEach(file -> attempt(
                () -> indexingClient.addDocumentToIndex(crateSampleIndexDocument(indexName, file))));
        Thread.sleep(DELAY_AFTER_INDEXING);
    }

    private SearchDocumentsQuery queryWithTermAndAggregation(
        String searchTerm, List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations) {
        return new SearchDocumentsQuery(
            searchTerm,
            SAMPLE_NUMBER_OF_RESULTS,
            SAMPLE_FROM,
            SAMPLE_ORDERBY,
            DESC,
            SAMPLE_REQUEST_URI,
            aggregations
        );
    }

    private static int getDocCountForAggregation(SearchResponseDto searchResponseDto, String aggregationName) {
        return searchResponseDto.getAggregations().get(aggregationName).get("docCount").asInt();
    }

    @Nested
    class ImportCandidateIndexTest {

        @BeforeEach
        void beforeEachTest() throws IOException {
            indexName = generateIndexName();

            var mappingsJson = stringFromResources(Path.of(TEST_IMPORT_CANDIDATES_MAPPINGS));
            var type = new TypeReference<Map<String, Object>>() {
            };
            var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
            indexingClient.createIndex(indexName, mappings);
        }

        @Test
        void shouldReturnCorrectAggregationsForImportCandidates()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("import_candidate.json",
                "import_candidate_with_not_imported.json",
                "import_candidate_with_not_applicable.json");

            var query = queryWithTermAndAggregation(SEARCH_ALL, IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            assertThat(response.getAggregations(), is(not(emptyIterable())));
        }

        @Test
        void shouldReturnAssociatedArtifactAggregationWithSingleDocCount()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("import_candidate.json", "import_candidate_with_not_imported.json");

            var query = queryWithTermAndAggregation(SEARCH_ALL, IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            var docCount = getDocCountForAggregation(response, "associatedArtifacts");
            assertThat(docCount, is(equalTo(1)));
        }

        @Test
        void shouldReturnInstanceTypeAggregationWithDocCountTwo()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("import_candidate.json", "import_candidate_with_not_imported.json");

            var query = queryWithTermAndAggregation(SEARCH_ALL, IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            var docCount = response.getAggregations().get("instanceType").get("buckets").get(0).get("docCount").asInt();
            assertThat(docCount, is(equalTo(1)));
        }

        @Test
        void shouldFilterDocumentsWithFiles()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("import_candidate.json", "import_candidate_with_not_imported.json");

            var query = queryWithTermAndAggregation(
                "(associatedArtifacts.type:\"PublishedFile\")AND(associatedArtifacts"
                    + ".administrativeAgreement:\"false\")", IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response.getHits().size(), is(equalTo(1)));
        }

        @Test
        void shouldQueryPublicationsWithMultipleOrganizations()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("import_candidate.json", "import_candidate_with_not_imported.json");

            var searchTerm = "collaborationType:\"Collaborative\"";
            var query = queryWithTermAndAggregation(searchTerm, IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            assertThat(response.getHits(), hasSize(1));
        }
    }

    @Nested
    class AddDocumentToIndexTest {

        @AfterEach
        void afterEachTest() throws Exception {
            indexingClient.deleteIndex(indexName);
        }

        @Nested
        class ResourcesTests {

            @BeforeEach
            void beforeEachTest() throws IOException {
                indexName = generateIndexName();

                var mappingsJson = stringFromResources(Path.of(TEST_RESOURCES_MAPPINGS));
                var type = new TypeReference<Map<String, Object>>() {
                };
                var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
                indexingClient.createIndex(indexName, mappings);
            }

            @Test
            void shouldNotReturnAggregationsWhenNotRequested()
                throws ApiGatewayException, InterruptedException {

                addDocumentsToIndex("publication_draft_publishing_request.json",
                    "publication_published_publishing_request.json");

                var query = queryWithTermAndAggregation(SEARCH_ALL, null);

                var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

                assertThat(response, notNullValue());
                assertThat(response.getAggregations(), nullValue());
            }


            @ParameterizedTest()
            @ValueSource(strings = {"navnesen", "navn", "navn+navnesen"})
            void shouldReturnHitsWhenSearchedForPartianMatchOfCuratorName(String queryStr) throws Exception {
                addDocumentsToIndex("publication.json");

                var query = queryWithTermAndAggregation(queryStr, null);

                var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
                assertThat(response.getHits(), hasSize(1));
            }
        }

    }
}
