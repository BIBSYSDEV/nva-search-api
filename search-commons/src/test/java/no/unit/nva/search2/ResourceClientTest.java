package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.Query.queryToMapEntries;
import static no.unit.nva.search2.constant.Words.SPACE;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.RestHighLevelClientWrapper;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
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
        searchClient = new ResourceClient(cachedJwtProvider, HttpClient.newHttpClient());
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
    class QueryOpensearchInstance {

        @Test
        void shoulCheckMapping() {

            var mapping = indexingClient.getMapping(indexName);
            assertThat(mapping, is(notNullValue()));
            var topLevelOrgType = mapping.path("properties").path("topLevelOrganizations").path("type").textValue();
            assertThat(topLevelOrgType, is(equalTo("nested")));

            logger.info(mapping.toString());
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {

            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build();

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);

            pagedSearchResourceDto.aggregations()
                .forEach((s, facets) -> logger.info(s + SPACE + facets.stream()
                             .map(JsonSerializable::toJsonString)
                             .collect(Collectors.joining(",\n"))
                         )
                );

            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(query.getValue(SIZE).as())));
            assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(query.getValue(SIZE).as())));
        }


        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsCSVResponse(URI uri) throws ApiGatewayException {

            var csvResult = ResourceQuery.builder()
                                .fromQueryParameters(queryToMapEntries(uri))
                                .withRequiredParameters(FROM, SIZE, SORT)
                                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                                .withMediaType("text/csv")
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
            assertThrows(BadRequestException.class,
                         () -> ResourceQuery.builder()
                                   .fromQueryParameters(queryToMapEntries(uri))
                                   .withRequiredParameters(FROM, SIZE)
                                   .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                                   .build()
                                   .doSearch(searchClient));
        }

        static Stream<URI> uriSortingProvider() {
            return Stream.of(
                URI.create(
                    "https://x.org/?category=AcademicChapter&sort=created_date&sortOrder=asc&sort=category&order"
                    + "=desc"),
                URI.create(
                    "https://x.org/?category=AcademicChapter&sort=modified_date&sortOrder=asc&sort=category"),
                URI.create(
                    "https://x.org/?category=AcademicChapter&sort=published_date&sortOrder=asc&sort=category"),
                URI.create("https://x.org/?category=AcademicChapter&size=10&from=0&sort=modified_date"),
                URI.create("https://x.org/?category=AcademicChapter&orderBy=UNIT_ID:asc,title:desc"),
                URI.create("https://x.org/?category=AcademicChapter&orderBy=created_date:asc,"
                           + "modifiedDate:desc&searchAfter=1241234,23412"),
                URI.create("https://x.org/?category=AcademicChapter&sort=published_date+asc&sort=category+desc"));
        }

        static Stream<URI> uriInvalidProvider() {
            return Stream.of(
                URI.create("https://x.org/?categories=hello+world&lang=en"),
                URI.create("https://x.org/?tittles=hello+world&modified_before=2019-01-01"),
                URI.create("https://x.org/?conttributors=hello+world&published_before=2020-01-01"),
                URI.create("https://x.org/?category=PhdThesis&sort=beunited+asc"),
                URI.create("https://x.org/?funding=NFR,296896"),
                URI.create("https://x.org/?useers=hello+world&lang=en"));
        }

        static Stream<URI> uriProvider() {
            return Stream.of(
                URI.create("https://x.org/?size=20"),
                URI.create("https://x.org/?category=ReportResearch&page=0&size=10"),
                URI.create("https://x.org/?category=ReportResearch,AcademicArticle&page=0&size=19"),
                URI.create("https://x.org/?CONTEXT_TYPE=Anthology&size=1"),
                URI.create("https://x.org/?CONTEXT_TYPE=Report&size=10"),
                URI.create("https://x.org/?CONTEXT_TYPE_NOT=Report&size=10"),
                URI.create("https://x.org/?CONTEXT_TYPE_SHOULD=Report&size=10"),
                //    URI.create("https://x.org/?CONTRIBUTOR_ID"
                //               + "=https://api.dev.nva.aws.unit.no/cristin/person/1136254&size=1"),
                URI.create("https://x.org/?CONTRIBUTOR=Kate+Robinson,Henrik+Langeland&size=3"),
                URI.create("https://x.org/?CONTRIBUTOR=Peter+Gauer,Kjetil+Møkkelgjerd&size=8"),
                URI.create("https://x.org/?CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254&size=2"),
                URI.create("https://x.org/?CONTRIBUTOR_NOT"
                           + "=https://api.dev.nva.aws.unit.no/cristin/person/1136254,Peter+Gauer&size=12"),
                URI.create("https://x.org/?DOI=https://doi.org/10.1371/journal.pone.0047887&size=1"),
                URI.create("https://x.org/?DOI_NOT=https://doi.org/10.1371/journal.pone.0047887&size=18"),
                URI.create("https://x.org/?DOI_SHOULD=https://doi.org/10.1371/journal.pone.0047887&size=2"),
                URI.create("https://x.org/?doi=https://doi.org/10.1371/journal.pone.0047855&size=1"),
                URI.create("https://x.org/?doi_should=.pone.0047855,pone.0047887&size=2"),
                URI.create("https://x.org/?funding=AFR:296896&size=1"),
                URI.create("https://x.org/?funding=NFR:1296896&size=2"),
                URI.create("https://x.org/?funding=NFR:296896&size=2"),
                URI.create("https://x.org/?funding_source=Norges+forskningsråd&size=2"),
                URI.create("https://x.org/?funding_source_not=Norges+forskningsråd&size=18"),
                URI.create("https://x.org/?funding_source_SHOULD=Norges&size=2"),
                URI.create("https://x.org/?funding_source=Research+Council+of+Norway+(RCN)&size=2"),
                URI.create("https://x.org/?ID=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39&size=1"),
                URI.create("https://x.org/?ID_NOT=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39&size=19"),
                URI.create("https://x.org/?ID_SHOULD=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39&size=1"),
                URI.create("https://x.org/?id=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642&size=1"),
                URI.create("https://x.org/?id_should=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642"
                           + "&id_not=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642&size=0"),
                URI.create("https://x.org/?id_should=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642&size=1"),
                URI.create("https://x.org/?INSTANCE_TYPE=AcademicArticle&size=9"),
                URI.create("https://x.org/?INSTANCE_TYPE_NOT=AcademicArticle&size=11"),
                URI.create("https://x.org/?INSTANCE_TYPE_SHOULD=AcademicArticle&size=9"),
                URI.create("https://x.org/?INSTITUTION"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0&size=1"),
                URI.create("https://x.org/?INSTITUTION_NOT"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0&size=19"),
                URI.create("https://x.org/?INSTITUTION_SHOULD=Forsvarets+høgskole&size=2"),
                URI.create("https://x.org/?INSTITUTION=1627.0.0.0&size=0"),
                URI.create("https://x.org/?INSTITUTION=Forsvarets+høgskole&size=2"),
                URI.create("https://x.org/?INSTITUTION=Norwegian+Defence+University+College&size=2"),
                URI.create("https://x.org/?INSTITUTION"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION_NOT=Forsvarets+høgskole&size=18"),
                URI.create("https://x.org/?INSTITUTION_NOT"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0&size=18"),
                URI.create("https://x.org/?INSTITUTION_SHOULD=1627.0.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION_SHOULD=1627.0.0.0,20754.6.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION_SHOULD"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0&size=2"),
                URI.create("https://x.org/?INSTITUTION_should=194.63.55.0&size=1"),
                URI.create("https://x.org/?ISBN=9788202535032&size=1"),
                URI.create("https://x.org/?ISBN_NOT=9788202535032&size=19"),
                URI.create("https://x.org/?ISBN_SHOULD=9788202535032&size=1"),
                URI.create("https://x.org/?issn=1872-9460&size=0"),
                URI.create("https://x.org/?ISSN=1435-9529&size=1"),
                URI.create("https://x.org/?ISSN_NOT=1435-9529&size=19"),
                URI.create("https://x.org/?ISSN_SHOULD=1435-9529&size=1"),
                URI.create("https://x.org/?ORCID=https://sandbox.orcid.org/0000-0003-4147-3499&size=2"),
                URI.create("https://x.org/?ORCID_NOT=https://sandbox.orcid.org/0000-0003-4147-3499&size=18"),
                URI.create("https://x.org/?ORCID_SHOULD=4147-3499&size=2"),
                URI.create("https://x.org/?PARENT_PUBLICATION=test&size=0"),
                URI.create("https://x.org/?PARENT_PUBLICATION_SHOULD=test&size=0"),
                URI.create("https://x.org/?PROJECT=https://api.dev.nva.aws.unit.no/cristin/project/14334813&size=1"),
                URI.create("https://x.org/?PROJECT_NOT"
                           + "=https://api.dev.nva.aws.unit.no/cristin/project/14334813&size=19"),
                URI.create("https://x.org/?PROJECT_SHOULD"
                           + "=https://api.dev.nva.aws.unit.no/cristin/project/14334813&size=1"),
                URI.create("https://x.org/?SEARCH_ALL=Fakultet+for+arkitektur&size=1"),
                URI.create("https://x.org/?TITLE=Kjetils+ticket+test&size=1"),
                URI.create("https://x.org/?TITLE_NOT=Kjetils+ticket+test&size=17"),
                URI.create("https://x.org/?TITLE_SHOULD=Simple&size=3"),
                URI.create("https://x.org/?TOP_LEVEL_ORGANIZATION"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0&size=2"),
                URI.create("https://x.org/?UNIT"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/45220004.0.0.0&size=2"),
                URI.create("https://x.org/?UNIT_NOT"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/45220004.0.0.0&size=18"),
                URI.create("https://x.org/?UNIT_SHOULD=194.63.55.0&size=1"),
                URI.create("https://x.org/?USER=1136254@20754.0.0.0&size=2"),
                URI.create("https://x.org/?USER_NOT=1136254@20754.0.0.0&size=18"),
                URI.create("https://x.org/?USER_SHOULD=1136254@&size=2"),
                URI.create("https://x.org/?MODIFIED_BEFORE=1872-01-01&MODIFIED_SINCE=9460-01-01&size=0"),
                URI.create("https://x.org/?PUBLICATION_YEAR=2022&size=2"),
                URI.create("https://x.org/?PUBLICATION_YEAR_SHOULD=2022&size=2"),
                URI.create("https://x.org/?fields=category,title,CONTRIBUTOR&query=Kjetil+Møkkelgjerd&size=2"),
                URI.create("https://x.org/?from=0&size=2&topLevelOrganization"
                           + "=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0"),
                URI.create("https://x.org/?published_before=2023-09-29&size=5"),
                URI.create("https://x.org/?published_since=2023-11-05&size=1"),
                URI.create("https://x.org/?query=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642"
                           + "&id_not=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642&size=0"),
                URI.create("https://x.org/?query=Forsvarets+høgskole&fields=INSTITUTION&size=2"),
                URI.create("https://x.org/?query=Forsvarets+høgskole&size=2"),
                URI.create("https://x.org/?query=Kjetil+Møkkelgjerd&fields=CONTRIBUTOR&size=2"),
                URI.create("https://x.org/?query=observations&fields=all&size=3"),
                URI.create("https://x.org/?query=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"
                           + "&fields=INSTITUTION&size=1"),
                URI.create("https://x.org/?fields=CONTRIBUTOR"
                           + "&query=https://api.dev.nva.aws.unit.no/cristin/person/1136254&size=2")
            );
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