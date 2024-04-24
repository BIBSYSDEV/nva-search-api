package no.unit.nva.search2.common;

import no.unit.nva.search.IndicesClientWrapper;
import nva.commons.core.JacocoGenerated;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Class for avoiding mocking/spying the ES final classes.
 */
public class RestHighLevelClientWrapper {


    public static final String INITIAL_LOG_MESSAGE = "Connecting to search infrastructure at {}";
    private static final Logger logger = LoggerFactory.getLogger(RestHighLevelClientWrapper.class);

    private final RestHighLevelClient client;


    public RestHighLevelClientWrapper(RestClientBuilder clientBuilder) {
        this.client = new RestHighLevelClient(clientBuilder);
        logger.info(INITIAL_LOG_MESSAGE, clientBuilder);
    }


    @JacocoGenerated
    public SearchResponse search(SearchRequest searchRequest, RequestOptions requestOptions) throws IOException {
        return client.search(searchRequest, requestOptions);
    }

    @JacocoGenerated
    public IndexResponse index(IndexRequest updateRequest, RequestOptions requestOptions) throws IOException {
        return client.index(updateRequest, requestOptions);
    }


    @JacocoGenerated
    public IndicesClientWrapper indices() {
        return new IndicesClientWrapper(client.indices());
    }


}
