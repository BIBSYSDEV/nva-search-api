package no.unit.nva.search;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static java.util.Collections.emptyList;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.indexing.testutils.TestConstants.TEST_TOKEN;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.DRAFT_PUBLICATION_STATUS;
import static no.unit.nva.search.SearchClient.GENERAL_SUPPORT_CASE;
import static no.unit.nva.search.SearchClient.PENDING;
import static no.unit.nva.search.SearchClient.PUBLISHING_REQUEST;
import static no.unit.nva.search.SearchClient.prepareWithSecretReader;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_TICKET_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opensearch.search.sort.SortOrder.DESC;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.models.AggregationDto;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.SearchTicketsQuery;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search.restclients.responses.ViewingScope;
import no.unit.nva.search.utils.RequestOptionsHeaderMatcher;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.secrets.SecretsReader;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.ExistsQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;

class SearchClientTest {

    public static final String SAMPLE_TERM = "SampleSearchTerm";
    public static final int MAX_RESULTS = 100;
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NO = 0;
    private static final int SAMPLE_NUMBER_OF_RESULTS = 7;
    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";
    private static final int SAMPLE_FROM = 0;
    private static final String SAMPLE_ORDERBY = "orderByField";
    private static final String OPENSEARCH_SAMPLE_RESPONSE_FILE = "sample_opensearch_response.json";
    private static final String OPENSEARCH_SAMPLE_TICKET_RESPONSE_FILE = "sample_opensearch_tickets_response.json";
    private static final int OPENSEARCH_ACTUAL_SAMPLE_NUMBER_OF_RESULTS = 2;
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    private static final List<AggregationDto> SAMPLE_AGGREGATIONS = List.of(
        new AggregationDto(randomString(), randomString()));
    public static final String EXPECTED_AGGREGATIONS = "sample_opensearch_response_searchresponsedto_aggregations.json";
    public static final String EXPECTED_TICKETS_AGGREGATIONS =
        "sample_opensearch_ticket_response_searchresponsedto_aggregations.json";

    SearchResponse defaultSearchResponse = mock(SearchResponse.class);

    CachedJwtProvider cachedJwtProvider;

    @BeforeEach
    void setup() {
        cachedJwtProvider = setupMockedCachedJwtProvider();
    }

    @Test
    void constructorWithSecretsReaderDefinedShouldCreateInstance() {
        var secretsReaderMock = mock(SecretsReader.class);
        var testCredentials = new UsernamePasswordWrapper("user", "password");
        when(secretsReaderMock.fetchClassSecret(anyString(), eq(UsernamePasswordWrapper.class)))
            .thenReturn(testCredentials);

        var searchClient = prepareWithSecretReader(secretsReaderMock);
        assertNotNull(searchClient);
    }

    @Test
    void shouldPassBearerTokenToOpensearchWhenPerformingSearch() throws ApiGatewayException, IOException {

        var restClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);

        var expectedRequestOptions = RequestOptions.DEFAULT
            .toBuilder()
            .addHeader(AUTHORIZATION, "Bearer " + TEST_TOKEN)
            .build();

        when(restClient.search(any(), argThat(new RequestOptionsHeaderMatcher(expectedRequestOptions))))
            .thenReturn(searchResponse);

