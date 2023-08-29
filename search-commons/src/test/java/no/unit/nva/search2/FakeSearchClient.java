package no.unit.nva.search2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.model.Publication;
import no.unit.nva.search2.aws.SearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

public class FakeSearchClient implements SearchClient<Publication> {

    private final Map<String, Publication> indexContents;

    public FakeSearchClient() {
        indexContents = new ConcurrentHashMap<>();
    }

    @Override
    public void addDocumentToIndex(Publication indexDocument) {

    }

    @Override
    public void removeDocumentFromIndex(Publication indexDocument) {
        indexContents.put(indexDocument.getIdentifier().toString(), indexDocument);

    }

    @Override
    public SearchResponse<Publication> search(Query query) {
        return null;
    }

    @Override
    public void deleteIndex() {

    }

    public Set<Publication> listAllDocuments() {
        return new HashSet<>(this.indexContents.values());
    }
}
