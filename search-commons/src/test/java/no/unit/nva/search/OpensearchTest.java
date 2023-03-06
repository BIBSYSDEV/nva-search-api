package no.unit.nva.search;

import static java.util.Collections.emptyList;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.SearchClient.DOCUMENT_TYPE;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.ORGANIZATION_IDS;
import static no.unit.nva.search.SearchClient.TICKET_STATUS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.opensearch.search.sort.SortOrder.DESC;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.constants.ApplicationConstants;
import no.unit.nva.search.models.AggregationDto;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.SearchTicketsQuery;
import no.unit.nva.search.restclients.responses.ViewingScope;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class OpensearchTest {

    private static final int SAMPLE_NUMBER_OF_RESULTS = 7;
    private static final int SAMPLE_FROM = 0;
    private static final String SAMPLE_ORDERBY = "orderByField";
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    public static final URI INCLUDED_ORGANIZATION_ID = randomUri();
    private static final List<AggregationDto> SAMPLE_AGGREGATIONS = List.of(
        new AggregationDto(randomString(), randomString()));
    public static final URI EXCLUDED_ORGANIZATION_ID = randomUri();
    public static final int ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY = 0;
    public static final int TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS = 2;
    public static final int ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED = 1;
    public static final String STATUS_TO_INCLUDE_IN_RESULT = "Pending";
    public static final int ZERO_HITS_BECAUSE_APPROVED_WAS_FILTERED_OUT = 0;
    public static final long DELAY_AFTER_INDEXING = 1000L;
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_NO = 0;
    private static final URI ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES = URI.create(
        "https://www.example.com/20754.0.0.0");
    private static final String COMPLETED = "Completed";

    private static SearchClient searchClient;
    private static IndexingClient indexingClient;
    private static final OpenSearchContainer container = new OpenSearchContainer();
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
    static void afterAll() throws Exception {
        indexingClient.deleteIndex(indexName);
        container.stop();
    }

    @BeforeEach
    void beforeEachTest() {
        indexName = generateIndexName();
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
    void shouldReturnZeroHitsOnEmptyViewingScope() throws Exception {

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of()));

        Thread.sleep(DELAY_AFTER_INDEXING);
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var response = searchClient.findTicketsForOrganizationIds(getEmptyViewingScope(),
                                                                  searchTicketsQuery,
                                                                  indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY)));
    }

    private static String generateIndexName() {
        return RandomDataGenerator.randomString().toLowerCase();
    }

    @Test
    void shouldReturnTwoHitsOnViewingScopeWithIncludedUnit() throws Exception {

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var response = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                  searchTicketsQuery,
                                                                  indexName);

        assertThat(response.getHits().getHits().length,
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
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var response = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                  searchTicketsQuery,
                                                                  indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(ZERO_HITS_BECAUSE_APPROVED_WAS_FILTERED_OUT)));
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
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var response = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                  searchTicketsQuery,
                                                                  indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED)));
    }

    @Test
    void shouldCreateSearchTicketsResponseFromSearchResponse() throws Exception {

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(INCLUDED_ORGANIZATION_ID);
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var response = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                  searchTicketsQuery,
                                                                  indexName);
        var searchId = SearchResponseDto.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResponseDto.fromSearchResponse(response, searchId);

        assertThat(searchResourcesResponse, is(notNullValue()));
        assertThat(searchResourcesResponse.getId(), is(equalTo(searchId)));
        assertThat(searchResourcesResponse.getHits().size(), is(equalTo(2)));
    }

    @Test
    void shouldVerifySearchNotReturningHitsWithDraftPublicationRequestInSearchResponse() throws Exception {

        indexingClient.addDocumentToIndex(crateSampleIndexDocument(
            indexName,
            "sample_response_with_publication_status_as_draft.json"));
        indexingClient.addDocumentToIndex(crateSampleIndexDocument(
            indexName,
            "sample_response_with_publication_status_as_requested.json"));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var response = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                  searchTicketsQuery,
                                                                  indexName);
        var searchId = SearchResponseDto.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResponseDto.fromSearchResponse(response, searchId);

        assertThat(searchResourcesResponse, is(notNullValue()));
        assertThat(searchResourcesResponse.getId(), is(equalTo(searchId)));
        var actualHitsExcludingHitsWithPublicationStatusDraft = 1;
        assertThat(searchResourcesResponse.getHits().size(),
                   is(equalTo(actualHitsExcludingHitsWithPublicationStatusDraft)));
    }

    @Test
    void shouldReturnPendingPublishingRequestsForDraftPublications()
        throws IOException, InterruptedException, BadGatewayException {

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        var aggregations = ApplicationConstants.TICKETS_AGGREGATIONS;
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, aggregations);
        var response = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                  searchTicketsQuery,
                                                                  indexName);

        var searchId = SearchResponseDto.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResponseDto.fromSearchResponse(response, searchId);
        assertThat(searchResourcesResponse, is(notNullValue()));
        var expectedHits = 1;
        assertThat(searchResourcesResponse.getHits().size(), is(equalTo(expectedHits)));
    }

    @Test
    void shuldReturnCorrectNumberOfBucketsWhenRequestedNonDefaultAmount() throws ApiGatewayException, IOException,
                                                                                 InterruptedException {

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);

        var aggregationDto = new AggregationDto("publication.status",
                                                "publication.status.keyword");
        aggregationDto.setAggregationBucketAmount(1);

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
    void shouldNotReturnAggregationsWhenNotRequested() throws ApiGatewayException, IOException, InterruptedException {

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_published_publication.json"));
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
            crateSampleIndexDocument(indexName, "sample_publication_with_several_of_the_same_affiliation.json"));
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

    void assertAggregation(JsonNode aggregationNode, String key, int expectedDocCount) {
        aggregationNode.forEach(
            bucketNode -> {
                if (bucketNode.get("key").asText().equals(key)) {
                    assertThat(bucketNode.get("docCount").asInt(), is(equalTo(expectedDocCount)));
                }
            }
        );
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

    @Test
    void shouldNotReturnTicketsAggregationsWhenNotRequested() throws ApiGatewayException, IOException,
                                                                     InterruptedException {

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_ticket_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_ticket_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);

        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, emptyList());
        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        var ticketsSearchResponse = searchClient.findTicketsForOrganizationIds(viewingScope, searchTicketsQuery,
                                                                               indexName);

        SearchResponseDto searchResponseDto = SearchResponseDto.fromSearchResponse(ticketsSearchResponse,
                                                                                   SAMPLE_REQUEST_URI);

        assertThat(searchResponseDto, notNullValue());
        assertThat(searchResponseDto.getAggregations(), nullValue());
    }

    @Test
    void shouldReturnCorrectTicketsAggregations() throws IOException, InterruptedException, ApiGatewayException {

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_ticket_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_ticket_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);

        var aggregations = ApplicationConstants.TICKETS_AGGREGATIONS;

        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        SearchTicketsQuery searchTicketsQuery = new SearchTicketsQuery(PAGE_SIZE, PAGE_NO, aggregations);
        var ticketsSearchResponse = searchClient.findTicketsForOrganizationIds(viewingScope, searchTicketsQuery,
                                                                               indexName);

        SearchResponseDto searchResponseDto = SearchResponseDto.fromSearchResponse(ticketsSearchResponse,
                                                                                   SAMPLE_REQUEST_URI);

        assertThat(searchResponseDto, notNullValue());

        var actualAggregations = searchResponseDto.getAggregations();

        var typeAggregation = actualAggregations.at("/type/"
                                                    + "buckets");
        assertAggregation(typeAggregation, "GeneralSupportCase", 1);

        var ownerAggregation = actualAggregations.at("/publication.status/buckets");
        assertAggregation(ownerAggregation, "DRAFT", 1);
    }
}
