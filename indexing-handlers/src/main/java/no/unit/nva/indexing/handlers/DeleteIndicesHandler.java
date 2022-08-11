package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.constants.ApplicationConstants.DOIREQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.MESSAGES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_INDEX;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;

public class DeleteIndicesHandler implements RequestHandler<Void, Void> {
    
    private final IndexingClient indexingClient;
    
    @JacocoGenerated
    public DeleteIndicesHandler() {
        this(new IndexingClient());
    }
    
    public DeleteIndicesHandler(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }
    
    @Override
    public Void handleRequest(Void input, Context context) {
        attempt(() -> indexingClient.deleteIndex(RESOURCES_INDEX)).orElseThrow();
        attempt(() -> indexingClient.deleteIndex(DOIREQUESTS_INDEX)).orElseThrow();
        attempt(() -> indexingClient.deleteIndex(MESSAGES_INDEX)).orElseThrow();
        attempt(() -> indexingClient.deleteIndex(PUBLISHING_REQUESTS_INDEX)).orElseThrow();
        return null;
    }
}