        var restClientWrapper = new RestHighLevelClientWrapper(restClient);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);

        var result = searchClient.searchWithSearchDocumentQuery(generateSampleQuery(),
                                                                OPENSEARCH_ENDPOINT_INDEX);

        assertNotNull(result);
    }

    @Test
    void shouldSendQueryWithAllNeededRulesForDoiRequestsTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingPublicationConversation = extractQueryBuilderValuesForDoiRequests(sentRequest);
        assertThat(rulesForIncludingPublicationConversation, hasItem(DRAFT_PUBLICATION_STATUS));
        assertThat(rulesForIncludingPublicationConversation, hasItem(DOI_REQUEST));
    }

    @Test
    void shouldSendQueryWithAllNeededRulesForPublicationConversationTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingPublicationConversation = extractQueryBuilderValuesForPublicationConversation(sentRequest);
        assertThat(rulesForIncludingPublicationConversation, hasItem(GENERAL_SUPPORT_CASE));
    }

    @Test
    void searchSingleTermReturnsResponse() throws ApiGatewayException, IOException {
        var restHighLevelClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient =
            new SearchClient(new RestHighLevelClientWrapper(restHighLevelClient), cachedJwtProvider);
        SearchResponseDto searchResponseDto =
            searchClient.searchWithSearchDocumentQuery(generateSampleQuery(), OPENSEARCH_ENDPOINT_INDEX);
        assertNotNull(searchResponseDto);
    }

    @Test
    void shouldReturnTicketsSearchResponseWhenSearchingWithOrganizationIds() throws ApiGatewayException, IOException {
        RestHighLevelClient restHighLevelClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient =
            new SearchClient(new RestHighLevelClientWrapper(restHighLevelClient), cachedJwtProvider);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       emptyList());
        var response =
            searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                       searchTicketsQuery,
                                                       OPENSEARCH_TICKET_ENDPOINT_INDEX);
        assertNotNull(response);
    }

    @Test
    void shouldSendRequestWithSuppliedPageSizeWhenSearchingForTickets() throws ApiGatewayException,
                                                                               IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        int resultSize = 1 + randomInteger(1000);
        var searchTicketsQuery = new SearchTicketsQuery(resultSize, DEFAULT_PAGE_NO,
                                                                       emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var actualRequestedSize = sentRequest.source().size();
        assertThat(actualRequestedSize, is(equalTo(resultSize)));
    }

    @Test
    void shouldSendTicketsRequestWithFirstEntryIndexCalculatedBySuppliedPageSizeAndPageNumber()
        throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        int pageNo = randomInteger(100);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, pageNo, emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var actualResultsFrom = sentRequest.source().from();
        var resultsFrom = pageNo * DEFAULT_PAGE_SIZE;
        assertThat(actualResultsFrom, is(equalTo(resultsFrom)));
    }

    @Test
    void shouldSendResourcesRequestWithAggregations() throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var nestedAggregationDTOs = List.of(
            new AggregationDto(
                randomString(),
                randomString(),
                new AggregationDto(
                    randomString(),
                    randomString(),
                    SAMPLE_AGGREGATIONS.get(0)
                )
            ),
            new AggregationDto(
                randomString(),
                randomString()
            )
        );

        SearchDocumentsQuery sampleQuery = new SearchDocumentsQuery(
            SAMPLE_TERM,
            SAMPLE_NUMBER_OF_RESULTS,
            SAMPLE_FROM,
            SAMPLE_ORDERBY,
            DESC,
            SAMPLE_REQUEST_URI,
            nestedAggregationDTOs
        );
        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        searchClient.searchWithSearchDocumentQuery(sampleQuery, OPENSEARCH_ENDPOINT_INDEX);

        var sentRequest = sentRequestBuffer.get();
        var actualAggregation = objectMapper.readTree(sentRequest.source().aggregations().toString());

        nestedAggregationDTOs.forEach(
            nestedAggregationDTO -> assertAggregationHasField(actualAggregation, nestedAggregationDTO));
    }

    private void assertAggregationHasField(JsonNode json, AggregationDto aggregationDto) {
        var actualField = json.at("/" + aggregationDto.term + "/terms/field").asText();
        assertThat(actualField, is(equalTo(aggregationDto.field)));

        if (aggregationDto.subAggregation != null) {
            assertAggregationHasField(json.at("/" + aggregationDto.term + "/aggregations"),
                                      aggregationDto.subAggregation);
        }
    }

    @Test
    void searchSingleTermReturnsResourcesResponseWithStatsFromOpensearch() throws ApiGatewayException, IOException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        var openSearchResponseJson = generateOpenSearchResponseAsString(OPENSEARCH_SAMPLE_RESPONSE_FILE);
        var searchResponse = getSearchResponseFromJson(openSearchResponseJson);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);

        SearchDocumentsQuery queryWithMaxResults = new SearchDocumentsQuery(SAMPLE_TERM,
                                                                            MAX_RESULTS,
                                                                            SAMPLE_FROM,
                                                                            SAMPLE_ORDERBY,
                                                                            DESC,
                                                                            SAMPLE_REQUEST_URI,
                                                                            SAMPLE_AGGREGATIONS);

        SearchResponseDto searchResponseDto =
            searchClient.searchWithSearchDocumentQuery(queryWithMaxResults, OPENSEARCH_ENDPOINT_INDEX);
        assertNotNull(searchResponseDto);
        assertEquals(searchResponseDto.getSize(), OPENSEARCH_ACTUAL_SAMPLE_NUMBER_OF_RESULTS);
    }

    @Test
    void searchSingleTermReturnsErrorResponseWhenExceptionInResourcesDoSearch() throws IOException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.search(any(), any())).thenThrow(new IOException());
        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);
        assertThrows(BadGatewayException.class,
                     () -> searchClient.searchWithSearchDocumentQuery(generateSampleQuery(),
                                                                      OPENSEARCH_ENDPOINT_INDEX));
    }

    @Test
    void resourcesSearchResponseShouldFormatAggregationsCorrectly() throws IOException, ApiGatewayException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        var openSearchResponseJson = generateOpenSearchResponseAsString(OPENSEARCH_SAMPLE_RESPONSE_FILE);
        var searchResponse = getSearchResponseFromJson(openSearchResponseJson);

        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);

        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);
        SearchDocumentsQuery sampleQuery = new SearchDocumentsQuery(
            SAMPLE_TERM,
            SAMPLE_NUMBER_OF_RESULTS,
            SAMPLE_FROM,
            SAMPLE_ORDERBY,
            DESC,
            SAMPLE_REQUEST_URI,
            SAMPLE_AGGREGATIONS
        );

        SearchResponseDto searchResponseDto =
            searchClient.searchWithSearchDocumentQuery(sampleQuery, OPENSEARCH_ENDPOINT_INDEX);

        var aggregations = searchResponseDto.getAggregations();

        var expected = objectMapperWithEmpty.readValue(inputStreamFromResources(EXPECTED_AGGREGATIONS),
                                                       JsonNode.class);

        assertThat(aggregations, is(Matchers.equalTo(expected)));
    }

    @Test
    void shouldSendTicketsRequestWithAggregations() throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var nestedAggregationDTOs = List.of(
            new AggregationDto(
                randomString(),
                randomString(),
                new AggregationDto(
                    randomString(),
                    randomString(),
                    SAMPLE_AGGREGATIONS.get(0)
                )
            ),
            new AggregationDto(
                randomString(),
                randomString()
            )
        );

        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       nestedAggregationDTOs);
        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(), searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);

        var sentRequest = sentRequestBuffer.get();
        var actualAggregation = objectMapper.readTree(sentRequest.source().aggregations().toString());

        nestedAggregationDTOs.forEach(
            nestedAggregationDTO -> assertAggregationHasField(actualAggregation, nestedAggregationDTO));
    }

    @Test
    void searchTicketsResponseShouldFormatAggregationsCorrectly() throws IOException, ApiGatewayException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        var openSearchResponseJson = generateOpenSearchResponseAsString(OPENSEARCH_SAMPLE_TICKET_RESPONSE_FILE);
        var searchResponse = getSearchResponseFromJson(openSearchResponseJson);

        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);

        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);

        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       SAMPLE_AGGREGATIONS
        );

        SearchResponse ticketsSearchResponse =
            searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(), searchTicketsQuery,
                                                       OPENSEARCH_TICKET_ENDPOINT_INDEX);

        SearchResponseDto searchResponseDto = SearchResponseDto.fromSearchResponse(ticketsSearchResponse,
                                                                                   SAMPLE_REQUEST_URI);

        var aggregations = searchResponseDto.getAggregations();

        var expected = objectMapperWithEmpty.readValue(inputStreamFromResources(EXPECTED_TICKETS_AGGREGATIONS),
                                                       JsonNode.class);

        assertThat(aggregations, is(Matchers.equalTo(expected)));
    }

    @Test
    void shouldSendQueryWithAllNeededClauseForPublishingRequestTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        var mockSearchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);

        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();
        var restClientWrapper = new RestHighLevelClientWrapper((RestHighLevelClient) null) {
            @Override
            public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions) {
                sentRequestBuffer.set(searchRequest);
                return mockSearchResponse;
            }
        };

        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingPublishingRequest =
            listAllExistingRulesForPublishingRequest(sentRequest);
        var mustIncludePublishingRequestType =
            rulesForIncludingPublishingRequest.stream()
                .anyMatch(rule -> rule.value().equals(PUBLISHING_REQUEST));
        var mustIncludePublishingRequestForDraftPublications =
            rulesForIncludingPublishingRequest.stream().anyMatch(rule -> rule.value().equals(
                DRAFT_PUBLICATION_STATUS));

        assertTrue(mustIncludePublishingRequestType,
                   "Could not find rule for including PublishingRequest");
        assertTrue(mustIncludePublishingRequestForDraftPublications,
                   "Could not find rule for including for Draft Publications");
    }

    @Test
    void shouldSendQueryWithAllExistingRulesForDoiRequestsTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        var mockSearchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);

        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();
        var restClientWrapper = new RestHighLevelClientWrapper((RestHighLevelClient) null) {
            @Override
            public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions) {
                sentRequestBuffer.set(searchRequest);
                return mockSearchResponse;
            }
        };

        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingDoiRequests =
            listAllExistingRulesForDoiRequests(sentRequest);
        var mustExistsTicketStatusInDoiRequests =
            rulesForIncludingDoiRequests.stream().anyMatch(condition -> condition.fieldName().equals("status"));
        var mustExistsOrganizationIdsInDoiRequests =
            rulesForIncludingDoiRequests.stream()
                .anyMatch(condition -> condition.fieldName().equals("organizationIds"));
        assertTrue(mustExistsTicketStatusInDoiRequests, "Could not find rule for including ticket status for "
                                                        + "DoiRequests");
        assertTrue(mustExistsOrganizationIdsInDoiRequests,
                   "Could not find rule for including organizationIds for DoiRequests");
    }

    @Test
    void shouldSendQueryWithAllExistingRulesForPublicationConversationTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        var mockSearchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);

        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();
        var restClientWrapper = new RestHighLevelClientWrapper((RestHighLevelClient) null) {
            @Override
            public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions) {
                sentRequestBuffer.set(searchRequest);
                return mockSearchResponse;
            }
        };

        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        var searchTicketsQuery = new SearchTicketsQuery(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NO,
                                                                       emptyList());
        searchClient.findTicketsForOrganizationIds(generateSampleViewingScope(),
                                                   searchTicketsQuery,
                                                   OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();

        var rulesForIncludingExistingPublicationConversation =
            listAllExistingRulesForPublicationConversation(sentRequest);

        var mustExistsTicketStatusInDoiRequests =
            rulesForIncludingExistingPublicationConversation.stream()
                .anyMatch(condition -> condition.fieldName().equals("status"));
        var mustExistsOrganizationIdsInDoiRequests =
            rulesForIncludingExistingPublicationConversation.stream()
                .anyMatch(condition -> condition.fieldName().equals(
                    "organizationIds"));
        assertTrue(mustExistsTicketStatusInDoiRequests, "Could not find rule for including ticket status for "
                                                        + "GeneralSupportCase");
        assertTrue(mustExistsOrganizationIdsInDoiRequests,
                   "Could not find rule for including OrganizationIds for GeneralSupportCase");
    }

    @NotNull
    private List<MatchQueryBuilder> listAllInclusionAndExclusionRulesForDoiRequests(SearchRequest sentRequest) {
        return listAllDisjunctiveRulesForMatchingDocuments(sentRequest)
            .filter(this::keepOnlyTheDoiRequestRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyMatchTypeRules)
            .map(matches -> (MatchQueryBuilder) matches)
            .collect(Collectors.toList());
    }

    @NotNull
    private List<MatchQueryBuilder> listAllInclusionAndExclusionRulesForPublicationConversation(
        SearchRequest sentRequest) {
        return listAllDisjunctiveRulesForMatchingDocuments(sentRequest)
            .filter(this::keepOnlyThePublicationConversationRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyMatchTypeRules)
            .map(matches -> (MatchQueryBuilder) matches)
            .collect(Collectors.toList());
    }

    private Stream<QueryBuilder> listAllInclusionAndExclusionRulesInQuery(BoolQueryBuilder q) {
        var exclusionRules = q.mustNot();
        var inclusionRules = q.must();
        return Stream.concat(exclusionRules.stream(), inclusionRules.stream());
    }

    private boolean keepOnlyThePublicationConversationRelatedConditions(BoolQueryBuilder q) {
        return
            q.must()
                .stream()
                .filter(this::keepOnlyMatchTypeRules)
                .map(match -> (MatchQueryBuilder) match)
                .anyMatch(match -> match.value().equals(GENERAL_SUPPORT_CASE));
    }

    private Stream<BoolQueryBuilder> listAllDisjunctiveRulesForMatchingDocuments(SearchRequest sentRequest) {
        return booleanQuery(sentRequest.source().query()).should()
            .stream()
            .map(queryClause -> (BoolQueryBuilder) queryClause);
    }

    private boolean keepOnlyMatchTypeRules(QueryBuilder condition) {
        return condition instanceof MatchQueryBuilder;
    }

    private boolean keepOnlyTheDoiRequestRelatedConditions(BoolQueryBuilder q) {
        return
            q.must()
                .stream()
                .filter(this::keepOnlyMatchTypeRules)
                .map(match -> (MatchQueryBuilder) match)
                .anyMatch(match -> match.value().equals(DOI_REQUEST));
    }

    private BoolQueryBuilder booleanQuery(QueryBuilder queryBuilder) {
        return (BoolQueryBuilder) queryBuilder;
    }

    private ViewingScope generateSampleViewingScope() {
        ViewingScope viewingScope = new ViewingScope();
        viewingScope.setIncludedUnits(Set.of(randomUri(), randomUri()));
        viewingScope.setExcludedUnits(Set.of(randomUri()));
        return viewingScope;
    }

    private SearchDocumentsQuery generateSampleQuery() {
        return new SearchDocumentsQuery(SAMPLE_TERM,
                                        SAMPLE_NUMBER_OF_RESULTS,
                                        SAMPLE_FROM,
                                        SAMPLE_ORDERBY,
                                        DESC,
                                        SAMPLE_REQUEST_URI,
                                        SAMPLE_AGGREGATIONS);
    }

    private String generateOpenSearchResponseAsString(String fileName) {
        return streamToString(inputStreamFromResources(fileName));
    }

    private SearchResponse generateMockSearchResponse(String fileName) throws IOException {
        String jsonResponse = generateOpenSearchResponseAsString(fileName);
        return getSearchResponseFromJson(jsonResponse);
    }

    @NotNull
    private List<ExistsQueryBuilder> listAllExistingRulesForDoiRequests(SearchRequest sentRequest) {
        return listAllDisjunctiveRulesForMatchingDocuments(sentRequest)
            .filter(this::keepOnlyTheDoiRequestRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyExistsTypeRules)
            .map(exists -> (ExistsQueryBuilder) exists)
            .collect(Collectors.toList());
    }

    @NotNull
    private List<ExistsQueryBuilder> listAllExistingRulesForPublicationConversation(
        SearchRequest sentRequest) {
        return listAllDisjunctiveRulesForMatchingDocuments(sentRequest)
            .filter(this::keepOnlyThePublicationConversationRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyExistsTypeRules)
            .map(matches -> (ExistsQueryBuilder) matches)
            .collect(Collectors.toList());
    }

    @NotNull
    private List<MatchQueryBuilder> listAllExistingRulesForPublishingRequest(
        SearchRequest sentRequest) {
        return listAllDisjunctiveRulesForMatchingDocuments(sentRequest)
            .filter(this::keepOnlyThePublishingRequestRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyMatchTypeRules)
            .map(matches -> (MatchQueryBuilder) matches)
            .collect(Collectors.toList());
    }

    private boolean keepOnlyExistsTypeRules(QueryBuilder condition) {
        return condition instanceof ExistsQueryBuilder;
    }

    private boolean keepOnlyThePublishingRequestRelatedConditions(BoolQueryBuilder q) {
        return
            q.must()
                .stream()
                .filter(this::keepOnlyMatchTypeRules)
                .map(match -> (MatchQueryBuilder) match)
                .anyMatch(match -> match.value().equals(PUBLISHING_REQUEST));
    }

    RestHighLevelClientWrapper getSearchClientReturningZeroHits(AtomicReference<SearchRequest> sentRequestBuffer)
        throws IOException {
        var mockSearchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        return new RestHighLevelClientWrapper((RestHighLevelClient) null) {
            @Override
            public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions) {
                sentRequestBuffer.set(searchRequest);
                return mockSearchResponse;
            }
        };
    }

    private List<Object> extractQueryBuilderValuesForPublicationConversation(SearchRequest sentRequest) {
        return listAllInclusionAndExclusionRulesForPublicationConversation(sentRequest)
            .stream().map(MatchQueryBuilder::value)
            .collect(Collectors.toList());
    }

    private List<Object> extractQueryBuilderValuesForDoiRequests(SearchRequest sentRequest) {
        return listAllInclusionAndExclusionRulesForDoiRequests(sentRequest)
            .stream().map(MatchQueryBuilder::value)
            .collect(Collectors.toList());
    }
}
