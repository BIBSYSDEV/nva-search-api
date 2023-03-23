package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.IndexingClient.defaultIndexingClient;
import static no.unit.nva.search.constants.ApplicationConstants.DOIREQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.MESSAGES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.TICKETS_INDEX;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.type.TypeReference;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestHandler<Object, String> {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED. See logs";

    private static final InputStream RESOURCES_MAPPINGS =
            IoUtils.inputStreamFromResources("resources_mapping.json");
    private static final List<IndexRequest> INDEXES = List.of(
            new IndexRequest(RESOURCES_INDEX, RESOURCES_MAPPINGS),
            new IndexRequest(DOIREQUESTS_INDEX),
            new IndexRequest(MESSAGES_INDEX),
            new IndexRequest(TICKETS_INDEX),
            new IndexRequest(PUBLISHING_REQUESTS_INDEX)
    );
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

        INDEXES.forEach(request -> {
            attempt(() -> indexingClient.createIndex(request.getName(), request.getMappings()))
                    .orElse(fail -> handleFailure(failState, fail));
        });

        return failState.get() ? FAILED : SUCCESS;
    }

    private Void handleFailure(AtomicBoolean failState, Failure failure) {
        failState.set(true);
        logger.warn("Index creation failed", failure.getException());
        return null;
    }

    private static class IndexRequest {
        private final String name;
        private final Map<String, Object> mappings;

        public IndexRequest(String name) {
            this.name = name;
            this.mappings = Collections.emptyMap();
        }

        public IndexRequest(String name, InputStream jsonMappings) {
            this.name = name;
            var typeReference = new TypeReference<Map<String, Object>>(){};
            this.mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(jsonMappings, typeReference))
                    .orElseThrow();
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getMappings() {
            return mappings;
        }
    }
}
