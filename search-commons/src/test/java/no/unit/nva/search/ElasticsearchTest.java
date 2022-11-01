package no.unit.nva.search;

import static no.unit.nva.search.SearchClient.DOCUMENT_TYPE;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.ORGANIZATION_IDS;
import static no.unit.nva.search.SearchClient.TICKET_STATUS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.models.SearchResourcesResponse;
import no.unit.nva.search.restclients.responses.ViewingScope;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.BadGatewayException;
import org.apache.http.HttpHost;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class ElasticsearchTest {

    public static final String ELASTICSEARCH_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";
    public static final String INDEX_NAME = RandomDataGenerator.randomString().toLowerCase();
    public static final URI INCLUDED_ORGANIZATION_ID = randomUri();
    public static final URI EXCLUDED_ORGANIZATION_ID = randomUri();
    public static final int ZERO_HITS_BECAUSE_VIEWING_SCOPE_IS_EMPTY = 0;
    public static final int TWO_HITS_BECAUSE_MATCH_ON_BOTH_INCLUDED_UNITS = 2;
    public static final int ONE_HIT_BECAUSE_ONE_UNIT_WAS_EXCLUDED = 1;
    public static final String STATUS_TO_INCLUDE_IN_RESULT = "Pending";
    public static final int ZERO_HITS_BECAUSE_APPROVED_WAS_FILTERED_OUT = 0;
    public static final long DELAY_AFTER_INDEXING = 1000L;
    private static final String ELASTICSEARCH_VERSION = "7.10.2";
    private static final int PAGE_SIZE = 10;
    private static final int PAGE_NO = 0;
    private static final URI ORGANIZATION_ID_URI_HARDCODED_IN_SAMPLE_FILES = URI.create("https://www.example.com/20754.0.0.0");
    private static final String COMPLETED = "Completed";
    
    @Container
    public ElasticsearchContainer container = new ElasticsearchContainer(DockerImageName
                                                                             .parse(ELASTICSEARCH_OSS)
                                                                             .withTag(ELASTICSEARCH_VERSION));
    private SearchClient searchClient;
    private IndexingClient indexingClient;

    @BeforeEach
    void setUp() {
        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        RestHighLevelClientWrapper restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);

        searchClient = new SearchClient(restHighLevelClientWrapper);
        indexingClient = new IndexingClient(restHighLevelClientWrapper);
    }

    @Test
    void shouldReturnZeroHitsOnEmptyViewingScope() throws Exception {
        indexingClient.addDocumentToIndex(getIndexDocument(Set.of()));

        Thread.sleep(DELAY_AFTER_INDEXING);

        SearchResponse response = searchClient.findResourcesForOrganizationIds(getEmptyViewingScope(),
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

        ViewingScope viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));

        SearchResponse response = searchClient.findResourcesForOrganizationIds(viewingScope,
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

        ViewingScope viewingScope = getEmptyViewingScope();
        viewingScope.setIncludedUnits(Set.of(INCLUDED_ORGANIZATION_ID));

        SearchResponse response = searchClient.findResourcesForOrganizationIds(viewingScope,
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

        SearchResponse response = searchClient.findResourcesForOrganizationIds(viewingScope,
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
        indexingClient.addDocumentToIndex(crateSampleIndexDocument("sample_publishing_request_of_draft_publication.json"));
        indexingClient.addDocumentToIndex(crateSampleIndexDocument("sample_publishing_request_of_published_publication"
                                                                   + ".json"));
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
        EventConsumptionAttributes eventConsumptionAttributes = new EventConsumptionAttributes(
            INDEX_NAME,
            SortableIdentifier.next()
        );
        Map<String, Object> map = Map.of(
            ORGANIZATION_IDS, organizationIds,
            DOCUMENT_TYPE, DOI_REQUEST,
            TICKET_STATUS, status
        );
        JsonNode jsonNode = objectMapperWithEmpty.convertValue(map, JsonNode.class);
        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }

    private IndexDocument crateSampleIndexDocument(String jsonFile) throws IOException {
        EventConsumptionAttributes eventConsumptionAttributes = new EventConsumptionAttributes(
            INDEX_NAME,
            SortableIdentifier.next()
        );
        JsonNode jsonNode = objectMapperWithEmpty.readValue(inputStreamFromResources(jsonFile),
                                                            JsonNode.class);

        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }
}