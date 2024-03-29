package no.unit.nva.search;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static no.unit.nva.indexing.testutils.Constants.TEST_TOKEN;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.DRAFT_PUBLICATION_STATUS;
import static no.unit.nva.search.SearchClient.GENERAL_SUPPORT_CASE;
import static no.unit.nva.search.SearchClient.PUBLISHING_REQUEST;
import static no.unit.nva.search.SearchClient.prepareWithSecretReader;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_TICKET_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search.models.CuratorSearchType.DOI;
import static no.unit.nva.search.models.CuratorSearchType.PUBLISHING;
import static no.unit.nva.search.models.CuratorSearchType.SUPPORT;
import static no.unit.nva.search.models.SearchTicketsQuery.VIEWING_SCOPE_QUERY_NAME;
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
import no.unit.nva.search.models.CuratorSearchType;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.SearchTicketsQuery;
import no.unit.nva.search.models.UsernamePasswordWrapper;
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
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

class SearchClientTest {

    public static final String SAMPLE_TERM = "SampleSearchTerm";
    public static final String EXPECTED_TICKETS_AGGREGATIONS =
        "ticket_searchresponsedto_aggregations_response.json";
    private static final int SAMPLE_NUMBER_OF_RESULTS = 7;
    private static final String NO_HITS_RESPONSE_JSON = "opensearch_no_hits_response.json";
    private static final int SAMPLE_FROM = 0;
    private static final String SAMPLE_ORDERBY = "orderByField";
    private static final String OPENSEARCH_SAMPLE_TICKET_RESPONSE_FILE = "ticket_response.json";
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    private static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> SAMPLE_AGGREGATIONS =
        List.of(new TermsAggregationBuilder(randomString()).field(randomString()));
    private static final int OPENSEARCH_ACTUAL_SAMPLE_NUMBER_OF_RESULTS = 2;
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

        searchClient.searchWithSearchTicketQuery(generateTicketQueryForSearchType(Set.of(DOI)),
                                                 OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingDoiRequest = extractQueryBuilderValuesForDoiRequests(sentRequest);
        assertThat(rulesForIncludingDoiRequest, hasItem(DRAFT_PUBLICATION_STATUS));
        assertThat(rulesForIncludingDoiRequest, hasItem(DOI_REQUEST));
    }

