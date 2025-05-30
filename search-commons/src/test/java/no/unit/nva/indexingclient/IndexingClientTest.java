package no.unit.nva.indexingclient;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.indexingclient.IndexingClient.BULK_SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.IndicesClientWrapper;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.UsernamePasswordWrapper;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.IndicesClient;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.indices.CreateIndexRequest;

class IndexingClientTest {

  public static final int
      SET_OF_RESOURCES_THAT_DO_NOT_FIT_EXACTLY_IN_THE_BULK_SIZE_OF_A_BULK_REQUEST = 1256;
  public static final IndexResponse UNUSED_INDEX_RESPONSE = null;
  private RestHighLevelClientWrapper esClient;
  private IndexingClient indexingClient;
  private AtomicReference<IndexRequest> submittedIndexRequest;

  private CachedJwtProvider cachedJwtProvider;

  @BeforeEach
  public void init() throws IOException {
    cachedJwtProvider = setupMockedCachedJwtProvider();
    esClient = setupMockEsClient();
    indexingClient = new IndexingClient(esClient, cachedJwtProvider);
    submittedIndexRequest = new AtomicReference<>();
  }

  @Test
  void constructorWithSecretsReaderDefinedShouldCreateInstance() {
    var secretsReaderMock = mock(SecretsReader.class);
    var testCredentials = new UsernamePasswordWrapper("user", "password");
    when(secretsReaderMock.fetchClassSecret(anyString(), eq(UsernamePasswordWrapper.class)))
        .thenReturn(testCredentials);

    IndexingClient indexingClient = IndexingClient.prepareWithSecretReader(secretsReaderMock);
    assertNotNull(indexingClient);
  }

  @Test
  void shouldIndexAllDocumentsInBatchInBulksOfSpecifiedSize() throws IOException {
    var indexDocuments =
        IntStream.range(
                0, SET_OF_RESOURCES_THAT_DO_NOT_FIT_EXACTLY_IN_THE_BULK_SIZE_OF_A_BULK_REQUEST)
            .boxed()
            .map(i -> randomJson())
            .map(this::toIndexDocument)
            .toList();
    List<BulkResponse> provokeExecution =
        indexingClient.batchInsert(indexDocuments.stream()).collect(Collectors.toList());
    assertThat(provokeExecution, is(not(nullValue())));

    int expectedNumberOfBulkRequests =
        (int) Math.ceil(((double) indexDocuments.size()) / ((double) BULK_SIZE));
    verify(esClient, times(expectedNumberOfBulkRequests))
        .bulk(any(BulkRequest.class), any(RequestOptions.class));
  }

  @Test
  void shouldSendIndexRequestWithIndexNameSpecifiedByIndexDocument() throws IOException {
    var indexDocument = sampleIndexDocument();
    var expectedIndex = indexDocument.consumptionAttributes().index();
    indexingClient.addDocumentToIndex(indexDocument);

    assertThat(submittedIndexRequest.get().index(), is(equalTo(expectedIndex)));
    assertThat(extractDocumentFromSubmittedIndexRequest(), is(equalTo(indexDocument.resource())));
  }

  @Test
  void shouldCallEsClientCreateIndexRequest() throws IOException {
    IndicesClient indicesClient = mock(IndicesClient.class);
    var indicesClientWrapper = new IndicesClientWrapper(indicesClient);
    when(esClient.indices()).thenReturn(indicesClientWrapper);
    indexingClient.createIndex(randomString());
    var expectedNumberOfCreateInvocationsToEs = 1;
    verify(indicesClient, times(expectedNumberOfCreateInvocationsToEs))
        .create(any(CreateIndexRequest.class), any(RequestOptions.class));
  }

  @Test
  void shouldThrowExceptionWhenEsClientFailedToCreateIndex() throws IOException {
    var expectedErrorMessage = randomString();
    var indicesClientWrapper = createMockIndicesClientWrapper();
    when(esClient.indices()).thenReturn(indicesClientWrapper);
    when(indicesClientWrapper.create(any(CreateIndexRequest.class), any(RequestOptions.class)))
        .thenThrow(new IOException(expectedErrorMessage));
    Executable createIndexAction = () -> indexingClient.createIndex(randomString());
    var actualException = assertThrows(IOException.class, createIndexAction);
    assertThat(actualException.getMessage(), containsString(expectedErrorMessage));
  }

