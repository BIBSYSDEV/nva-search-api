package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search2.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search2.constant.Words.HAS_FILE;
import static no.unit.nva.search2.constant.Words.PUBLISHER;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.enums.PublicationStatus.DELETED;
import static no.unit.nva.search2.enums.PublicationStatus.DRAFT;
import static no.unit.nva.search2.enums.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.search2.enums.PublicationStatus.NEW;
import static no.unit.nva.search2.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search2.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search2.enums.ResourceParameter.AGGREGATION;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public static final long DELAY_AFTER_INDEXING = 1500L;
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
            var uri1 = URI.create("https://x.org/?size=20&aggregation=all");
            var uri2 = URI.create("https://x.org/?size=20&aggregation=entityDescription,associatedArtifacts," +
                "topLevelOrganizations,fundings,status");
            var query1 = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri1))
                .withRequiredParameters(FROM, SIZE)
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);
            var query2 = ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri2))
                .withRequiredParameters(FROM, SIZE)
                .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                .build()
                .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);
            var response1 = searchClient.doSearch(query1);
            var response2 = searchClient.doSearch(query2);

            assertNotNull(response1);
            assertNotNull(response2);

            assertEquals(response1.aggregations(),response2.aggregations());

            var aggregations = query1.toPagedResponse(response1).aggregations();

            assertFalse(aggregations.isEmpty());
            assertThat(aggregations.get(TYPE).size(), is(3));
            assertThat(aggregations.get(HAS_FILE).size(), is(2));
            assertThat(aggregations.get(HAS_FILE).get(0).count(), is(19));
            assertThat(aggregations.get(FUNDING_SOURCE).size(), is(2));
            assertThat(aggregations.get(PUBLISHER).size(), is(2));
            assertThat(aggregations.get(CONTRIBUTOR).size(), is(12));
            assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION).size(), is(4));
            assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION).get(1).labels().get("nb"),
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
                    .withRequiredStatus(NEW, DRAFT, PUBLISHED_METADATA, PUBLISHED, DELETED, UNPUBLISHED, DRAFT_FOR_DELETION )
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
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            assertThat(pagedSearchResourceDto.aggregations().size(), is(equalTo(9)));
            logger.debug(pagedSearchResourceDto.id().toString());
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsOpenSearchAwsResponse(URI uri, int expectedCount) throws ApiGatewayException {

            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);

            assertNotNull(pagedSearchResourceDto);
            if (expectedCount == 0) {
                logger.debug(pagedSearchResourceDto.toJsonString());
            } else {
                logger.debug(pagedSearchResourceDto.toString());
            }

            assertThat(pagedSearchResourceDto.hits().size(), is(equalTo(expectedCount)));
            assertThat(pagedSearchResourceDto.totalHits(), is(equalTo(expectedCount)));
        }

        @ParameterizedTest
        @MethodSource("uriProvider")
        void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
            var csvResult =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, AGGREGATION)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .withMediaType(Words.TEXT_CSV)
                    .build()
                    .withRequiredStatus(PUBLISHED_METADATA)
                    .doSearch(searchClient);
            assertNotNull(csvResult);
        }

        @ParameterizedTest
        @MethodSource("uriSortingProvider")
        void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
            var query =
                ResourceQuery.builder()
                    .fromQueryParameters(queryToMapEntries(uri))
                    .withRequiredParameters(FROM, SIZE, SORT, INSTANCE_TYPE, AGGREGATION)
                    .withOpensearchUri(URI.create(container.getHttpHostAddress()))
                    .build()
                    .withRequiredStatus(PUBLISHED, PUBLISHED_METADATA);

            logger.info(query.getValue(SORT).toString());
            var response = searchClient.doSearch(query);
            var pagedSearchResourceDto = query.toPagedResponse(response);
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
                createArgument("page=0&aggregation=all", 20),
                createArgument("page=1&aggregation=all&size=1", 1),
                createArgument("page=2&aggregation=all&size=1", 1),
                createArgument("page=3&aggregation=all&size=1", 1),
                createArgument("page=0&aggregation=all&size=0", 0),
                createArgument("offset=15&aggregation=all&size=2", 2),
                createArgument("offset=15&aggregation=all&limit=2", 2),
                createArgument("offset=15&aggregation=all&results=2", 2),
                createArgument("offset=15&aggregation=all&per_page=2", 2),
                createArgument("OFFSET=15&aggregation=all&PER_PAGE=2", 2),
                createArgument("offset=15&aggregation=all&perPage=2", 2)
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
                URI.create(uriRoot + "sort=epler"),
                URI.create(uriRoot + "sort=CATEGORY:DEdd"),
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
                createArgument("ABSTRACT=NAKSIN&page=0", 3),
                createArgument("ABSTRACT_NOT=NAKSIN&page=0", 17),
                createArgument("ABSTRACT_SHOULD=probability,hazard&page=0", 6),
                createArgument("CATEGORY=ReportResearch&page=0", 10),
                createArgument("TYPE_should=ReportResearch,AcademicArticle", 19),
                createArgument("CONTEXT_TYPE=Anthology", 1),
                createArgument("CONTEXT_TYPE=Report", 10),
                createArgument("CONTEXT_TYPE_SHOULD=Report", 10),
                createArgument("CONTEXT_TYPE_NOT=Report", 10),
                createArgument("CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254", 3),
                createArgument("CONTRIBUTOR=https://api.dev.nva.aws.unit.no/cristin/person/1136254,"
                               + "https://api.dev.nva.aws.unit.no/cristin/person/1136255", 0),
                createArgument("CONTRIBUTOR_NOT=https://api.dev.nva.aws.unit.no/cristin/person/1136254", 17),
                createArgument("CONTRIBUTOR_NAME=Kate+Robinson,Henrik+Langeland", 1),
                createArgument("CONTRIBUTOR_NAME=Kate+Robinson&CONTRIBUTOR_NAME=Henrik+Langeland", 1),
                createArgument("CONTRIBUTOR_NAME=Peter+Gauer,Kjetil+Møkkelgjerd", 1),
                createArgument("CONTRIBUTOR_NAME=Gauer,Møkkelgjerd", 1),
                createArgument("CONTRIBUTOR_NAME_SHOULD=Peter+Gauer,Kjetil+Møkkelgjerd", 8),
                createArgument("CONTRIBUTOR_NAME_SHOULD=Gauer,Møkkelgjerd", 8),
                createArgument("DOI=https://doi.org/10.1371/journal.pone.0047887", 1),
                createArgument("DOI_NOT=https://doi.org/10.1371/journal.pone.0047887", 19),
                createArgument("DOI_SHOULD=https://doi.org/10.1371/journal.pone.0047887", 1),
                createArgument("DOI=https://doi.org/10.1371/journal.pone.0047855", 1),
                createArgument("DOI_SHOULD=10.1371/journal.pone.0047887", 1),
                createArgument("DOI_SHOULD=0047855,0047887", 2),
                createArgument("FUNDING=AFR:296896", 0),
                createArgument("FUNDING=NFR:296896", 1),
                createArgument("FUNDING=NFR:3333", 0),
                createArgument("FUNDING_SOURCE=NFR", 2),
                createArgument("FUNDING_SOURCE=NFR,1234", 1),
                createArgument("FUNDING_SOURCE=1234", 1),
                createArgument("FUNDING_SOURCE=Norges+forskningsråd", 2),
                createArgument("FUNDING_SOURCE_NOT=Norges+forskningsråd", 18),
                createArgument("FUNDING_SOURCE_SHOULD=Norges", 2),
                createArgument("FUNDING_SOURCE=Research+Council+of+Norway+(RCN)", 2),
                createArgument("hasFile=PublishedFile", 19),
                createArgument("hasFile=1", 19),
                createArgument("HASFILE=true", 19),
                createArgument("HAS_FILE=0", 1),
                createArgument("ID=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39", 1),
                createArgument("ID=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 1),
                createArgument("ID_SHOULD=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 1),
                createArgument("ID_NOT=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39", 19),
                createArgument("ID_NOT=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642&query=25e43dc3027e", 10),
                createArgument("ID_SHOULD=018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619ecbbaf39,"
                               + "018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 2),
                createArgument("ID_SHOULD=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642"
                               + "&ID_NOT=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 0),
                createArgument("CaTeGoRy=ReportResearch&page=0", 10),
                createArgument("type_should=ReportResearch,AcademicArticle", 19),
                createArgument("instanceType=AcademicArticle", 9),
                createArgument("INSTANCE_TYPE_NOT=AcademicArticle", 11),
                createArgument("INSTANCE_TYPE_SHOULD=AcademicArticle", 9),
                createArgument("INSTITUTION=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0", 2),
                createArgument("INSTITUTION_NOT=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0", 18),
                createArgument("INSTITUTION_SHOULD=Forsvarets+høgskole", 3),
                createArgument("INSTITUTION=1627.0.0.0", 3),
                createArgument("INSTITUTION=Forsvarets+høgskole", 3),
                createArgument("INSTITUTION=Norwegian+Defence+University+College", 3),
                createArgument("INSTITUTION=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0", 3),
                createArgument("INSTITUTION_NOT=Forsvarets+høgskole", 17),
                createArgument("INSTITUTION_NOT"
                               + "=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0", 17),
                createArgument("INSTITUTION_SHOULD=1627.0.0.0", 3),
                createArgument("INSTITUTION_SHOULD=1627.0.0.0,20754.6.0.0", 3),
                createArgument("INSTITUTION_SHOULD=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0", 3),
                createArgument("INSTITUTION_should=194.63.55.0", 1),
                createArgument("ISBN=9788202535032", 1),
                createArgument("ISBN_NOT=9788202535032", 19),
                createArgument("ISBN_SHOULD=9788202535032", 1),
                createArgument("ISSN=1872-9460", 0),
                createArgument("ISSN=1435-9529", 1),
                createArgument("ISSN_NOT=1435-9529", 19),
                createArgument("ISSN_SHOULD=1435-9529", 1),
                createArgument("LICENSE=https://creativecommons.org/licenses/by/4.0", 5),
                createArgument("LICENSE_NOT=https://creativecommons.org/licenses/by/4.0", 15),
                createArgument("LICENSE_SHOULD=https://creativecommons.org/licenses/by/4.0", 5),
                createArgument("ORCID=https://sandbox.orcid.org/0000-0003-4147-3499", 3),
                createArgument("ORCID_NOT=https://sandbox.orcid.org/0000-0003-4147-3499", 17),
                createArgument("ORCID_SHOULD=4147-3499", 3),
                createArgument("PARENT_PUBLICATION=test", 0),
                createArgument("PARENT_PUBLICATION_SHOULD=test", 0),
                createArgument("PROJECT=https://api.dev.nva.aws.unit.no/cristin/project/14334813", 2),
                createArgument("PROJECT=https://api.dev.nva.aws.unit.no/cristin/project/14334813,"
                               + "https://api.dev.nva.aws.unit.no/cristin/project/14334631", 1),
                createArgument("PROJECT_NOT=https://api.dev.nva.aws.unit.no/cristin/project/14334813,"
                               + "https://api.dev.nva.aws.unit.no/cristin/project/14334631", 17),
                createArgument("PROJECT_SHOULD=https://api.dev.nva.aws.unit.no/cristin/project/14334813,"
                               + "https://api.dev.nva.aws.unit.no/cristin/project/14334631", 3),
                createArgument("PUBLISHER=NGI+–+Norges+Geotekniske+institutt", 9),
                createArgument("PUBLISHER_NOT=NGI+–+Norges+Geotekniske+institutt", 11),
                createArgument("PUBLISHER_SHOULD=NGI+–+Norges+Geotekniske+institutt", 9),
                createArgument("SEARCH_ALL=Fakultet+for+arkitektur", 1),
                createArgument("TAGS=NAKSIN,Avalanche-RnD&page=0", 1),
                createArgument("TAGS_NOT=NAKSIN&page=0", 19),
                createArgument("TAGS_SHOULD=NAKSIN,Avalanche-RnD&page=0", 9),
                createArgument("TITLE=Kjetils+ticket+test", 1),
                createArgument("TITLE_NOT=Kjetils+ticket+test", 19),
                createArgument("TITLE_NOT=Kjetils", 18),
                createArgument("TITLE_SHOULD=Simple", 3),
                createArgument(
                    "TOP_LEVEL_ORGANIZATION=https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0", 2),
                createArgument("UNIT=https://api.dev.nva.aws.unit.no/cristin/organization/45220004.0.0.0", 3),
                createArgument("UNIT_NOT=https://api.dev.nva.aws.unit.no/cristin/organization/45220004.0.0.0", 17),
                createArgument("UNIT_SHOULD=194.63.55.0", 1),
                createArgument("USER=1136254@20754.0.0.0", 2),
                createArgument("USER_NOT=1136254@20754.0.0.0", 18),
                createArgument("USER_SHOULD=1136254@", 2),
                createArgument("MODIFIED_BEFORE=1872-01-01&MODIFIED_SINCE=9460-01-01", 0),
                createArgument("PUBLICATION_YEAR_BEFORE=2022", 4),
                createArgument("PUBLICATION_YEAR_SINCE=2023", 14),
                createArgument("PUBLICATION_YEAR_SHOULD=2022", 2),
                createArgument("FIELDS=category,title,CONTRIBUTOR_NAME&query=Kjetil+Møkkelgjerd", 3),
                createArgument("TOPLEVEL_ORGANIZATION=https://api.dev.nva.aws.unit.no/cristin/organization/1627.0.0.0",
                               2),
                createArgument("PUBLISHED_BETWEEN=2023-10-15,2023-11-05", 2),
                createArgument("PUBLISHED_BEFORE=2023-09-29", 5),
                createArgument("PUBLISHED_SINCE=2023-11-05", 1),
                createArgument("PUBLISH_STATUS_NOT=PUBLISHED", 1),
                createArgument("SERIES=NGI-Rapport", 9),
                createArgument("SERIES_NOT=NGI-Rapport", 11),
                createArgument("SERIES_SHOULD=NGI-Rapport", 9),
                createArgument("QUERY=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642"
                               + "&ID_NOT=018b857b77b7-697ebc73-5195-4ce4-9ba1-1d5a7b540642", 0),
                createArgument("QUERY=Forsvarets+høgskole&fields=INSTITUTION", 3),
                createArgument("QUERY=Forsvarets+høgskole", 3),
                createArgument("QUERY=Kjetil+Møkkelgjerd&fields=contributorName", 3),
                createArgument("QUERY=observations&fields=all", 3),
                createArgument("QUERY=https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0"
                               + "&FIELDS=INSTITUTION", 2),
                createArgument("FIELDS=CONTRIBUTOR&QUERY=https://api.dev.nva.aws.unit.no/cristin/person/1136254", 3)
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