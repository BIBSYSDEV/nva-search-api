package no.unit.nva.indexingclient.models;

import no.unit.nva.indexingclient.constants.ApplicationConstants;

import nva.commons.core.JacocoGenerated;

import org.apache.http.HttpHost;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** Class for avoiding mocking/spying the ES final classes. */
public class RestHighLevelClientWrapper {

    public static final String INITIAL_LOG_MESSAGE = "Connecting to search infrastructure at {}";
    private static final Logger logger = LoggerFactory.getLogger(RestHighLevelClientWrapper.class);
    public static final String SEARCH_INFRASTRUCTURE_CREDENTIALS =
            "SearchInfrastructureCredentials";

    private final RestHighLevelClient client;

    public RestHighLevelClientWrapper(RestHighLevelClient client) {
        this.client = client;
    }

    public RestHighLevelClientWrapper(RestClientBuilder clientBuilder) {
        this(new RestHighLevelClient(clientBuilder));
        logger.debug(INITIAL_LOG_MESSAGE, clientBuilder);
    }

    /**
     * Use this method only to experiment and to extend the functionality of the wrapper.
     *
     * @return the contained client
     */
    @JacocoGenerated
    public RestHighLevelClient getClient() {
        logger.warn("Use getClient only for finding which methods you need to add to the wrapper");
        return this.client;
    }

    @JacocoGenerated
    public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions)
            throws IOException {
        return client.search(searchRequest, requestOptions);
    }

    @JacocoGenerated
    public IndexResponse index(IndexRequest updateRequest, RequestOptions requestOptions)
            throws IOException {
        return client.index(updateRequest, requestOptions);
    }

    @JacocoGenerated
    public DeleteResponse delete(DeleteRequest deleteRequest, RequestOptions requestOptions)
            throws IOException {
        return client.delete(deleteRequest, requestOptions);
    }

    @JacocoGenerated
    public UpdateResponse update(UpdateRequest updateRequest, RequestOptions requestOptions)
            throws IOException {
        return client.update(updateRequest, requestOptions);
    }

    @JacocoGenerated
    public IndicesClientWrapper indices() {
        return new IndicesClientWrapper(client.indices());
    }

    @JacocoGenerated
    public BulkResponse bulk(BulkRequest request, RequestOptions requestOption) throws IOException {
        return client.bulk(request, requestOption);
    }

    public static RestHighLevelClientWrapper defaultRestHighLevelClientWrapper() {
        return prepareRestHighLevelClientWrapperForUri(
                ApplicationConstants.SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static RestHighLevelClientWrapper prepareRestHighLevelClientWrapperForUri(
            String address) {
        return new RestHighLevelClientWrapper(RestClient.builder(HttpHost.create(address)));
    }
}
