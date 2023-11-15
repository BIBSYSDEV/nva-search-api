package no.unit.nva.search2;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

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

            var query =
                ResourceAwsQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build();

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            logger.info(pagedSearchResourceDto.toJsonString());
            assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(pagedSearchResourceDto.hits().size())));
        }

        static Stream<URI> uriProvider() {
            return Stream.of(
                URI.create("https://x.org/?size=20"),
                URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                           + "https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=10"),
                URI.create("https://x.org/?CONTRIBUTOR=Isar+Kristoffer+Buzza,Kjetil+Møkkelgjerd&size=10"),
                URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254&size=2"),
                URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=2"),
                URI.create("https://x.org/?CONTRIBUTOR_NOT=https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                           + "https://api.dev.nva.aws.unit.no/cristin/person/1136918&size=18"),
                URI.create("https://x.org/?CONTRIBUTOR_SHOULD=https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                           + "person/1136918&size=10"),
                URI.create("https://x.org/?INSTITUTION="
                           + "https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0&size=2"),
                URI.create("https://x.org//?INSTITUTION_NOT="
                           + "https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0&size=18"),
                URI.create("https://x.org//?INSTITUTION_SHOULD="
                           + "https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION=1627.0.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION_SHOULD=1627.0.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION=Forsvarets+høgskole&size=18"),
                URI.create("https://x.org/?INSTITUTION=194.63.55.0&size=1"),
                URI.create("https://x.org/?INSTITUTION=Norwegian+Defence+University+College&size=2"),
                URI.create("https://x.org/?INSTITUTION_NOT=Forsvarets+høgskole&size=18"),
                URI.create("https://x.org//?INSTITUTION_NOT=Forsvarets+høgskole,194.63.55.0&size=18"),
                URI.create("https://x.org/?INSTITUTION_SHOULD=1627.0.0.0,20754.6.0.0&size=2"),
                URI.create("https://x.org/?category=AcademicArticle&size=9"),
                URI.create("https://x.org/?category=ReportResearch&page=0&size=10"),
                URI.create("https://x.org/?category=ReportResearch,AcademicArticle&page=0&size=19"),
                URI.create("https://x.org/?fields=category,title,created_date&query=Kjetil+Møkkelgjerd&size=2"),
                URI.create("https://x.org/?funding=AFR:296896&size=2"),
                URI.create("https://x.org/?funding=NFR:1296896&size=2"),
                URI.create("https://x.org/?funding=NFR:296896&size=2"),
                URI.create("https://x.org/?funding_source=Norges+forskningsråd&size=2"),
                URI.create("https://x.org/?funding_source_not=Norges+forskningsråd&size=19"),
                URI.create("https://x.org/?funding_source_SHOULD=Norges+epler&size=2"),
                URI.create("https://x.org/?funding_source=Research+Council+of+Norway+(RCN)&size=2"),
                URI.create("https://x.org/?published_before=2020-01-01&size=2"),
                URI.create("https://x.org/?published_since=2023-11-05&size=2"),
                URI.create("https://x.org/?query=Forsvarets+høgskole&fields=INSTITUTION&size=2"),
                URI.create("https://x.org/?query=Forsvarets+høgskole&size=2"),
                URI.create("https://x.org/?query=Kjetil+Møkkelgjerd&fields=CONTRIBUTOR&size=2"),
                URI.create("https://x.org/?query=https://api.dev.nva.aws.unit.no/cristin/person/1136254"
                           + "&fields=CONTRIBUTOR&size=3"),
                URI.create("https://x.org/?query=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"
                           + "&fields=INSTITUTION&size=2"));
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
