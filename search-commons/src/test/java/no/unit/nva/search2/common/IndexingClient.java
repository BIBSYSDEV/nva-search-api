package no.unit.nva.search2.common;

import static no.unit.nva.search2.common.constant.Functions.readSearchInfrastructureApiUri;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search2.common.security.CachedJwtProvider;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.compress.CompressedXContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static nva.commons.core.attempt.Try.attempt;

public class IndexingClient extends AuthenticatedOpenSearchClientWrapper {

    public static final String INITIAL_LOG_MESSAGE = "using search infrastructure endpoint {} and index {}";
    private static final Logger logger = LoggerFactory.getLogger(IndexingClient.class);

    /**
     * Creates a new OpenSearchRestClient.
     *
     * @param openSearchClient client to use for access to search infrastructure
     */
    public IndexingClient(RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwtProvider) {
        super(openSearchClient, cachedJwtProvider);
    }


    public void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
        logger.info(INITIAL_LOG_MESSAGE, readSearchInfrastructureApiUri(), indexDocument.getDocumentIdentifier());
        openSearchClient.index(indexDocument.toIndexRequest(), getRequestOptions());
    }


    public void createIndex(String indexName, Map<String, ?> mappings) throws IOException {
        var createRequest = new CreateIndexRequest(indexName);
        createRequest.mapping(mappings);
        openSearchClient.indices().create(createRequest, getRequestOptions());
    }


    public void deleteIndex(String indexName) throws IOException {
        openSearchClient.indices().delete(new DeleteIndexRequest(indexName), getRequestOptions());
    }

    public JsonNode getMapping(String indexName) {
        return attempt(() -> getMappingMetadata(indexName))
                   .map(MappingMetadata::source)
                   .map(CompressedXContent::uncompressed)
                   .map(BytesReference::utf8ToString)
                   .map(JsonUtils.dtoObjectMapper::readTree)
                   .orElseThrow();
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
