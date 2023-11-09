package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.model.OpenSearchQuery.queryToMapEntries;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import nva.commons.apigateway.exceptions.ApiGatewayException;
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
import org.testcontainers.junit.jupiter.Testcontainers;

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
    static void setUp() throws IOException, InterruptedException {
        container.start();
        System.setProperty("SEARCH_INFRASTRUCTURE_API_URI", container.getHttpHostAddress());

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
        searchClient = new ResourceAwsClient(cachedJwtProvider, HttpClient.newHttpClient());
        indexName = generateIndexName();

        createIndex();
        populateIndex();
        logger.info("Waiting {} ms for indexing to complete", DELAY_AFTER_INDEXING);
        Thread.sleep(DELAY_AFTER_INDEXING);
    }

    @AfterAll
    static void afterAll() throws IOException, InterruptedException {
        logger.info("Stopping container");
        indexingClient.deleteIndex(indexName);
        Thread.sleep(DELAY_AFTER_INDEXING);
        container.stop();
    }

    @Nested
    class AddDocumentToIndexTest {

        @Test
        void shoulCheckMapping() {


            var mapping = indexingClient.getMapping(indexName);
            assertThat(mapping, is(notNullValue()));
            var topLevelOrgType = mapping.path("properties").path("topLevelOrganizations").path("type").textValue();
            assertThat(topLevelOrgType, is(equalTo("nested")));

            logger.info(mapping.toPrettyString());

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
                URI.create(hostAddress + "/?query=https://api.dev.nva.aws.unit" +
                    ".no/cristin/person/1136918&fields=CONTRIBUTOR&size=4"),
                URI.create(hostAddress + "/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254&size=2"),
                URI.create(hostAddress + "/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=2"),

                URI.create(hostAddress + "/?CONTRIBUTOR=Isar+Kristoffer+Buzza,Kjetil+Møkkelgjerd&size=10"),
                URI.create(hostAddress + "/?CONTRIBUTOR="
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=10"),
                URI.create(hostAddress + "/?CONTRIBUTOR_SHOULD="
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=10"),
                URI.create(hostAddress + "/?CONTRIBUTOR_NOT="
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136254,+"
                    + "https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=2"),
                URI.create(hostAddress + "/?category=ReportResearch&page=0&size=2"),
                URI.create(hostAddress + "/?category=ReportResearch,AcademicArticle&page=2&size=2"),
                URI.create(hostAddress + "/?category=AcademicArticle&offset=2"),
                URI.create(hostAddress + "/?category=ReportResearch&from=2&results=2"),
                URI.create(hostAddress + "/?published_since=2023-09-01&size=2"),
                URI.create(hostAddress + "/?funding_source=Research+Council+of+Norway+(RCN)&size=2"),
                URI.create(hostAddress + "/?funding=NFR:296896&size=2"),
                URI.create(hostAddress + "/?funding=AFR:296896&size=2"),
                URI.create(hostAddress + "/?funding=NFR:1296896&size=2"),
                URI.create(hostAddress
                    + "/?query=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"
                    + "&size=2"),
                URI.create(hostAddress
                    + "/?query=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0&fields=INSTITUTION"
                    + "&size=2"),
                URI.create(hostAddress
                    + "/?INSTITUTION=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"
                    + "&size=2"),
                URI.create(hostAddress + "/?INSTITUTION=20754&size=2"),
                URI.create(hostAddress + "/?INSTITUTION_SHOULD=20754&size=2"),
                URI.create(hostAddress + "/?INSTITUTION_NOT=20754&size=2"),
                URI.create(hostAddress + "/?INSTITUTION_SHOULD=Sikt&size=2"),
                URI.create(hostAddress + "/?INSTITUTION_NOT=Sikt&size=2"),
                URI.create(hostAddress + "/?published_since=2023-11-05&institution=209.0.0.0&size=2"));
        }
    }

    private static void populateIndex() {
        var jsonFile = stringFromResources(Path.of("sample_resources_search.json"));
        var jsonNodes =
            attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

        jsonNodes.forEach(node -> {
            try {
                var attributes = new EventConsumptionAttributes(indexName, SortableIdentifier.next());
                indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void createIndex() throws IOException {
        var mappingsJson = stringFromResources(Path.of(TEST_RESOURCES_MAPPINGS));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
        indexingClient.createIndex(indexName, mappings);
    }

    private static String generateIndexName() {
        return "resources";
    }


}
