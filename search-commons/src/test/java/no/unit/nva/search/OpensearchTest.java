package no.unit.nva.search;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search2.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.stream.Stream;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchDocumentsQuery;
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

    public static final String TEST_RESOURCES_MAPPINGS = "mapping_test_resources.json";
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