  @Test
  void shouldThrowExceptionContainingTheCauseWhenIndexDocumentFailsToBeIndexed()
      throws IOException {
    String expectedMessage = randomString();
    esClient = mock(RestHighLevelClientWrapper.class);
    when(esClient.index(any(IndexRequest.class), any(RequestOptions.class)))
        .thenThrow(new IOException(expectedMessage));

    indexingClient = new IndexingClient(esClient, cachedJwtProvider);

    Executable indexingAction = () -> indexingClient.addDocumentToIndex(sampleIndexDocument());
    var exception = assertThrows(IOException.class, indexingAction);
    assertThat(exception.getMessage(), containsString(expectedMessage));
  }

  @Test
  void shouldNotThrowExceptionWhenTryingToDeleteNonExistingDocument() throws IOException {
    RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
    DeleteResponse nothingFoundResponse = mock(DeleteResponse.class);
    when(nothingFoundResponse.getResult()).thenReturn(DocWriteResponse.Result.NOT_FOUND);
    when(restHighLevelClient.delete(any(), any())).thenReturn(nothingFoundResponse);
    IndexingClient indexingClient = new IndexingClient(restHighLevelClient, cachedJwtProvider);
    assertDoesNotThrow(() -> indexingClient.removeDocumentFromResourcesIndex("1234"));
  }

  @Test
  void shouldNotThrowExceptionWhenTryingToDeleteNonExistingDocumentFromImportCandidateIndex()
      throws IOException {
    RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
    DeleteResponse nothingFoundResponse = mock(DeleteResponse.class);
    when(nothingFoundResponse.getResult()).thenReturn(DocWriteResponse.Result.NOT_FOUND);
    when(restHighLevelClient.delete(any(), any())).thenReturn(nothingFoundResponse);
    IndexingClient indexingClient = new IndexingClient(restHighLevelClient, cachedJwtProvider);
    assertDoesNotThrow(() -> indexingClient.removeDocumentFromImportCandidateIndex("1234"));
  }

  @Test
  void shouldCallEsClientDeleteIndexRequest() throws IOException {
    var indicesClient = mock(IndicesClient.class);
    var indicesClientWrapper = new IndicesClientWrapper(indicesClient);
    when(esClient.indices()).thenReturn(indicesClientWrapper);
    indexingClient.deleteIndex(randomString());
    var expectedNumberOfCreateInvocationsToEs = 1;
    verify(indicesClient, times(expectedNumberOfCreateInvocationsToEs))
        .delete(any(DeleteIndexRequest.class), any(RequestOptions.class));
  }

  @Test
  void shouldCallEsClientRefreshIndexRequest() throws IOException {
    var indicesClient = mock(IndicesClient.class);
    var indicesClientWrapper = new IndicesClientWrapper(indicesClient);
    when(esClient.indices()).thenReturn(indicesClientWrapper);
    indexingClient.refreshIndex(randomString());
    var expectedNumberOfRefreshInvocationsToEs = 1;
    verify(indicesClient, times(expectedNumberOfRefreshInvocationsToEs))
        .refresh(any(RefreshRequest.class), any(RequestOptions.class));
  }

  private IndicesClientWrapper createMockIndicesClientWrapper() {
    IndicesClient indicesClient = mock(IndicesClient.class);
    return new IndicesClientWrapper(indicesClient);
  }

  private RestHighLevelClientWrapper setupMockEsClient() throws IOException {
    var esClient = mock(RestHighLevelClientWrapper.class);
    when(esClient.index(any(IndexRequest.class), any(RequestOptions.class)))
        .thenAnswer(
            invocation -> {
              var indexRequest = (IndexRequest) invocation.getArgument(0);
              submittedIndexRequest.set(indexRequest);
              return UNUSED_INDEX_RESPONSE;
            });
    return esClient;
  }

  private IndexDocument sampleIndexDocument() {
    EventConsumptionAttributes consumptionAttributes =
        new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
    return new IndexDocument(consumptionAttributes, sampleJsonObject());
  }

  private JsonNode sampleJsonObject() {
    return attempt(() -> dtoObjectMapper.readTree(randomJson())).orElseThrow();
  }

  private IndexDocument toIndexDocument(String jsonString) {
    var consumptionAttributes =
        new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
    var json = attempt(() -> objectMapperWithEmpty.readTree(jsonString)).orElseThrow();
    return new IndexDocument(consumptionAttributes, json);
  }

  private JsonNode extractDocumentFromSubmittedIndexRequest() throws JsonProcessingException {
    return objectMapperWithEmpty.readTree(
        submittedIndexRequest.get().source().toBytesRef().utf8ToString());
  }
}
