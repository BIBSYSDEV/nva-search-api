package no.unit.nva.search;

import static java.util.Collections.emptyList;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.SearchClient.DOCUMENT_TYPE;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.ORGANIZATION_IDS;
import static no.unit.nva.search.SearchClient.TICKET_STATUS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.opensearch.search.sort.SortOrder.DESC;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.opensearch.client.RestClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OpensearchTest {

    private static final URI INCLUDED_ORGANIZATION_ID = randomUri();
    private static final URI EXCLUDED_ORGANIZATION_ID = randomUri();
    private static final long ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY = 0;
    private static final long TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS = 2;
    private static final long ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED = 1;
    private static final String STATUS_TO_INCLUDE_IN_RESULT = "Pending";
    private static final long NON_ZERO_HITS_BECAUSE_APPROVED_WAS_INCLUDED = 1;
    private static final long DELAY_AFTER_INDEXING = 1000L;
    private static final String TEST_RESOURCES_MAPPINGS = "test_resources_mappings.json";
    private static final String FIELD_VIEWED_BY = "viewedBy";
    private static final int SAMPLE_NUMBER_OF_RESULTS = 7;
    private static final int SAMPLE_FROM = 0;
    private static final String SAMPLE_ORDERBY = "orderByField";
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_NO = 0;
    private static final URI ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES = URI.create(
        "https://www.example.com/20754.0.0.0");
    private static final String COMPLETED = "Completed";
    private static final OpenSearchContainer container = new OpenSearchContainer();
    private static final String FIELD_OWNER = "owner";
    private static final String FIELD_ASSIGNEE = "assignee";
    private static SearchClient searchClient;
    private static IndexingClient indexingClient;
    private static String indexName;

    @BeforeAll
    static void setUp() {
        container.start();

        var httpHostAddress = container.getHttpHostAddress();

        var restClientBuilder = RestClient.builder(HttpHost.create(httpHostAddress));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);

        var cachedJwtProvider = setupMockedCachedJwtProvider();

        searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
    }

    @AfterAll
    static void afterAll() throws IOException {
        container.stop();
    }

    @Test
    void canConnectToContainer() throws IOException, InterruptedException {

        var httpHostAddress = container.getHttpHostAddress();

        var httpClient = HttpClient.newBuilder().build();

        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(URI.create("http://" + httpHostAddress))
                          .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(equalTo(200)));
    }

    @Test
    void shouldReturnCorrectMappingsFromIndexWhenIndexIsCreatedWithMappingAndAfterDocIsAdded()
        throws IOException,
               InterruptedException {
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
        var topLevelOrgType = mapping.path("properties").path("topLevelOrganization")
                                  .path("type").textValue();
        assertThat(topLevelOrgType, is(equalTo("nested")));
    }

    @Test
    void shouldReturnCreatorUnreadOfOneWhenOwnerBytNotInViewedIn()
        throws IOException, InterruptedException, ApiGatewayException {
        indexName = generateIndexName();
        var owner = "test@123.4.5.6";
        var document = Map.of(ORGANIZATION_IDS, INCLUDED_ORGANIZATION_ID,
                              FIELD_OWNER, Map.of("username", owner),
                              FIELD_VIEWED_BY, emptyList(),
                              DOCUMENT_TYPE, DOI_REQUEST,
                              TICKET_STATUS, STATUS_TO_INCLUDE_IN_RESULT);
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName, document));
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName, document));
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName, document));
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName,
                                                                    Map.of(ORGANIZATION_IDS, INCLUDED_ORGANIZATION_ID,
                                                                           FIELD_OWNER, Map.of("username", owner),
                                                                           FIELD_VIEWED_BY, List.of(owner),
                                                                           DOCUMENT_TYPE, DOI_REQUEST,
                                                                           TICKET_STATUS, STATUS_TO_INCLUDE_IN_RESULT)
        ));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var filterAggregationBuilder = new FilterAggregationBuilder(
            "creator.unread",
            new BoolQueryBuilder()
                .must(QueryBuilders.queryStringQuery("*"))
                .must(QueryBuilders.matchQuery("owner.username", owner).operator(Operator.AND))
                .mustNot(QueryBuilders.matchQuery(FIELD_VIEWED_BY, owner).operator(Operator.AND))
        );

        SearchDocumentsQuery query = new SearchDocumentsQuery(
            "*",
            SAMPLE_NUMBER_OF_RESULTS,
            SAMPLE_FROM,
            SAMPLE_ORDERBY,
            DESC,
            SAMPLE_REQUEST_URI,
            List.of(filterAggregationBuilder)
        );

        var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

        assertThat(response, notNullValue());

        var actualAggregations = response.getAggregations();
        var unread = actualAggregations.get("creator.unread");
        assertThat(unread.get("docCount").asInt(), is(equalTo(3)));
    }

    @Test
    void shouldReturnCuratorUnreadOfOneWhenAssigneeBytNotInViewedIn()
        throws IOException, InterruptedException, ApiGatewayException {
        indexName = generateIndexName();
        var assignee = "test@123.4.5.6";
        var document = Map.of(ORGANIZATION_IDS, INCLUDED_ORGANIZATION_ID,
                              FIELD_ASSIGNEE, Map.of("username", assignee),
                              FIELD_VIEWED_BY, emptyList(),
                              DOCUMENT_TYPE, DOI_REQUEST,
                              TICKET_STATUS, STATUS_TO_INCLUDE_IN_RESULT);
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName, document));
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName, document));
        indexingClient.addDocumentToIndex(createTicketIndexDocument(indexName, document));
        indexingClient.addDocumentToIndex(createTicketIndexDocument(
            indexName,
            Map.of(ORGANIZATION_IDS, INCLUDED_ORGANIZATION_ID,
                   FIELD_ASSIGNEE, Map.of("username", assignee),
                   FIELD_VIEWED_BY, List.of(assignee),
                   DOCUMENT_TYPE, DOI_REQUEST,
                   TICKET_STATUS, STATUS_TO_INCLUDE_IN_RESULT)
        ));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var filterAggregationBuilder = new FilterAggregationBuilder(
            "curator.unread",
            new BoolQueryBuilder()
                .must(QueryBuilders.queryStringQuery("*"))
                .must(QueryBuilders.matchQuery("assignee.username", assignee).operator(Operator.AND))
                .mustNot(QueryBuilders.matchQuery(FIELD_VIEWED_BY, assignee).operator(Operator.AND))
        );

        SearchDocumentsQuery query = new SearchDocumentsQuery(
            "*",
            SAMPLE_NUMBER_OF_RESULTS,
            SAMPLE_FROM,
            SAMPLE_ORDERBY,
            DESC,
            SAMPLE_REQUEST_URI,
            List.of(filterAggregationBuilder)
        );

        var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

        assertThat(response, notNullValue());

        var actualAggregations = response.getAggregations();
        var unread = actualAggregations.get("curator.unread");
        assertThat(unread.get("docCount").asInt(), is(equalTo(3)));
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

    private IndexDocument createTicketIndexDocument(String indexName, Map<String, Object> map) {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
            indexName,
            SortableIdentifier.next()
        );
        var jsonNode = objectMapperWithEmpty.convertValue(map, JsonNode.class);
        return new IndexDocument(eventConsumptionAttributes, jsonNode);
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
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
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
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
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
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
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
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
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
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var searchResourcesResponse = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                                   searchTicketsQuery,
                                                                                   indexName);

            assertThat(searchResourcesResponse, is(notNullValue()));
            assertThat(searchResourcesResponse.getHits().size(), is(equalTo(2)));
        }

        @Test
        void shouldVerifySearchNotReturningHitsWithPublicationRequestInSearchResponse() throws Exception {

            indexingClient.addDocumentToIndex(crateSampleIndexDocument(
                indexName,
                "sample_response_with_publication_status_as_draft.json"));
            indexingClient.addDocumentToIndex(crateSampleIndexDocument(
                indexName,
                "sample_response_with_publication_status_as_requested.json"));

            Thread.sleep(DELAY_AFTER_INDEXING);

            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var searchResourcesResponse = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                                   searchTicketsQuery,
                                                                                   indexName);

            assertThat(searchResourcesResponse, is(notNullValue()));
            var actualHitsExcludingHitsWithPublicationStatusDraft = 1;
            assertThat(searchResourcesResponse.getHits().size(),
                       is(equalTo(actualHitsExcludingHitsWithPublicationStatusDraft)));
        }

        @Test
        void shouldReturnPendingPublishingRequestsForPublications()
            throws IOException, InterruptedException, ApiGatewayException {

            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_publishing_request_of_published_publication.json"));
            Thread.sleep(DELAY_AFTER_INDEXING);

            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var searchResourcesResponse = searchClient.searchWithSearchTicketQuery(viewingScope,
                                                                                   searchTicketsQuery,
                                                                                   indexName);

            assertThat(searchResourcesResponse, is(notNullValue()));
            var expectedHits = 2;
            assertThat(searchResourcesResponse.getHits().size(), is(equalTo(expectedHits)));
        }

        @Test
        void shuldReturnCorrectNumberOfBucketsWhenRequestedNonDefaultAmount() throws ApiGatewayException, IOException,
                                                                                     InterruptedException {

            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName,
                                         "sample_publishing_request_of_published_publication.json"));
            Thread.sleep(DELAY_AFTER_INDEXING);

            var aggregationDto = new TermsAggregationBuilder("publication.status")
                                     .field("publication.status.keyword")
                                     .size(1);

            SearchDocumentsQuery query = new SearchDocumentsQuery(
                "*",
                SAMPLE_NUMBER_OF_RESULTS,
                SAMPLE_FROM,
                SAMPLE_ORDERBY,
                DESC,
                SAMPLE_REQUEST_URI,
                List.of(aggregationDto)
            );

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response.getAggregations()
                           .get("publication.status")
                           .get("buckets")
                           .size(),
                       is(equalTo(1))
            );
        }

        @Test
        void shouldNotReturnAggregationsWhenNotRequested()
            throws ApiGatewayException, IOException, InterruptedException {

            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName,
                                         "sample_publishing_request_of_published_publication.json"));
            Thread.sleep(DELAY_AFTER_INDEXING);

            SearchDocumentsQuery query = new SearchDocumentsQuery(
                "*",
                SAMPLE_NUMBER_OF_RESULTS,
                SAMPLE_FROM,
                SAMPLE_ORDERBY,
                DESC,
                SAMPLE_REQUEST_URI,
                null
            );

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response, notNullValue());
            assertThat(response.getAggregations(), nullValue());
        }

        @Test
        void shouldReturnCorrectAggregations() throws IOException, InterruptedException, ApiGatewayException {
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_publication_with_affiliations.json"));
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName,
                                         "sample_publication_with_several_of_the_same_affiliation.json"));
            Thread.sleep(DELAY_AFTER_INDEXING);

            var aggregations = ApplicationConstants.RESOURCES_AGGREGATIONS;

            SearchDocumentsQuery query = new SearchDocumentsQuery(
                "*",
                SAMPLE_NUMBER_OF_RESULTS,
                SAMPLE_FROM,
                SAMPLE_ORDERBY,
                DESC,
                SAMPLE_REQUEST_URI,
                aggregations
            );

            var response = searchClient.searchWithSearchDocumentQuery(query, indexName);

            assertThat(response, notNullValue());

            var actualAggregations = response.getAggregations();
            var topOrgAggregation = actualAggregations.at("/entityDescription.contributors.affiliations"
                                                          + ".topLevelOrganization.id/buckets");
            assertAggregation(topOrgAggregation, "https://api.dev.nva.aws.unit.no/cristin/organization/185.0.0.0", 2);

            var typeAggregation = actualAggregations.at("/entityDescription.reference.publicationInstance.type/"
                                                        + "buckets");
            assertAggregation(typeAggregation, "AcademicArticle", 2);

            var ownerAggregation = actualAggregations.at("/resourceOwner.owner/buckets");
            assertAggregation(ownerAggregation, "fredrikTest@unit.no", 1);

            var ownerAffiliationAggregation = actualAggregations.at("/resourceOwner.ownerAffiliation/buckets");
            assertAggregation(ownerAffiliationAggregation, "https://www.example.org/Bergen", 1);

            var contributorAggregation = actualAggregations.at("/entityDescription.contributors.identity.name/"
                                                               + "buckets");
            assertAggregation(contributorAggregation, "lametti, Stefania", 2);
        }

        @Test
        void shouldNotReturnTicketsAggregationsWhenNotRequested() throws ApiGatewayException, IOException,
                                                                         InterruptedException {

            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_ticket_publishing_request_of_draft_publication.json"));
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName,
                                         "sample_ticket_general_support_case_of_published_publication.json"));
            Thread.sleep(DELAY_AFTER_INDEXING);

            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
                                                            DESC, SAMPLE_REQUEST_URI, emptyList());
            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchResponseDto = searchClient.searchWithSearchTicketQuery(viewingScope, searchTicketsQuery,
                                                                             indexName);

            assertThat(searchResponseDto, notNullValue());
            assertThat(searchResponseDto.getAggregations(), nullValue());
        }

        @Test
        void shouldReturnCorrectTicketsAggregations() throws IOException, InterruptedException, ApiGatewayException {

            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName, "sample_ticket_publishing_request_of_draft_publication.json"));
            indexingClient.addDocumentToIndex(
                crateSampleIndexDocument(indexName,
                                         "sample_ticket_general_support_case_of_published_publication.json"));
            Thread.sleep(DELAY_AFTER_INDEXING);

            var aggregations = ApplicationConstants.TICKETS_AGGREGATIONS;

            var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
            var searchTicketsQuery = new SearchTicketsQuery("*", PAGE_SIZE, PAGE_NO, SAMPLE_ORDERBY,
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
