package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.constants.ApplicationConstants.DOIREQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.MESSAGES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_INDEX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteIndicesHandlerTest {
    
    private final Set<String> deletedIndexes = Collections.synchronizedSet(new HashSet<>());
    private DeleteIndicesHandler handler;
    private FakeContext context;
    
    @BeforeEach
    public void setup() {
        FakeIndexingClient indexingClient = indexingClientRegisteringDeleteIndexRequests();
        handler = new DeleteIndicesHandler(indexingClient);
        context = new FakeContext();
    }
    
    @Test
    void shouldDeleteAllIndices() {
        handler.handleRequest(null, context);
        var expectedDeletedIndices =
            new String[]{RESOURCES_INDEX, DOIREQUESTS_INDEX, PUBLISHING_REQUESTS_INDEX, MESSAGES_INDEX};
        assertThat(deletedIndexes, containsInAnyOrder(expectedDeletedIndices));
    }
    
    private FakeIndexingClient indexingClientRegisteringDeleteIndexRequests() {
        return new FakeIndexingClient() {
            @Override
            public Void deleteIndex(String indexName) {
                deletedIndexes.add(indexName);
                return null;
            }
        };
    }
}