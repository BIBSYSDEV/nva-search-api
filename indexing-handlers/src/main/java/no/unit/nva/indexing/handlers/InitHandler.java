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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestHandler<Object, String> {
    
    public static final String FINISHED = "FINISHED";
    private final IndexingClient indexingClient;
    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
    
    @JacocoGenerated
    public InitHandler() {
        this(new IndexingClient());
    }
    
    public InitHandler(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }
    
    @Override
    public String handleRequest(Object input, Context context) {
        
        attempt(() -> indexingClient.createIndex(RESOURCES_INDEX)).orElse(fail -> logError(fail.getException()));
        attempt(() -> indexingClient.createIndex(DOIREQUESTS_INDEX)).orElse(fail -> logError(fail.getException()));
        attempt(() -> indexingClient.createIndex(MESSAGES_INDEX)).orElse(fail -> logError(fail.getException()));
        attempt(() -> indexingClient.createIndex(PUBLISHING_REQUESTS_INDEX)).orElse(
            fail -> logError(fail.getException()));
        return FINISHED;
    }
    
    private Void logError(Exception exception) {
        logger.warn("Index creation failed", exception);
        return null;
    }
}