    @Test
    void shouldSendQueryWithAllNeededRulesForPublicationConversationTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);

        searchClient.searchWithSearchTicketQuery(generateTicketQueryForSearchType(Set.of(SUPPORT)),
                                                 OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingPublicationConversation = extractQueryBuilderValuesForPublicationConversation(sentRequest);
        assertThat(rulesForIncludingPublicationConversation, hasItem(GENERAL_SUPPORT_CASE));
    }

    @Test
    void shouldSendQueryWithAllNeededRulesForPublishingRequestTypeWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);

        searchClient.searchWithSearchTicketQuery(generateTicketQueryForSearchType(Set.of(PUBLISHING)),
                                                 OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingPublicationRequest = extractQueryBuilderValuesForPublicationRequest(sentRequest);
        assertThat(rulesForIncludingPublicationRequest, hasItem(PUBLISHING_REQUEST));
    }

    @Test
    void searchSingleTermReturnsResponse() throws ApiGatewayException, IOException {
        var restHighLevelClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient =
            new SearchClient(new RestHighLevelClientWrapper(restHighLevelClient), cachedJwtProvider);
        var searchResponseDto =
            searchClient.searchWithSearchDocumentQuery(generateSampleQuery(), OPENSEARCH_ENDPOINT_INDEX);
        assertNotNull(searchResponseDto);
    }

    @Test
    void shouldReturnTicketSearchResponse() throws ApiGatewayException, IOException {
        var restHighLevelClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient =
            new SearchClient(new RestHighLevelClientWrapper(restHighLevelClient), cachedJwtProvider);

        var response =
            searchClient.searchWithSearchTicketQuery(generateSampleTicketQuery(),
                                                     OPENSEARCH_TICKET_ENDPOINT_INDEX);
        assertNotNull(response);
    }

    @Test
    void shouldReturnOwnerTicketSearchResponse() throws ApiGatewayException, IOException {
        var restHighLevelClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient =
            new SearchClient(new RestHighLevelClientWrapper(restHighLevelClient), cachedJwtProvider);

        var response =
            searchClient.searchOwnerTickets(generateSampleTicketQuery(),
                                            "",
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
        var searchTicketsQuery = new SearchTicketsQuery(SAMPLE_TERM, resultSize, SAMPLE_FROM, SAMPLE_ORDERBY, DESC,
                                                        SAMPLE_REQUEST_URI,
                                                        emptyList(),
                                                        generateSampleViewingScope(),
                                                        Set.of(),
                                                        false);

        searchClient.searchWithSearchTicketQuery(searchTicketsQuery,
                                                 OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var actualRequestedSize = sentRequest.source().size();
        assertThat(actualRequestedSize, is(equalTo(resultSize)));
    }

    @Test
    void shouldSendTicketsRequestWithSuppliedPageNumberWhenSearchingForTickets()
        throws ApiGatewayException, IOException {
        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();

        var restClientWrapper = getSearchClientReturningZeroHits(sentRequestBuffer);
        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        int resultsFrom = randomInteger(100);
        var searchTicketsQuery = new SearchTicketsQuery(SAMPLE_TERM, SAMPLE_NUMBER_OF_RESULTS, resultsFrom,
                                                        SAMPLE_ORDERBY,
                                                        DESC, SAMPLE_REQUEST_URI, emptyList(),
                                                        generateSampleViewingScope(),
                                                        Set.of(),false);
        searchClient.searchWithSearchTicketQuery(searchTicketsQuery,
                                                 OPENSEARCH_TICKET_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var actualResultsFrom = sentRequest.source().from();
        assertThat(actualResultsFrom, is(equalTo(resultsFrom)));
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
    void searchOwnerReturnsErrorResponseWhenExceptionInResourcesDoSearch() throws IOException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.search(any(), any())).thenThrow(new IOException());
        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);
        assertThrows(BadGatewayException.class,
                     () -> searchClient.searchOwnerTickets(generateSampleTicketQuery(), "",
                                                           OPENSEARCH_ENDPOINT_INDEX));
    }

    @Test
    void searchTicketsResponseShouldFormatAggregationsCorrectly() throws IOException, ApiGatewayException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        var openSearchResponseJson = generateOpenSearchResponseAsString(OPENSEARCH_SAMPLE_TICKET_RESPONSE_FILE);
        var searchResponse = getSearchResponseFromJson(openSearchResponseJson);

        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);

        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);

        var searchResponseDto =
            searchClient.searchWithSearchTicketQuery(generateSampleTicketQuery(),
                                                     OPENSEARCH_TICKET_ENDPOINT_INDEX);

        var aggregations = searchResponseDto.getAggregations();

        var expected = objectMapperWithEmpty.readValue(inputStreamFromResources(EXPECTED_TICKETS_AGGREGATIONS),
                                                       JsonNode.class);

        assertThat(aggregations, is(Matchers.equalTo(expected)));
    }

    @Test
    void searchSingleTermReturnsTicketsResponseWithStatsFromOpensearch() throws ApiGatewayException,
                                                                                IOException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        var openSearchResponseJson = generateOpenSearchResponseAsString(OPENSEARCH_SAMPLE_TICKET_RESPONSE_FILE);
        var searchResponse = getSearchResponseFromJson(openSearchResponseJson);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);

        SearchResponseDto searchResponseDto =
            searchClient.searchWithSearchTicketQuery(generateSampleTicketQuery(),
                                                     OPENSEARCH_TICKET_ENDPOINT_INDEX);

        assertNotNull(searchResponseDto);
        assertEquals(searchResponseDto.getHits().size(), OPENSEARCH_ACTUAL_SAMPLE_NUMBER_OF_RESULTS);
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

    @NotNull
    private List<MatchQueryBuilder> listAllInclusionAndExclusionRulesForDoiRequests(SearchRequest sentRequest) {
        return listNonViewingScopePartOfRequest(sentRequest)
            .filter(this::keepOnlyTheDoiRequestRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyMatchTypeRules)
            .map(matches -> (MatchQueryBuilder) matches)
            .collect(Collectors.toList());
    }

    @NotNull
    private List<MatchQueryBuilder> listAllInclusionAndExclusionRulesForPublicationConversation(
        SearchRequest sentRequest) {
        return listNonViewingScopePartOfRequest(sentRequest)
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

    @NotNull
    private List<MatchQueryBuilder> listAllInclusionAndExclusionRulesForPublicationRequest(
        SearchRequest sentRequest) {
        return listNonViewingScopePartOfRequest(sentRequest)
            .filter(this::keepOnlyThePublicationRequestRelatedConditions)
            .flatMap(this::listAllInclusionAndExclusionRulesInQuery)
            .filter(this::keepOnlyMatchTypeRules)
            .map(matches -> (MatchQueryBuilder) matches)
            .collect(Collectors.toList());
    }

    private boolean keepOnlyThePublicationConversationRelatedConditions(BoolQueryBuilder q) {
        return
            q.must()
                .stream()
                .filter(this::keepOnlyMatchTypeRules)
                .map(match -> (MatchQueryBuilder) match)
                .anyMatch(match -> match.value().equals(GENERAL_SUPPORT_CASE));
    }

    private Stream<BoolQueryBuilder> listNonViewingScopePartOfRequest(SearchRequest sentRequest) {
        return booleanQuery(sentRequest.source().query()).must()
                   .stream()
                   .filter(queryClause -> isNull(queryClause.queryName())
                                          || !queryClause.queryName().equals(VIEWING_SCOPE_QUERY_NAME))
                   .map(queryClause -> (BoolQueryBuilder) queryClause)
            .map(BoolQueryBuilder::should)
                   .flatMap(List::stream)
                   .map(q -> (BoolQueryBuilder) q);
    }

    private boolean keepOnlyMatchTypeRules(QueryBuilder condition) {
        return condition instanceof MatchQueryBuilder;
    }

    private boolean keepOnlyTheDoiRequestRelatedConditions(BoolQueryBuilder q) {
        return q.must()
            .stream()
            .filter(this::keepOnlyMatchTypeRules)
            .map(match -> (MatchQueryBuilder) match)
            .anyMatch(match -> match.value().equals(DOI_REQUEST));
    }

    private boolean keepOnlyThePublicationRequestRelatedConditions(BoolQueryBuilder q) {
        return
            q.must()
                .stream()
                .filter(this::keepOnlyMatchTypeRules)
                .map(match -> (MatchQueryBuilder) match)
                .anyMatch(match -> match.value().equals(PUBLISHING_REQUEST));
    }

    private BoolQueryBuilder booleanQuery(QueryBuilder queryBuilder) {
        return (BoolQueryBuilder) queryBuilder;
    }

    private List<URI> generateSampleViewingScope() {
        return List.of(randomUri(), randomUri());
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

    private SearchTicketsQuery generateSampleTicketQuery() {
        return new SearchTicketsQuery(SAMPLE_TERM,
                                      SAMPLE_NUMBER_OF_RESULTS,
                                      SAMPLE_FROM,
                                      SAMPLE_ORDERBY,
                                      DESC,
                                      SAMPLE_REQUEST_URI,
                                      SAMPLE_AGGREGATIONS,
                                      generateSampleViewingScope(),
                                      Set.of(),
                                      false);
    }

    private SearchTicketsQuery generateTicketQueryForSearchType(Set<CuratorSearchType> searchTypes) {
        return new SearchTicketsQuery(SAMPLE_TERM,
                                      SAMPLE_NUMBER_OF_RESULTS,
                                      SAMPLE_FROM,
                                      SAMPLE_ORDERBY,
                                      DESC,
                                      SAMPLE_REQUEST_URI,
                                      SAMPLE_AGGREGATIONS,
                                      generateSampleViewingScope(),
                                      searchTypes,
                                      false);
    }

    private String generateOpenSearchResponseAsString(String fileName) {
        return streamToString(inputStreamFromResources(fileName));
    }

    private SearchResponse generateMockSearchResponse(String fileName) throws IOException {
        var jsonResponse = generateOpenSearchResponseAsString(fileName);
        return getSearchResponseFromJson(jsonResponse);
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

    private List<Object> extractQueryBuilderValuesForPublicationRequest(SearchRequest sentRequest) {
        return listAllInclusionAndExclusionRulesForPublicationRequest(sentRequest)
            .stream().map(MatchQueryBuilder::value)
            .collect(Collectors.toList());
    }
}
