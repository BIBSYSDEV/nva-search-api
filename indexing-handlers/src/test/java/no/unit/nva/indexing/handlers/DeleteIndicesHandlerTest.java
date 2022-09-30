package no.unit.nva.indexing.handlers;


import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.constants.ApplicationConstants;
import no.unit.nva.stubs.FakeContext;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.StringContains.containsString;

class DeleteIndicesHandlerTest {

    @Test
    void shouldDeleteIndicesWhenFunctionIsInvoked() {
        final var buffer = new ArrayList<String>();
        var indexingClient = new IndexingClient(null) {
            @Override
            public Void deleteIndex(String indexName) throws IOException {
                buffer.add(indexName);
                return null;
            }
        };
        var handler = new DeleteIndicesHandler(indexingClient);
        handler.handleRequest(null, new FakeContext());
        assertThat(buffer, containsInAnyOrder(ApplicationConstants.ALL_INDICES.toArray(String[]::new)));
    }

    @Test
    void shouldLogWarningWhenIndexDeletionFails() {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var expectedMessage = randomString();
        var indexingClient = new IndexingClient(null) {
            @Override
            public Void deleteIndex(String indexName) throws IOException {
                throw new RuntimeException(expectedMessage);
            }
        };
        var handler = new DeleteIndicesHandler(indexingClient);
        handler.handleRequest(null, new FakeContext());
        assertThat(logger.getMessages(), containsString(expectedMessage));
    }
}