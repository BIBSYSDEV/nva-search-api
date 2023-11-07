package no.unit.nva.search2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.RestHighLevelClientWrapper;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search2.model.OpenSearchQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.model.OpenSearchQuery.queryToMapEntries;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class ResourceOpenSearchTest {


    protected static final Logger logger = LoggerFactory.getLogger(ResourceOpenSearchTest.class);
    public static final String TEST_RESOURCES_MAPPINGS = "test_resources_mappings.json";
    public static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.0.0";
    public static final long DELAY_AFTER_INDEXING = 1000L;
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    private static ResourceAwsClient searchClient;
    private static IndexingClient indexingClient;
    private static String indexName;

    @BeforeAll
    static void setUp() {
        container.start();
        System.setProperty("SEARCH_INFRASTRUCTURE_API_URI", container.getHttpHostAddress());

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
        searchClient = new ResourceAwsClient(cachedJwtProvider, HttpClient.newHttpClient());
        indexName = generateIndexName();

    }

    @AfterAll
    static void afterAll() throws IOException {
        container.stop();
    }

    @Nested
    class AddDocumentToIndexTest {

        @BeforeEach
        void setUp() throws IOException, InterruptedException {
            var mappingsJson = stringFromResources(Path.of(TEST_RESOURCES_MAPPINGS));
            var type = new TypeReference<Map<String, Object>>() {
            };
            var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
            indexingClient.createIndex(indexName, mappings);
            addDocumentsToIndex("sample_opensearch_response.json");
        }

        @AfterEach
        void tearDown() throws IOException {
            indexingClient.deleteIndex(indexName);
        }


        @Test
        void shoulCheckMapping() {


            var mapping = indexingClient.getMapping(indexName);
            assertThat(mapping, is(notNullValue()));
            var topLevelOrgType = mapping.path("properties").path("topLevelOrganizations").path("type").textValue();
            assertThat(topLevelOrgType, is(equalTo("nested")));

            logger.info(mapping.asText());

        }


        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var pagedSearchResourceDto =
                ResourceAwsQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearcUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .doSearch(searchClient);

            assertNotNull(pagedSearchResourceDto);
            logger.info("{}", pagedSearchResourceDto);
        }

        static Stream<URI> uriProvider() {
            final var hostAddress = "https://api.example.com";
            return Stream.of(
                URI.create(hostAddress + "/?size=3"),
                URI.create(hostAddress + "/?fields=category,title,created_date&query=Kjetil+Møkkelgjerd&size=2"),
                URI.create(hostAddress + "/?query=Kjetil+Møkkelgjerd&fields=CONTRIBUTOR&size=2"),
                URI.create(hostAddress + "/?CONTRIBUTOR=Kjetil+Møkkelgjerd&size=2"),
                URI.create(hostAddress + "/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254&size=2"),
                URI.create(hostAddress + "/?CONTRIBUTOR_ID=https://api.dev.nva.aws.unit" +
                    ".no/cristin/person/1136254&size=2"),
                URI.create(hostAddress + "/?CONTRIBUTOR_SHOULD="
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136254+"
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1135555&size=2"),
                URI.create(hostAddress + "/?CONTRIBUTOR_NOT="
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136254+"
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1135555&size=2"),
                URI.create(hostAddress + "/?category=ReportResearch&page=0&user=12%203&size=2"),
                URI.create(hostAddress + "/?category=ReportResearch&page=2&size=2"),
                URI.create(hostAddress + "/?category=ReportResearch&offset=2"),
                URI.create(hostAddress + "/?category=ReportResearch&from=2&results=2"),
                URI.create(hostAddress + "/?published_before=2020-01-01&size=2"),
                URI.create(hostAddress + "/?published_since=2019-01-01&institution=uib&funding_source=NFR&size=2"));
        }

    }

    private IndexDocument crateSampleIndexDocument(String indexName, String jsonFile) throws IOException {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
            indexName,
            SortableIdentifier.next()
        );
        var jsonNode = objectMapperWithEmpty.readValue(inputStreamFromResources(jsonFile),
            JsonNode.class);

        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }

    private void addDocumentsToIndex(String... files) throws InterruptedException {
        Stream.of(files)
            .forEach(file -> attempt(
                () -> indexingClient.addDocumentToIndex(crateSampleIndexDocument(indexName, file))));
        Thread.sleep(DELAY_AFTER_INDEXING);
    }

    private static String generateIndexName() {
        return "resources";
    }


}
