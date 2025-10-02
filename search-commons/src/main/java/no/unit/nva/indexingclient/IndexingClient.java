package no.unit.nva.indexingclient;

import static no.unit.nva.indexingclient.models.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.indexingclient.models.AuthenticatedOpenSearchClientWrapper;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.jwt.CognitoAuthenticator;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.common.compress.CompressedXContent;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexingClient extends AuthenticatedOpenSearchClientWrapper {

  public static final int BULK_SIZE = 100;
  private static final Logger logger = LoggerFactory.getLogger(IndexingClient.class);

  private static final String INITIAL_LOG_MESSAGE = "Adding document [{}] to -> {}";
  private static final String DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_SEARCH_INFRASTRUCTURE =
      "Document with id={} was not found in search infrastructure";
  private static final boolean SEQUENTIAL = false;

  /**
   * Creates a new OpenSearchRestClient.
   *
   * @param openSearchClient client to use for access to search infrastructure
   */
  public IndexingClient(
      RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwtProvider) {
    super(openSearchClient, cachedJwtProvider);
  }

  @JacocoGenerated
  public static IndexingClient defaultIndexingClient() {
    return prepareWithSecretReader(new SecretsReader());
  }

  public static IndexingClient prepareWithSecretReader(SecretsReader secretReader) {
    var cognitoCredentials = createCognitoCredentials(secretReader);
    var cognitoAuthenticator =
        CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
    var cachedJwtProvider = CachedJwtProvider.prepareWithAuthenticator(cognitoAuthenticator);
    return new IndexingClient(defaultRestHighLevelClientWrapper(), cachedJwtProvider);
  }

  public void refreshIndex(String indexName) throws IOException {
    var refreshRequest = new RefreshRequest(indexName);
    openSearchClient.indices().refresh(refreshRequest, getRequestOptions());
  }

  public Void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
    var documentIdentifier = indexDocument.getDocumentIdentifier();
    var indexName = indexDocument.getIndexName();
    logger.debug(INITIAL_LOG_MESSAGE, documentIdentifier, indexName);
    removeDocumentFromIndex(documentIdentifier, indexName);
    openSearchClient.index(indexDocument.toIndexRequest(), getRequestOptions());
    return null;
  }

  /**
   * Removes a document from Opensearch index.
   *
   * @param identifier the identifier of the document
   * @param index
   */
  public void removeDocumentFromIndex(String identifier, String index) throws IOException {
    var request = new DeleteByQueryRequest(index);
    request.setQuery(QueryBuilders.idsQuery().addIds(identifier));
    request.setRefresh(true);
    var response = openSearchClient.deleteByQuery(request, getRequestOptions());
    logWarningIfNotFound(identifier, response);
  }

  public Void createIndex(String indexName) throws IOException {
    var createRequest = new CreateIndexRequest(indexName);
    openSearchClient.indices().create(createRequest, getRequestOptions());
    return null;
  }

  public Void createIndex(String indexName, Map<String, ?> mappings) throws IOException {
    var createRequest = new CreateIndexRequest(indexName);
    createRequest.mapping(mappings);
    openSearchClient.indices().create(createRequest, getRequestOptions());
    return null;
  }

  public Void createIndex(String indexName, Map<String, ?> mappings, Map<String, ?> settings)
      throws IOException {
    var createRequest = new CreateIndexRequest(indexName);
    createRequest.mapping(mappings);
    createRequest.settings(settings);
    openSearchClient.indices().create(createRequest, getRequestOptions());
    return null;
  }

  public Stream<BulkResponse> batchInsert(Stream<IndexDocument> contents) {
    var batches = splitStreamToBatches(contents);
    return batches.map(attempt(this::insertBatch)).map(Try::orElseThrow);
  }

  private Stream<List<IndexDocument>> splitStreamToBatches(Stream<IndexDocument> indexDocuments) {
    UnmodifiableIterator<List<IndexDocument>> bulks =
        Iterators.partition(indexDocuments.iterator(), BULK_SIZE);
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(bulks, Spliterator.ORDERED), SEQUENTIAL);
  }

  private BulkResponse insertBatch(List<IndexDocument> bulk) throws IOException {
    var indexRequests = bulk.stream().parallel().map(IndexDocument::toIndexRequest).toList();

    var request = new BulkRequest();
    indexRequests.forEach(request::add);
    request.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
    request.waitForActiveShards(ActiveShardCount.ONE);
    return openSearchClient.bulk(request, getRequestOptions());
  }

  public Void deleteIndex(String indexName) throws IOException {
    openSearchClient.indices().delete(new DeleteIndexRequest(indexName), getRequestOptions());
    return null;
  }

  public JsonNode getMapping(String indexName) {
    return attempt(() -> getMappingMetadata(indexName))
        .map(MappingMetadata::source)
        .map(CompressedXContent::uncompressed)
        .map(BytesReference::utf8ToString)
        .map(JsonUtils.dtoObjectMapper::readTree)
        .orElseThrow();
  }

  private void logWarningIfNotFound(String identifier, BulkByScrollResponse deleteResponse) {
    if (Optional.ofNullable(deleteResponse)
        .map(BulkByScrollResponse::getDeleted)
        .map(value -> value == 0)
        .orElse(false)) {
      logger.warn(DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_SEARCH_INFRASTRUCTURE, identifier);
    } else {
      logger.info("Successfully deleted document with id: {}", identifier);
    }
  }

  private MappingMetadata getMappingMetadata(String indexName) throws IOException {
    var request = new GetMappingsRequest().indices(indexName);
    return openSearchClient
        .indices()
        .getIndicesClient()
        .getMapping(request, getRequestOptions())
        .mappings()
        .get(indexName);
  }
}
