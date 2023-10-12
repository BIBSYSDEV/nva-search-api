package no.unit.nva.search;

import static java.util.Collections.emptyList;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.SearchClient.DOCUMENT_TYPE;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.ORGANIZATION_IDS;
import static no.unit.nva.search.SearchClient.TICKET_STATUS;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
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
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.constants.ApplicationConstants;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchTicketsQuery;
import no.unit.nva.search.restclients.responses.ViewingScope;
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
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OpensearchTest {

    public static final URI INCLUDED_ORGANIZATION_ID = randomUri();
    public static final URI EXCLUDED_ORGANIZATION_ID = randomUri();
    public static final long ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY = 0;
    public static final long TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS = 2;
    public static final long ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED = 1;
    public static final String STATUS_TO_INCLUDE_IN_RESULT = "Pending";
    public static final long NON_ZERO_HITS_BECAUSE_APPROVED_WAS_INCLUDED = 1;
    public static final long DELAY_AFTER_INDEXING = 1000L;
    public static final String TEST_RESOURCES_MAPPINGS = "test_resources_mappings.json";
    public static final String TEST_IMPORT_CANDIDATES_MAPPINGS = "test_import_candidates_mappings.json";
    public static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.0.0";
    private static final int SAMPLE_NUMBER_OF_RESULTS = 7;
    private static final int SAMPLE_FROM = 0;
    private static final String SAMPLE_ORDERBY = "orderByField";
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_NO = 0;
    private static final URI ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES = URI.create(
        "https://www.example.com/20754.0.0.0");
    private static final String COMPLETED = "Completed";
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

    @Test
    void shouldReturnCorrectMappingsFromIndexWhenIndexIsCreatedWithMappingAndAfterDocIsAdded()
        throws IOException, InterruptedException {
        indexName = generateIndexName();

        var mappingsJson = stringFromResources(Path.of(TEST_RESOURCES_MAPPINGS));
        var type = new TypeReference<Map<String, Object>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();

        indexingClient.createIndex(indexName, mappings);
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_response_with_publication_status_as_draft.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);

        var mapping = indexingClient.getMapping(indexName);
        assertThat(mapping, is(notNullValue()));
        var topLevelOrgType = mapping.path("properties").path("topLevelOrganizations").path("type").textValue();
        assertThat(topLevelOrgType, is(equalTo("nested")));
    }

    void assertAggregation(JsonNode aggregationNode, String key, int expectedDocCount) {
        aggregationNode.forEach(
            bucketNode -> {
                if (bucketNode.get("key").asText().equals(key)) {
                    assertThat(bucketNode.get("docCount").asInt(), is(equalTo(expectedDocCount)));
                }
            }
        );
    }

    private String generateIndexName() {
        return RandomDataGenerator.randomString().toLowerCase();
    }

    private ViewingScope getEmptyViewingScope() {
        return new ViewingScope();
    }

    private IndexDocument getIndexDocument(String indexName, Set<URI> organizationIds) {
        return getIndexDocument(indexName, organizationIds, STATUS_TO_INCLUDE_IN_RESULT);
    }

    private IndexDocument getIndexDocument(String indexName, Set<URI> organizationIds, String status) {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
            indexName,
            SortableIdentifier.next()
        );
        Map<String, Object> map = Map.of(
            ORGANIZATION_IDS, organizationIds,
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
            addDocumentsToIndex("imported_candidate_from_index.json",
                                        "not_imported_candidate_from_index.json",
                                       "not_applicable_import_candidate_from_index.json");

            var query = queryWithTermAndAggregation(SEARCH_ALL, IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            assertThat(response.getAggregations(), is(not(emptyIterable())));
        }

        @Test
        void shouldQueryPublicationsWithMultipleOrganizations()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("imported_candidate_from_index.json", "not_imported_candidate_from_index.json");

            var searchTerm = "collaborationType:\"Collaborative\"";
            var query = queryWithTermAndAggregation(searchTerm, IMPORT_CANDIDATES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            assertThat(response.getHits(), hasSize(1));
        }
    }

    @Nested
    class AddDocumentToIndexTest {

        @BeforeEach
        void beforeEachTest() throws IOException {
            indexName = generateIndexName();

            var mappingsJson = stringFromResources(Path.of(TEST_RESOURCES_MAPPINGS));
            var type = new TypeReference<Map<String, Object>>() {
            };
            var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
            indexingClient.createIndex(indexName, mappings);
        }

        @AfterEach
        void afterEachTest() throws Exception {
            indexingClient.deleteIndex(indexName);
        }

        @Test
        void shouldReturnZeroHitsOnEmptyViewingScope() throws Exception {

            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of()));

            Thread.sleep(DELAY_AFTER_INDEXING);
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var response = searchClient.searchWithSearchTicketQuery(getEmptyViewingScope(),
                                                                    searchTicketsQuery,
                                                                    indexName);

            assertThat(response.getSize(),
                       is(equalTo(ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY)));
        }

        @Test
        void shouldReturnTwoHitsOnViewingScopeWithIncludedUnit() throws Exception {

            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));

            Thread.sleep(DELAY_AFTER_INDEXING);

            var viewingScope = getEmptyViewingScope();
            viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var response = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                    searchTicketsQuery,
                                                                    indexName);

            assertThat(response.getSize(),
                       is(equalTo(TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS)));
        }

        @Test
        void shouldReturnZeroHitsBecauseStatusIsCompleted() throws Exception {

            indexingClient.addDocumentToIndex(
                getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID), COMPLETED)
            );

            Thread.sleep(DELAY_AFTER_INDEXING);

            var viewingScope = getEmptyViewingScope();
            viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var response = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                    searchTicketsQuery,
                                                                    indexName);

            assertThat(response.getSize(),
                       is(equalTo(NON_ZERO_HITS_BECAUSE_APPROVED_WAS_INCLUDED)));
        }

        @Test
        void shouldReturnOneHitOnViewingScopeWithExcludedUnit() throws Exception {

            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID,
                                                                                 EXCLUDED_ORGANIZATION_ID)));

            Thread.sleep(DELAY_AFTER_INDEXING);

            var viewingScope = getEmptyViewingScope();
            viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));
            viewingScope.setExcludedUnits(Set.of(EXCLUDED_ORGANIZATION_ID));
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var response = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                    searchTicketsQuery,
                                                                    indexName);

            assertThat(response.getSize(),
                       is(equalTo(ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED)));
        }

        @Test
        void shouldCreateSearchTicketsResponseFromSearchResponse() throws Exception {

            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
            indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));

            Thread.sleep(DELAY_AFTER_INDEXING);

            var viewingScope = ViewingScope.create(INCLUDED_ORGANIZATION_ID);
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var searchResourcesResponse = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                                   searchTicketsQuery,
                                                                                   indexName);

            assertThat(searchResourcesResponse, is(notNullValue()));
            assertThat(searchResourcesResponse.getHits().size(), is(equalTo(2)));
        }

        @Test
        void shouldVerifySearchNotReturningHitsWithPublicationRequestInSearchResponse() throws Exception {
            addDocumentsToIndex("sample_response_with_publication_status_as_draft.json",
                                "sample_response_with_publication_status_as_requested.json");

            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var searchResourcesResponse = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                                   searchTicketsQuery,
                                                                                   indexName);

            assertThat(searchResourcesResponse, is(notNullValue()));
            var actualHitsExcludingHitsWithPublicationStatusDraft = 1;
            assertThat(searchResourcesResponse.getHits().size(),
                       is(equalTo(actualHitsExcludingHitsWithPublicationStatusDraft)));
        }

        @ParameterizedTest()
        @ValueSource(strings = {"navnesen", "navn", "navn+navnesen"})
        void shouldReturnHitsWhenSearchedForPartianMatchOfCuratorName(String queryStr) throws Exception {
            addDocumentsToIndex("publication.json");

            var query = queryWithTermAndAggregation(queryStr, null);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            assertThat(response.getHits(), hasSize(1));
        }

        @Test
        void shouldReturnPendingPublishingRequestsForPublications()
            throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_publishing_request_of_draft_publication.json",
                                "sample_publishing_request_of_published_publication.json");

            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var searchResourcesResponse = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                                   searchTicketsQuery,
                                                                                   indexName);

            assertThat(searchResourcesResponse, is(notNullValue()));
            var expectedHits = 2;
            assertThat(searchResourcesResponse.getHits().size(), is(equalTo(expectedHits)));
        }

        @Test
        void shouldReturnCorrectNumberOfBucketsWhenRequestedNonDefaultAmount() throws ApiGatewayException,
                                                                                      InterruptedException {
            addDocumentsToIndex("sample_publishing_request_of_draft_publication.json",
                                "sample_publishing_request_of_published_publication.json");

            var aggregationDto = new TermsAggregationBuilder("publication.status")
                                     .field("publication.status.keyword")
                                     .size(1);

            SearchDocumentsQuery query = queryWithTermAndAggregation(SEARCH_ALL, List.of(aggregationDto));

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response.getAggregations()
                           .get("publication.status")
                           .get("buckets")
                           .size(),
                       is(equalTo(1))
            );
        }

        @Test
        void shouldReturnHitsWithScore() throws ApiGatewayException, InterruptedException {
            addDocumentsToIndex("sample_publication.json",
                                "sample_publication_with_several_of_the_same_affiliation.json",
                               "sample_publication_with_affiliations.json");
            var mostBoostedPublication = "https://api.sandbox.nva.aws.unit"
                                       + ".no/publication/8c9f0155-bf95-4ba9-b291-0fdc2814f8df";
            var secondBoostedPublication = "https://api.sandbox.nva.aws.unit"
                        + ".no/publication/0186305463c3-898f18b2-d1eb-47f3-a8e9-7beed4470dc9";
            var promotedPublications = List.of(mostBoostedPublication, secondBoostedPublication);
            var contributor = "1234";
            var query = queryWithTermAndAggregation(SEARCH_ALL, RESOURCES_AGGREGATIONS);
            var response =
                searchClient.searchWithSearchPromotedPublicationsForContributorQuery(
                    contributor,
                    promotedPublications,
                    query,
                    indexName);
            assertThat(response.getHits().get(0).toString(), containsString(mostBoostedPublication));
            assertThat(response.getHits().get(1).toString(), containsString(secondBoostedPublication));
            assertThat(response, notNullValue());
            assertThat(response.getAggregations(), notNullValue());
        }

        @Test
        void shouldNotReturnAggregationsWhenNotRequested()
            throws ApiGatewayException, InterruptedException {

            addDocumentsToIndex("sample_publishing_request_of_draft_publication.json",
                                "sample_publishing_request_of_published_publication.json");

            var query = queryWithTermAndAggregation(SEARCH_ALL, null);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response, notNullValue());
            assertThat(response.getAggregations(), nullValue());
        }

        @Test
        void shouldReturnCorrectAggregations() throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_publication_with_affiliations.json",
                                "sample_publication_with_several_of_the_same_affiliation.json");

            var query = queryWithTermAndAggregation(
                SEARCH_ALL, ApplicationConstants.RESOURCES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response, notNullValue());

            var actualAggregations = response.getAggregations();
            var topOrgAggregation = actualAggregations.at(
                "/topLevelOrganizations/id/buckets");
            assertAggregation(topOrgAggregation, "https://api.dev.nva.aws.unit.no/cristin/organization/185.0.0.0", 2);

            var typeAggregation = actualAggregations.at(
                "/entityDescription/reference/publicationInstance/type/buckets");
            assertAggregation(typeAggregation, "AcademicArticle", 2);

            var ownerAggregation = actualAggregations.at("/resourceOwner.owner/buckets");
            assertAggregation(ownerAggregation, "1136263@20754.0.0.0", 2);

            var ownerAffiliationAggregation = actualAggregations.at("/resourceOwner.ownerAffiliation/buckets");
            assertAggregation(ownerAffiliationAggregation, "https://www.example.org/Bergen", 1);

            var contributorAggregation = actualAggregations.at(
                "/entityDescription/contributors/identity/id/buckets/0/name/buckets");
            assertAggregation(contributorAggregation, "lametti, Stefania", 1);
        }

        @Test
        void shouldReturnPublicationWhenQueryingByProject() throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_publication_with_affiliations.json",
                                "sample_publication_with_several_of_the_same_affiliation.json");

            var query = queryWithTermAndAggregation(
                "projects.id:\"https://api.dev.nva.aws.unit.no/cristin/project/14334813\"",
                ApplicationConstants.RESOURCES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response.getHits(), hasSize(1));
        }

        @Test
        void shouldReturnPublicationWhenQueryingByTopLevelOrg() throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_publication_with_affiliations.json",
                                "sample_publication_with_several_of_the_same_affiliation.json");

            var query = queryWithTermAndAggregation(
                "topLevelOrganizations.id:\"https://api.dev.nva.aws.unit.no/cristin/organization/185.0.0.0\"",
                ApplicationConstants.RESOURCES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response.getHits(), hasSize(2));
        }

        @Test
        void shouldQueryingFundingSuccessfully() throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_publication_with_affiliations.json",
                                "sample_publication_with_several_of_the_same_affiliation.json");

            var query = queryWithTermAndAggregation(
                "fundings.source.identifier:\"NFR\"",
                ApplicationConstants.RESOURCES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);
            assertThat(response.getHits(), hasSize(2));
        }

        @Test
        void shouldQueryingHasFileSuccessfully() throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_publication_with_affiliations.json",
                                "sample_publication_with_several_of_the_same_affiliation.json");

            var query = queryWithTermAndAggregation(
           SEARCH_ALL, ApplicationConstants.RESOURCES_AGGREGATIONS);

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response, notNullValue());

            var actualAggregations = response.getAggregations();

            var hasPublishedFileAggregation = actualAggregations.at(
                "/associatedArtifacts/type");
            assertThat(hasPublishedFileAggregation.get("docCount").asInt(), is(equalTo(2)));

            var hasPublishedFileWithAdminAgreementFalseAggregation = actualAggregations.at(
                "/associatedArtifacts/type/administrativeAgreement");
            assertThat(hasPublishedFileWithAdminAgreementFalseAggregation.get("docCount").asInt(), is(equalTo(1)));
        }

        @Test
        void shouldNotReturnTicketsAggregationsWhenNotRequested() throws ApiGatewayException,
                                                                         InterruptedException {

            addDocumentsToIndex("sample_ticket_publishing_request_of_draft_publication.json",
                                "sample_ticket_general_support_case_of_published_publication.json");

            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchResponseDto = searchClient.searchWithSearchTicketQuery(viewingScope, searchTicketsQuery,
                                                                             indexName);

            assertThat(searchResponseDto, notNullValue());
            assertThat(searchResponseDto.getAggregations(), nullValue());
        }

        @Test
        void shouldReturnCorrectTicketsAggregations() throws InterruptedException, ApiGatewayException {
            addDocumentsToIndex("sample_ticket_publishing_request_of_draft_publication.json",
                                "sample_ticket_general_support_case_of_published_publication.json");

            var aggregations = ApplicationConstants.TICKETS_AGGREGATIONS;

            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchTicketsQuery = new SearchTicketsQuery(SEARCH_ALL, PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, aggregations);
            var searchResponseDto = searchClient.searchWithSearchTicketQuery(viewingScope, searchTicketsQuery,
                                                                             indexName);

            assertThat(searchResponseDto, notNullValue());

            var actualAggregations = searchResponseDto.getAggregations();

            var typeAggregation = actualAggregations.at("/type/"
                                                        + "buckets");
            assertThat(typeAggregation.size(), greaterThan(0));
            assertAggregation(typeAggregation, "GeneralSupportCase", 1);

            var statusAggregation = actualAggregations.at("/status/buckets");
            assertThat(statusAggregation.size(), greaterThan(0));
            assertAggregation(statusAggregation, "Pending", 2);
        }
    }
}
