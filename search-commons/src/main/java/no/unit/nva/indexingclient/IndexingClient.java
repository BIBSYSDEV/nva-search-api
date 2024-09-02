package no.unit.nva.indexingclient;

import static no.unit.nva.indexingclient.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.RESOURCES_INDEX;
import static no.unit.nva.indexingclient.models.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.indexingclient.models.AuthenticatedOpenSearchClientWrapper;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.jwt.CognitoAuthenticator;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.secrets.SecretsReader;

import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.common.compress.CompressedXContent;
import org.opensearch.core.common.bytes.BytesReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IndexingClient extends AuthenticatedOpenSearchClientWrapper {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    public static final String INITIAL_LOG_MESSAGE = "Adding document [{}] to -> {}";
    public static final String DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_SEARCH_INFRASTRUCTURE =
            "Document with id={} was not found in search infrastructure";
    public static final int BULK_SIZE = 100;
    public static final boolean SEQUENTIAL = false;
    private static final Logger logger = LoggerFactory.getLogger(IndexingClient.class);

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

    public Void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
        logger.debug(
                INITIAL_LOG_MESSAGE,
                indexDocument.getDocumentIdentifier(),
                indexDocument.getIndexName());
        openSearchClient.index(indexDocument.toIndexRequest(), getRequestOptions());
        return null;
    }

    /**
     * Removes a document from Opensearch index.
     *
     * @param identifier og document
     */
    public void removeDocumentFromResourcesIndex(String identifier) throws IOException {
        DeleteResponse deleteResponse =
                openSearchClient.delete(
                        new DeleteRequest(RESOURCES_INDEX, identifier), getRequestOptions());
        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            logger.warn(DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_SEARCH_INFRASTRUCTURE, identifier);
        }
    }

    public void removeDocumentFromImportCandidateIndex(String identifier) throws IOException {
        DeleteResponse deleteResponse =
                openSearchClient.delete(
                        new DeleteRequest(IMPORT_CANDIDATES_INDEX, identifier),
                        getRequestOptions());
        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            logger.warn(DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_SEARCH_INFRASTRUCTURE, identifier);
        }
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

    private Stream<List<IndexDocument>> splitStreamToBatches(Stream<IndexDocument> indexDocuments) {
        UnmodifiableIterator<List<IndexDocument>> bulks =
                Iterators.partition(indexDocuments.iterator(), BULK_SIZE);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(bulks, Spliterator.ORDERED), SEQUENTIAL);
    }

    private BulkResponse insertBatch(List<IndexDocument> bulk) throws IOException {
        List<IndexRequest> indexRequests =
                bulk.stream().parallel().map(IndexDocument::toIndexRequest).toList();

        BulkRequest request = new BulkRequest();
        indexRequests.forEach(request::add);
        request.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
        request.waitForActiveShards(ActiveShardCount.ONE);
        return openSearchClient.bulk(request, getRequestOptions());
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
