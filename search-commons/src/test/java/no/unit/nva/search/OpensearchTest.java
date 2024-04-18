package no.unit.nva.search;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
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
import static org.opensearch.search.sort.SortOrder.DESC;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClient;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OpensearchTest {

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


    private String generateIndexName() {
        return RandomDataGenerator.randomString().toLowerCase();
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

}
