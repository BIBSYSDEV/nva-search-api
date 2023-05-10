package no.unit.nva.indexing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.indexing.model.IndexRequest;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static nva.commons.core.attempt.Try.attempt;

public class ImportCandidateInitHandler implements RequestHandler<Object, String> {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED. See logs";
    public static final String RESOURCES_MAPPING_JSON = "resources_mapping.json";
    private static final String IMPORT_CANDIDATE_MAPPINGS =
            IoUtils.stringFromResources(Path.of(RESOURCES_MAPPING_JSON));
    private static final IndexRequest INDEX = new IndexRequest(IMPORT_CANDIDATES_INDEX, IMPORT_CANDIDATE_MAPPINGS);
    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
    private final IndexingClient indexingClient;

    @JacocoGenerated
    public ImportCandidateInitHandler() {
        this(IndexingClient.defaultIndexingClient());
    }

    public ImportCandidateInitHandler(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }

    @Override
    public String handleRequest(Object input, Context context) {
        var failState = new AtomicBoolean(false);

        attempt(() -> indexingClient.createIndex(INDEX.getName(), INDEX.getMappings()))
                .orElse(fail -> handleFailure(failState, fail));

        return failState.get() ? FAILED : SUCCESS;
    }

    private Void handleFailure(AtomicBoolean failState, Failure failure) {
        failState.set(true);
        logger.warn("Index creation failed", failure.getException());
        return null;
    }
}
