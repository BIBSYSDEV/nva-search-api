package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.constants.ApplicationConstants.DOIREQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.MESSAGES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_INDEX;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestHandler<Object, String> {
    
    public static final String FINISHED = "FINISHED";
    public static final String FAILED_MESSAGE = "Failed. See logs";
    private final IndexingClient indexingClient;
    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
    
    @JacocoGenerated
    public InitHandler() {
        this(IndexingClient.defaultIndexingClient());
    }
    
    public InitHandler(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }
    
    @Override
    public String handleRequest(Object input, Context context) {
        try {
            indexingClient.createIndex(RESOURCES_INDEX);
            indexingClient.createIndex(DOIREQUESTS_INDEX);
            indexingClient.createIndex(MESSAGES_INDEX);
            indexingClient.createIndex(PUBLISHING_REQUESTS_INDEX);
        } catch (Exception e) {
            logger.warn("Index creation failed", e);
            return FAILED_MESSAGE;
        }

        return FINISHED;
    }
}
