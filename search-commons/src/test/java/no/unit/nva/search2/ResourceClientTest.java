package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.Query.queryToMapEntries;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.INSTANCE_TYPE;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import no.unit.nva.search2.constant.Words;
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

    protected static final Logger logger = LoggerFactory.getLogger(ResourceClientTest.class);
    public static final String TEST_RESOURCES_MAPPINGS = "test_resources_mappings.json";
    public static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.0.0";
    public static final long DELAY_AFTER_INDEXING = 1000L;
    private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
    private static ResourceClient searchClient;
    private static IndexingClient indexingClient;
    private static String indexName;

    @BeforeAll
    static void setUp() throws IOException, InterruptedException {
        container.start();

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
        searchClient = new ResourceClient(HttpClient.newHttpClient(), cachedJwtProvider);
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
    class ResourceQueries {

        @Test
        void shoulCheckMapping() {

            var mapping = indexingClient.getMapping(indexName);
            assertThat(mapping, is(notNullValue()));
            var topLevelOrgType = mapping.path("properties")
                .path("topLevelOrganizations")
                .path("type").textValue();
            assertThat(topLevelOrgType, is(equalTo("nested")));

            logger.info(mapping.toString());
        }

        @Test
        void shoulCheckFacets() throws BadRequestException {
            var uri = URI.create("https://x.org/?size=20");
            var query = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE)
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .build();
            var response = searchClient.doSearch(query);
            var aggregations = query.toPagedResponse(response).aggregations();

            assertFalse(aggregations.isEmpty());
            assertThat(aggregations.get("type").size(), is(3));
            assertThat(aggregations.get("hasFile").size(), is(1));
            assertThat(aggregations.get("hasFile").get(0).count(), is(20));
            assertThat(aggregations.get("fundingSource").size(), is(2));
            assertThat(aggregations.get("contributorId").size(), is(1));
            assertThat(aggregations.get("topLevelOrganization").size(), is(4));
            assertThat(aggregations.get("topLevelOrganization").get(1).labels().get("nb"),
                       is(equalTo("Sikt – Kunnskapssektorens tjenesteleverandør")));
        }

        @Test
        void emptyResultShouldIncludeHits() throws BadRequestException {
            var uri = URI.create("https://x.org/?id=018b857b77b7");

            var pagedResult =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .doSearch(searchClient);
            assertNotNull(pagedResult);
            assertTrue(pagedResult.contains("\"hits\":["));
        }

        @ParameterizedTest
        @MethodSource("uriPagingProvider")
        void searchWithUriPageableReturnsOpenSearchResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build();

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            assertThat(pagedSearchResourceDto.aggregations().size(), is(equalTo(5)));
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build();

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            if (expectedCount == 0) {
                logger.info(pagedSearchResourceDto.toJsonString());
            }

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedCount)));
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsCSVResponse(URI uri) throws ApiGatewayException {
            var csvResult =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withMediaType(Words.TEXT_CSV)
                    .build()
                    .doSearch(searchClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, SORT, INSTANCE_TYPE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build();

            logger.info(query.getValue(SORT).toString());
            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);
            logger.info(pagedSearchResourceDto.id().toString());
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

        static Stream<Arguments> uriPagingProvider() {
            return Stream.of(
                createArgument("page=0", 20),
                createArgument("page=1&size=1", 1),
                createArgument("page=2&size=1", 1),
                createArgument("page=3&size=1", 1),
                createArgument("page=0&size=0", 0)
            );
        }

        static Stream<URI> uriSortingProvider() {
            final var uriRoot = "https://x.org/?category=AcademicChapter&";
            return Stream.of(
                URI.create(uriRoot + "sort=created_date&sortOrder=asc&sort=category&order=desc"),
                URI.create(uriRoot + "sort=modified_date&sortOrder=asc&sort=category"),
                URI.create(uriRoot + "sort=published_date&sortOrder=asc&sort=category"),
                URI.create(uriRoot + "size=10&from=0&sort=modified_date"),
                URI.create(uriRoot + "orderBy=UNIT_ID:asc,title:desc"),
                URI.create(uriRoot + "orderBy=created_date:asc,modifiedDate:desc&searchAfter=1241234,23412"),
                URI.create(uriRoot + "sort=published_date+asc&sort=category+desc"));
        }

        static Stream<URI> uriInvalidProvider() {
            final var uriRoot = "https://x.org/?";
            return Stream.of(
                URI.create(uriRoot + "categories=hello+world&lang=en"),
                URI.create(uriRoot + "tittles=hello+world&modified_before=2019-01-01"),
                URI.create(uriRoot + "conttributors=hello+world&published_before=2020-01-01"),
                URI.create(uriRoot + "category=PhdThesis&sort=beunited+asc"),
                URI.create(uriRoot + "funding=NFR,296896"),
                URI.create(uriRoot + "useers=hello+world&lang=en"));
        }

        static Stream<Arguments> uriProvider() {
            return Stream.of(
                createArgument("page=0", 20),
                createArgument("CATEGORY=ReportResearch&page=0", 10),
                createArgument("TYPE=ReportResearch,AcademicArticle", 19),
                createArgument("CONTEXT_TYPE=Anthology", 1),
                createArgument("CONTEXT_TYPE=Report", 10),
                createArgument("CONTEXT_TYPE_SHOULD=Report", 10),
                createArgument("CONTEXT_TYPE_NOT=Report", 10),
                createArgument("CONTRIBUTOR=Kate+Robinson,Henrik+Langeland", 3),
                createArgument("CONTRIBUTOR=Peter+Gauer,Kjetil+Møkkelgjerd", 8),
                createArgument("CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254", 2),
                createArgument("CONTRIBUTOR_SHOULD=Gauer,Møkkelgjerd", 8),
                createArgument("CONTRIBUTOR_NOT=https://api.dev.nva.aws.unit.no/cristin/person/1136254,Peter+Gauer",
                               12),
                createArgument("DOI=https://doi.org/10.1371/journal.pone.0047887", 1),
                createArgument("DOI_NOT=https://doi.org/10.1371/journal.pone.0047887", 18),
                createArgument("DOI_SHOULD=https://doi.org/10.1371/journal.pone.0047887", 2),
                createArgument("DOI=https://doi.org/10.1371/journal.pone.0047855", 1),
                createArgument("DOI_SHOULD=.pone.0047855,pone.0047887", 2),
                createArgument("FUNDING=AFR:296896", 1),
                createArgument("FUNDING=NFR:1296896", 2),
                createArgument("FUNDING=NFR:296896", 2),
                createArgument("FUNDING_SOURCE=Norges+forskningsråd", 2),
                createArgument("FUNDING_SOURCE_NOT=Norges+forskningsråd", 18),
                createArgument("FUNDING_SOURCE_SHOULD=Norges", 2),
                createArgument("FUNDING_SOURCE=Research+Council+of+Norway+(RCN)", 2),
                createArgument("hasFile=1", 19),
                createArgument("HASFILE=true", 19),
                createArgument("HAS_FILE=0", 1),
                createArgument("ID=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39", 1),
                createArgument("ID_NOT=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39", 19),
                createArgument("ID_NOT=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642&query=25e43dc3027e", 10),
                createArgument("ID_SHOULD=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39", 1),
                createArgument("ID=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 1),
                createArgument("ID_SHOULD=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642"
                               + "&ID_NOT=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 0),
                createArgument("ID_SHOULD=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 1),
                createArgument("INSTANCE_TYPE=AcademicArticle", 9),
                createArgument("INSTANCE_TYPE_NOT=AcademicArticle", 11),
                createArgument("INSTANCE_TYPE_SHOULD=AcademicArticle", 9),
                createArgument("INSTITUTION=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0", 1),
                createArgument("INSTITUTION_NOT=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0", 19),
                createArgument("INSTITUTION_SHOULD=Forsvarets+høgskole", 2),
                createArgument("INSTITUTION=1627.0.0.0", 0),
                createArgument("INSTITUTION=Forsvarets+høgskole", 2),
                createArgument("INSTITUTION=Norwegian+Defence+University+College", 2),
                createArgument("INSTITUTION=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0", 2),
                createArgument("INSTITUTION_NOT=Forsvarets+høgskole", 18),
                createArgument("INSTITUTION_NOT"
                               + "=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0", 18),
                createArgument("INSTITUTION_SHOULD=1627.0.0.0", 2),
                createArgument("INSTITUTION_SHOULD=1627.0.0.0,20754.6.0.0", 2),
                createArgument("INSTITUTION_SHOULD=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0", 2),
                createArgument("INSTITUTION_should=194.63.55.0", 1),
                createArgument("ISBN=9788202535032", 1),
                createArgument("ISBN_NOT=9788202535032", 19),
                createArgument("ISBN_SHOULD=9788202535032", 1),
                createArgument("ISSN=1872-9460", 0),
                createArgument("ISSN=1435-9529", 1),
                createArgument("ISSN_NOT=1435-9529", 19),
                createArgument("ISSN_SHOULD=1435-9529", 1),
                createArgument("ORCID=https://sandbox.orcid.org/0000-0003-4147-3499", 2),
                createArgument("ORCID_NOT=https://sandbox.orcid.org/0000-0003-4147-3499", 18),
                createArgument("ORCID_SHOULD=4147-3499", 2),
                createArgument("PARENT_PUBLICATION=test", 0),
                createArgument("PARENT_PUBLICATION_SHOULD=test", 0),
                createArgument("PROJECT=https://api.dev.nva.aws.unit.no/cristin/project/14334813", 1),
                createArgument("PROJECT_NOT=https://api.dev.nva.aws.unit.no/cristin/project/14334813", 19),
                createArgument("PROJECT_SHOULD=https://api.dev.nva.aws.unit.no/cristin/project/14334813", 1),
                createArgument("SEARCH_ALL=Fakultet+for+arkitektur", 1),
                createArgument("TITLE=Kjetils+ticket+test", 1),
                createArgument("TITLE_NOT=Kjetils+ticket+test", 17),
                createArgument("TITLE_SHOULD=Simple", 3),
                createArgument(
                    "TOP_LEVEL_ORGANIZATION=https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0", 2),
                createArgument("UNIT=https://api.dev.nva.aws.unit.no/cristin/organization/45220004.0.0.0", 2),
                createArgument("UNIT_NOT=https://api.dev.nva.aws.unit.no/cristin/organization/45220004.0.0.0", 18),
                createArgument("UNIT_SHOULD=194.63.55.0", 1),
                createArgument("USER=1136254@20754.0.0.0", 2),
                createArgument("USER_NOT=1136254@20754.0.0.0", 18),
                createArgument("USER_SHOULD=1136254@", 2),
                createArgument("MODIFIED_BEFORE=1872-01-01&MODIFIED_SINCE=9460-01-01", 0),
                createArgument("PUBLICATION_YEAR=2022", 2),
                createArgument("PUBLICATION_YEAR_SHOULD=2022", 2),
                createArgument("FIELDS=category,title,CONTRIBUTOR&query=Kjetil+Møkkelgjerd", 2),
                createArgument("TOPLEVEL_ORGANIZATION=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0"
                    , 2),
                createArgument("PUBLISHED_BEFORE=2023-09-29", 5),
                createArgument("PUBLISHED_SINCE=2023-11-05", 1),
                createArgument("QUERY=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642"
                               + "&ID_NOT=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 0),
                createArgument("QUERY=Forsvarets+høgskole&fields=INSTITUTION", 2),
                createArgument("QUERY=Forsvarets+høgskole", 2),
                createArgument("QUERY=Kjetil+Møkkelgjerd&fields=CONTRIBUTOR", 2),
                createArgument("QUERY=observations&fields=all", 3),
                createArgument("QUERY=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"
                               + "&FIELDS=INSTITUTION", 1),
                createArgument("FIELDS=CONTRIBUTOR&QUERY=https://api.dev.nva.aws.unit.no/cristin/person/1136254", 2)
            );
        }
    }

    private static Arguments createArgument(String searchUri, int expectedCount) {
        final var uriRoot = "https://x.org/?size=20&";
        return Arguments.of(URI.create(uriRoot + searchUri), expectedCount);
    }

    private static void populateIndex() {
        var jsonFile = stringFromResources(Path.of("sample_resources_search.json"));
        var jsonNodes =
            attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

        jsonNodes.forEach(ResourceClientTest::addDocumentToIndex);
    }

    private static void addDocumentToIndex(JsonNode node) {
        try {
            var attributes = new EventConsumptionAttributes(indexName, SortableIdentifier.next());
            indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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