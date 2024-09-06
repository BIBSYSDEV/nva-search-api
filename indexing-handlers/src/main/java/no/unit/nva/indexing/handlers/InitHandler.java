package no.unit.nva.indexing.handlers;

import static no.unit.nva.indexingclient.IndexingClient.defaultIndexingClient;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.DOIREQUESTS_INDEX;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.MESSAGES_INDEX;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.RESOURCES_INDEX;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.TICKETS_INDEX;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import no.unit.nva.indexing.model.IndexRequest;
import no.unit.nva.indexingclient.IndexingClient;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InitHandler implements RequestHandler<Object, String> {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED. See logs";
    public static final String TICKET_MAPPINGS =
            IoUtils.stringFromResources(Path.of("ticket_mappings.json"));
    private static final String RESOURCE_MAPPINGS =
            IoUtils.stringFromResources(Path.of("resource_mappings.json"));
    private static final String RESOURCE_SETTINGS =
            IoUtils.stringFromResources(Path.of("resource_settings.json"));
    private static final List<IndexRequest> INDEXES =
            List.of(
                    new IndexRequest(RESOURCES_INDEX, RESOURCE_MAPPINGS, RESOURCE_SETTINGS),
                    new IndexRequest(DOIREQUESTS_INDEX),
                    new IndexRequest(MESSAGES_INDEX),
                    new IndexRequest(TICKETS_INDEX, TICKET_MAPPINGS),
                    new IndexRequest(PUBLISHING_REQUESTS_INDEX));
    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
    private final IndexingClient indexingClient;

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

        INDEXES.forEach(
                request -> {
                    attempt(
                                    () ->
                                            indexingClient.createIndex(
                                                    request.getName(),
                                                    request.getMappings(),
                                                    request.getSettings()))
                            .orElse(fail -> handleFailure(failState, fail));
                });

        return failState.get() ? FAILED : SUCCESS;
    }

    private Void handleFailure(AtomicBoolean failState, Failure<?> failure) {
        failState.set(true);
        logger.warn("Index creation failed", failure.getException());
        return null;
    }
}
