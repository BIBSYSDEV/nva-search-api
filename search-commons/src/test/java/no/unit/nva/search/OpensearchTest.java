package no.unit.nva.search;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchResourcesResponse;
import no.unit.nva.search.restclients.responses.ViewingScope;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.BadGatewayException;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static no.unit.nva.search.SearchClient.*;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Testcontainers
public class OpensearchTest {

    public static final String INDEX_NAME = RandomDataGenerator.randomString().toLowerCase();
    public static final URI INCLUDED_ORGANIZATION_ID = randomUri();
    public static final URI EXCLUDED_ORGANIZATION_ID = randomUri();
    public static final int ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY = 0;
    public static final int TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS = 2;
    public static final int ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED = 1;
    public static final String STATUS_TO_INCLUDE_IN_RESULT = "Pending";
    public static final int ZERO_HITS_BECAUSE_APPROVED_WAS_FILTERED_OUT = 0;
    public static final long DELAY_AFTER_INDEXING = 1000L;
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_NO = 0;
    private static final URI ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES = URI.create("https://www.example.com/20754.0.0.0");
    private static final String COMPLETED = "Completed";
    private static final int CONTAINER_PORT = 9200;

    private SearchClient searchClient;
    private IndexingClient indexingClient;


    @SuppressWarnings("rawtypes")
    private final GenericContainer container = new GenericContainer(
            DockerImageName.parse("opensearchproject/opensearch:latest"))
            .withEnv("plugins.security.disabled", "true")
            .withEnv("discovery.type", "single-node");

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {

        container.setPortBindings(List.of(CONTAINER_PORT + ":" + CONTAINER_PORT));
        container.setExposedPorts(List.of(CONTAINER_PORT));
        container.start();

        var httpHostAddress = getContainerHost();

        var restClientBuilder = RestClient.builder(HttpHost.create(httpHostAddress));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);

        searchClient = new SearchClient(restHighLevelClientWrapper);
        indexingClient = new IndexingClient(restHighLevelClientWrapper);
    }

    @AfterEach
    void afterEach() {
        container.stop();
    }


    @Test
    void canConnectToContainer() throws IOException, InterruptedException{

        var httpHostAddress = getContainerHost();

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
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of()));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var response = searchClient.findResourcesForOrganizationIds(getEmptyViewingScope(),
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);

        assertThat(response.getHits().getHits().length,
                is(equalTo(ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY)));
    }

    @Test
    void shouldReturnTwoHitsOnViewingScopeWithIncludedUnit() throws Exception {
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));

        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);

        assertThat(response.getHits().getHits().length,
                is(equalTo(TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS)));
    }

    @Test
    void shouldReturnZeroHitsBecauseStatusIsCompleted() throws Exception {
        indexingClient.addDocumentToIndex(
                getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID), COMPLETED)
        );

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));

        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);

        assertThat(response.getHits().getHits().length,
                is(equalTo(ZERO_HITS_BECAUSE_APPROVED_WAS_FILTERED_OUT)));
    }

    @Test
    void shouldReturnOneHitOnViewingScopeWithExcludedUnit() throws Exception {
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID, EXCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));
        viewingScope.setExcludedUnits(Set.of(EXCLUDED_ORGANIZATION_ID));

        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);

        assertThat(response.getHits().getHits().length,
                is(equalTo(ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED)));
    }

    @Test
    void shouldCreateSearchResourcesResponseFromSearchResponse() throws Exception {
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID)));
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of(INCLUDED_ORGANIZATION_ID)));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(INCLUDED_ORGANIZATION_ID);
        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);
        var searchId = SearchResourcesResponse.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResourcesResponse.fromSearchResponse(response, searchId);

        assertThat(searchResourcesResponse, is(notNullValue()));
        assertThat(searchResourcesResponse.getId(), is(equalTo(searchId)));
        assertThat(searchResourcesResponse.getHits().size(), is(equalTo(2)));
    }

    @Test
    void shouldVerifySearchNotReturningHitsWithDraftPublicationRequestInSearchResponse() throws Exception {
        indexingClient.addDocumentToIndex(crateSampleIndexDocument(
                "sample_response_with_publication_status_as_draft.json"));
        indexingClient.addDocumentToIndex(crateSampleIndexDocument(
                "sample_response_with_publication_status_as_requested.json"));

        Thread.sleep(DELAY_AFTER_INDEXING);

        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);
        var searchId = SearchResourcesResponse.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResourcesResponse.fromSearchResponse(response, searchId);

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
                crateSampleIndexDocument("sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(
                crateSampleIndexDocument("sample_publishing_request_of_published_publication.json"));
        Thread.sleep(DELAY_AFTER_INDEXING);
        var viewingScope = ViewingScope.create(ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES);
        var response = searchClient.findResourcesForOrganizationIds(viewingScope,
                PAGE_SIZE,
                PAGE_NO,
                INDEX_NAME);

        var searchId = SearchResourcesResponse.createIdWithQuery(randomUri(), null);
        var searchResourcesResponse = SearchResourcesResponse.fromSearchResponse(response, searchId);
        assertThat(searchResourcesResponse, is(notNullValue()));
        var expectedHits = 1;
        assertThat(searchResourcesResponse.getHits().size(), is(equalTo(expectedHits)));


    }

    private ViewingScope getEmptyViewingScope() {
        return new ViewingScope();
    }

    private IndexDocument getIndexDocument(Set<URI> organizationIds) {
        return getIndexDocument(organizationIds, STATUS_TO_INCLUDE_IN_RESULT);
    }

    private IndexDocument getIndexDocument(Set<URI> organizationIds, String status) {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
                INDEX_NAME,
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

    private IndexDocument crateSampleIndexDocument(String jsonFile) throws IOException {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
                INDEX_NAME,
                SortableIdentifier.next()
        );
        var jsonNode = objectMapperWithEmpty.readValue(inputStreamFromResources(jsonFile),
                JsonNode.class);

        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }

    private String getContainerHost() {
        var host = container.getHost();
        return host + ":" + CONTAINER_PORT;
    }
}