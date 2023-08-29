package no.unit.nva.search2.aws;

import java.io.IOException;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

public interface SearchClient<T> {

    void addDocumentToIndex(T indexDocument);

    void removeDocumentFromIndex(T indexDocument);

    SearchResponse<T> search(Query query) throws IOException;

    void deleteIndex() throws IOException;
}
