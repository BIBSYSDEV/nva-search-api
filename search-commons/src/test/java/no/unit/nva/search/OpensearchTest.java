package no.unit.nva.search;

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
import no.unit.nva.search.models.AggregationDto;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.restclients.responses.ViewingScope;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
    static void afterAll() {
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
    void shouldReturnZeroHitsOnEmptyViewingScope() throws Exception {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of()));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var response = searchClient.findResourcesForOrganizationIds(getEmptyViewingScope(),
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY)));
    }

    private static String generateIndexName() {
        return RandomDataGenerator.randomString().toLowerCase();
    }

    @Test
    void shouldReturnTwoHitsOnViewingScopeWithIncludedUnit() throws Exception {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));

        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS)));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldReturnZeroHitsBecauseStatusIsCompleted() throws Exception {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(
            getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID), COMPLETED)
        );

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));

        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(ZERO_HITS_BECAUSE_APPROVED_WAS_FILTERED_OUT)));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldReturnOneHitOnViewingScopeWithExcludedUnit() throws Exception {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID,
                                                                             EXCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));
        viewingScope.setExcludedUnits(Set.of(EXCLUDED_ORGANIZATION_ID));

        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);

        assertThat(response.getHits().getHits().length,
                   is(equalTo(ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED)));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldCreateSearchResourcesResponseFromSearchResponse() throws Exception {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(indexName, Set.of(INCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(INCLUDED_ORGANIZATION_ID);
        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);
        var searchId = SearchResponseDto.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResponseDto.fromSearchResponse(response, searchId);

        assertThat(searchResourcesResponse, is(notNullValue()));
        assertThat(searchResourcesResponse.getId(), is(equalTo(searchId)));
        assertThat(searchResourcesResponse.getHits().size(), is(equalTo(2)));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldVerifySearchNotReturningHitsWithDraftPublicationRequestInSearchResponse() throws Exception {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(crateSampleIndexDocument(
            indexName,
            "sample_response_with_publication_status_as_draft.json"));
        indexingClient.addDocumentToIndex(crateSampleIndexDocument(
            indexName,
            "sample_response_with_publication_status_as_requested.json"));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);
        var searchId = SearchResponseDto.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResponseDto.fromSearchResponse(response, searchId);

        assertThat(searchResourcesResponse, is(notNullValue()));
        assertThat(searchResourcesResponse.getId(), is(equalTo(searchId)));
        var actualHitsExcludingHitsWithPublicationStatusDraft = 1;
        assertThat(searchResourcesResponse.getHits().size(),
                   is(equalTo(actualHitsExcludingHitsWithPublicationStatusDraft)));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldReturnPendingPublishingRequestsForDraftPublications()
        throws IOException, InterruptedException, BadGatewayException {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);
        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                                                                    PAGE_SIZE,
                                                                    PAGE_NO,
                                                                    indexName);

        var searchId = SearchResponseDto.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResponseDto.fromSearchResponse(response, searchId);
        assertThat(searchResourcesResponse, is(notNullValue()));
        var expectedHits = 1;
        assertThat(searchResourcesResponse.getHits().size(), is(equalTo(expectedHits)));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldReturnAggregations() throws ApiGatewayException, IOException, InterruptedException {
        var indexName = generateIndexName();

        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
            crateSampleIndexDocument(indexName, "sample_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);

        var aggregationDto = new AggregationDto(
            "contributors", "publication.contributors.identity.name.keyword",
            new AggregationDto("affiliations", "publication.contributors.affiliations.id.keyword")
        );

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

        assertThat(response, notNullValue());
        var aggregations = response.getAggregations()
            .at("/contributors/buckets");
        assertThat(aggregations.size(), is(equalTo(2)));

        var affiliation = aggregations.get(0).at("/affiliations/buckets").get(0).at("/key");
        assertThat("org is on right format",
                   affiliation.asText().matches("https://api.dev.nva.aws.unit.no/cristin/organization/.*"));

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shuldReturnCorrectNumberOfBucketsWhenRequestedNonDefaultAmount() throws ApiGatewayException, IOException,
                                                                                 InterruptedException {
        var indexName = generateIndexName();

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

        indexingClient.deleteIndex(indexName);
    }

    @Test
    void shouldNotReturnAggregationsWhenNotRequested() throws ApiGatewayException, IOException, InterruptedException {
        var indexName = generateIndexName();

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

        indexingClient.deleteIndex(indexName);
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
}