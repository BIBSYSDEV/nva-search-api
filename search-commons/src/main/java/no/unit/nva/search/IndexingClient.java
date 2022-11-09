package no.unit.nva.search;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import no.unit.nva.search.models.IndexDocument;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.WriteRequest.RefreshPolicy;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.indices.CreateIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static no.unit.nva.search.constants.ApplicationConstants.ELASTICSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_API_URI;
import static nva.commons.core.attempt.Try.attempt;

public class IndexingClient {

    public static final String INITIAL_LOG_MESSAGE = "using Elasticsearch endpoint {} and index {}";
    public static final String DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_ELASTICSEARCH
        = "Document with id={} was not found in elasticsearch";
    public static final int BULK_SIZE = 100;
    public static final boolean SEQUENTIAL = false;
    private static final Logger logger = LoggerFactory.getLogger(IndexingClient.class);
    private final RestHighLevelClientWrapper elasticSearchClient;

    @JacocoGenerated
    public IndexingClient() {
        elasticSearchClient = RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper();
    }

    /**
     * Creates a new ElasticSearchRestClient.
     *
     * @param elasticSearchClient client to use for access to ElasticSearch
     */
    public IndexingClient(RestHighLevelClientWrapper elasticSearchClient) {

        this.elasticSearchClient = elasticSearchClient;
    }

    public Void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
        logger.info(INITIAL_LOG_MESSAGE, SEARCH_INFRASTRUCTURE_API_URI, indexDocument.getIndexName());
        elasticSearchClient.index(indexDocument.toIndexRequest(), RequestOptions.DEFAULT);
        return null;
    }

    /**
     * Removes a document from Elasticsearch index.
     *
     * @param identifier og document
     */
    public void removeDocumentFromIndex(String identifier) throws IOException {
        DeleteResponse deleteResponse = elasticSearchClient
            .delete(new DeleteRequest(ELASTICSEARCH_ENDPOINT_INDEX, identifier),
                    RequestOptions.DEFAULT);
        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            logger.warn(DOCUMENT_WITH_ID_WAS_NOT_FOUND_IN_ELASTICSEARCH, identifier);
        }
    }

    public Void createIndex(String indexName) throws IOException {
        elasticSearchClient.indices().create(new CreateIndexRequest(indexName), RequestOptions.DEFAULT);
        return null;
    }

    public Stream<BulkResponse> batchInsert(Stream<IndexDocument> contents) {
        var batches = splitStreamToBatches(contents);
        return batches.map(attempt(this::insertBatch)).map(Try::orElseThrow);
    }

    private Stream<List<IndexDocument>> splitStreamToBatches(Stream<IndexDocument> indexDocuments) {
        UnmodifiableIterator<List<IndexDocument>> bulks = Iterators.partition(
            indexDocuments.iterator(), BULK_SIZE);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(bulks, Spliterator.ORDERED), SEQUENTIAL);
    }

    private BulkResponse insertBatch(List<IndexDocument> bulk) throws IOException {
        List<IndexRequest> indexRequests = bulk.stream()
            .parallel()
            .map(IndexDocument::toIndexRequest)
            .collect(Collectors.toList());

        BulkRequest request = new BulkRequest();
        indexRequests.forEach(request::add);
        request.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
        request.waitForActiveShards(ActiveShardCount.ONE);
        return elasticSearchClient.bulk(request, RequestOptions.DEFAULT);
    }

    public Void deleteIndex(String indexName) throws IOException {
        elasticSearchClient.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        return null;
    }
}
