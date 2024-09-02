package no.unit.nva.indexing.testutils;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;

import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.IndexDocument;

import org.opensearch.action.DocWriteRequest.OpType;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Faking the Indexing Client instead of the OpenSearch client because faking the OpenSearch client
 * is difficult.
 */
public class FakeIndexingClient extends IndexingClient {

    private static final long IGNORED_PROCESSING_TIME = 0;
    private final Map<String, Map<String, JsonNode>> indexContents;

    public FakeIndexingClient() {
        super(null, null);
        indexContents = new ConcurrentHashMap<>();
    }

    @Override
    public Void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
        if (!indexContents.containsKey(indexDocument.getIndexName())) {
            indexContents.put(indexDocument.getIndexName(), new HashMap<>());
        }

        indexContents
                .get(indexDocument.getIndexName())
                .put(indexDocument.getDocumentIdentifier(), indexDocument.resource());
        return null;
    }

    @Override
    public void removeDocumentFromResourcesIndex(String identifier) throws IOException {
        indexContents.forEach((index, set) -> removeDocument(set, identifier));
    }

    @Override
    public void removeDocumentFromImportCandidateIndex(String identifier) throws IOException {
        indexContents.forEach((index, set) -> removeDocument(set, identifier));
    }

    @Override
    public Stream<BulkResponse> batchInsert(Stream<IndexDocument> indexDocuments) {
        var collectedDocuments = indexDocuments.collect(Collectors.toList());
        for (IndexDocument collectedDocument : collectedDocuments) {
            attempt(() -> addDocumentToIndex(collectedDocument)).orElseThrow();
        }

        return constructSampleBulkResponse(collectedDocuments).stream();
    }

    public Set<JsonNode> getIndex(String indexName) {
        return new HashSet<>(this.indexContents.getOrDefault(indexName, new HashMap<>()).values());
    }

    public Set<JsonNode> listAllDocuments(String indexName) {
        return new HashSet<>(this.indexContents.get(indexName).values());
    }

    private void removeDocument(Map<String, JsonNode> jsonNodes, String identifier) {
        jsonNodes.remove(identifier);
    }

    private List<BulkResponse> constructSampleBulkResponse(
            Collection<IndexDocument> indexDocuments) {
        DocWriteResponse response = null;
        List<BulkItemResponse> responses =
                indexDocuments.stream()
                        .map(doc -> new BulkItemResponse(doc.hashCode(), OpType.UPDATE, response))
                        .toList();
        BulkItemResponse[] responsesArray = responses.toArray(BulkItemResponse[]::new);
        return List.of(new BulkResponse(responsesArray, IGNORED_PROCESSING_TIME));
    }
}
