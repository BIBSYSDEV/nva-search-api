package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.IndexingClient.defaultIndexingClient;
import static no.unit.nva.search.constants.ApplicationConstants.DOIREQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.MESSAGES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_INDEX;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestHandler<Object, String> {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED. See logs";
    private final IndexingClient indexingClient;
    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);

    @JacocoGenerated
    public InitHandler() {
        this(defaultIndexingClient());
    }

    public InitHandler(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }

    @Override
    public String handleRequest(Object input, Context context) {

        var failState = new AtomicBoolean(false);

        attempt(() -> indexingClient.createIndex(RESOURCES_INDEX)).orElse(fail -> handleFailure(failState, fail));
        attempt(() -> indexingClient.createIndex(DOIREQUESTS_INDEX)).orElse(fail -> handleFailure(failState, fail));
        attempt(() -> indexingClient.createIndex(MESSAGES_INDEX)).orElse(fail -> handleFailure(failState, fail));
        attempt(() -> indexingClient.createIndex(PUBLISHING_REQUESTS_INDEX)).orElse(
            fail -> handleFailure(failState, fail)
        );

        return failState.get() ? FAILED : SUCCESS;
    }

    private Void handleFailure(AtomicBoolean failState, Failure failure) {
        failState.set(true);
        logger.warn("Index creation failed", failure.getException());
        return null;
    }
}
