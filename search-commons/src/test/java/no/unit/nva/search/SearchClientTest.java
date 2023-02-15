package no.unit.nva.search;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.indexing.testutils.TestConstants.TEST_TOKEN;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.SearchClient.DOI_REQUEST;
import static no.unit.nva.search.SearchClient.DRAFT_PUBLICATION_STATUS;
import static no.unit.nva.search.SearchClient.GENERAL_SUPPORT_CASE;
import static no.unit.nva.search.SearchClient.PENDING;
import static no.unit.nva.search.SearchClient.prepareWithSecretReader;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.indexing.testutils.SearchResponseUtil;
import no.unit.nva.search.models.AggregationDto;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
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
    private static final int OPENSEARCH_ACTUAL_SAMPLE_NUMBER_OF_RESULTS = 2;
    private static final URI SAMPLE_REQUEST_URI = randomUri();
    private static final List<AggregationDto> SAMPLE_AGGREGATIONS = List.of(
        new AggregationDto(randomString(), randomString()));
    public static final String EXPECTED_AGGREGATIONS = "sample_opensearch_response_searchresponsedto_aggregations.json";

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
    void shouldSendQueryWithAllNeededRulesForDoiRequestsTypeWhenSearchingForResources()
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
        searchClient.findResourcesForOrganizationIds(generateSampleViewingScope(),
                                                     DEFAULT_PAGE_SIZE,
                                                     DEFAULT_PAGE_NO,
                                                     OPENSEARCH_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingDoiRequests =
            listAllInclusionAndExclusionRulesForDoiRequests(sentRequest);
        var mustIncludeOnlyPendingDoiRequests =
            rulesForIncludingDoiRequests.stream().anyMatch(condition -> condition.value().equals(PENDING));
        var mustExcludeDoiRequestsForDraftPublications =
            rulesForIncludingDoiRequests.stream().anyMatch(condition -> condition.value().equals(
                DRAFT_PUBLICATION_STATUS));
        var mustIncludeDoiRequestsType =
            rulesForIncludingDoiRequests.stream().anyMatch(condition -> condition.value().equals(DOI_REQUEST));

        assertTrue(mustIncludeOnlyPendingDoiRequests, "Could not find rule for including Pending DoiRequests");
        assertTrue(mustExcludeDoiRequestsForDraftPublications,
                   "Could not find rule for excluding DoiRequests for Draft Publications");
        assertTrue(mustIncludeDoiRequestsType, "Could not find rule for including DoiRequest");
    }

    @Test
    void shouldSendQueryWithAllNeededClauseForPublicationConversationTypeWhenSearchingForResources()
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
        searchClient.findResourcesForOrganizationIds(generateSampleViewingScope(),
                                                     DEFAULT_PAGE_SIZE,
                                                     DEFAULT_PAGE_NO,
                                                     OPENSEARCH_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var rulesForIncludingPublicationConversation =
            listAllInclusionAndExclusionRulesForPublicationConversation(sentRequest);
        var mustIncludePublicationConversationType =
            rulesForIncludingPublicationConversation.stream()
                .anyMatch(rule -> rule.value().equals(GENERAL_SUPPORT_CASE));
        assertTrue(mustIncludePublicationConversationType,
                   "Could not find rule for including PublicationConversation");
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
    void shouldReturnSearchResponseWhenSearchingWithOrganizationIds() throws ApiGatewayException, IOException {
        RestHighLevelClient restHighLevelClient = mock(RestHighLevelClient.class);
        var searchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        var searchClient =
            new SearchClient(new RestHighLevelClientWrapper(restHighLevelClient), cachedJwtProvider);
        var response =
            searchClient.findResourcesForOrganizationIds(generateSampleViewingScope(),
                                                         DEFAULT_PAGE_SIZE,
                                                         DEFAULT_PAGE_NO,
                                                         OPENSEARCH_ENDPOINT_INDEX);
        assertNotNull(response);
    }

    @Test
    void shouldSendRequestWithSuppliedPageSizeWhenSearchingForResources() throws ApiGatewayException, IOException {
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
        int resultSize = 1 + randomInteger(1000);
        searchClient.findResourcesForOrganizationIds(generateSampleViewingScope(),
                                                     resultSize,
                                                     DEFAULT_PAGE_NO,
                                                     OPENSEARCH_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var actualRequestedSize = sentRequest.source().size();
        assertThat(actualRequestedSize, is(equalTo(resultSize)));
    }

    @Test
    void shouldSendRequestWithFirstEntryIndexCalculatedBySuppliedPageSizeAndPageNumber()
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
        int pageNo = randomInteger(100);
        searchClient.findResourcesForOrganizationIds(generateSampleViewingScope(),
                                                     DEFAULT_PAGE_SIZE,
                                                     pageNo,
                                                     OPENSEARCH_ENDPOINT_INDEX);
        var sentRequest = sentRequestBuffer.get();
        var actualResultsFrom = sentRequest.source().from();
        var resultsFrom = pageNo * DEFAULT_PAGE_SIZE;
        assertThat(actualResultsFrom, is(equalTo(resultsFrom)));
    }

    @Test
    void shouldSendRequestWithAggregations() throws ApiGatewayException, IOException {
        var mockSearchResponse = generateMockSearchResponse(NO_HITS_RESPONSE_JSON);

        AtomicReference<SearchRequest> sentRequestBuffer = new AtomicReference<>();
        var restClientWrapper = new RestHighLevelClientWrapper((RestHighLevelClient) null) {
            @Override
            public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions) {
                sentRequestBuffer.set(searchRequest);
                return mockSearchResponse;
            }
        };

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

        var searchClient = new SearchClient(restClientWrapper, cachedJwtProvider);
        searchClient.searchWithSearchDocumentQuery(sampleQuery, OPENSEARCH_ENDPOINT_INDEX);

        var sentRequest = sentRequestBuffer.get();
        var actualAggregation = objectMapper.readTree(sentRequest.source().aggregations().toString());

        nestedAggregationDTOs.forEach(aggDTO -> assertAggregationHasField(actualAggregation, aggDTO));
    }

    private void assertAggregationHasField(JsonNode json, AggregationDto aggDto) {
        var actualField = json.at("/" + aggDto.term + "/terms/field").asText();
        assertThat(actualField, is(equalTo(aggDto.field)));

        if (aggDto.subAggregation != null) {
            assertAggregationHasField(json.at("/" + aggDto.term + "/aggregations"), aggDto.subAggregation);
        }
    }

    @Test
    void searchSingleTermReturnsResponseWithStatsFromOpensearch() throws ApiGatewayException, IOException {
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
    void searchSingleTermReturnsErrorResponseWhenExceptionInDoSearch() throws IOException {
        var restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.search(any(), any())).thenThrow(new IOException());
        var searchClient = new SearchClient(restHighLevelClient, cachedJwtProvider);
        assertThrows(BadGatewayException.class,
                     () -> searchClient.searchWithSearchDocumentQuery(generateSampleQuery(),
                                                                      OPENSEARCH_ENDPOINT_INDEX));
    }

    @Test
    void searchResponseShouldFormatAggregationsCorrectly() throws IOException, ApiGatewayException {
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
}
