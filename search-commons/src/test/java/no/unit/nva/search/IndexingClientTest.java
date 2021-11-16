package no.unit.nva.search;

import static no.unit.nva.search.IndexingConfig.objectMapper;
import static no.unit.nva.search.SearchClient.BULK_SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.exception.SearchException;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class IndexingClientTest {

    public static final int NUMBER_NOT_DIVIDABLE_BY_BLOCK_SIZE = 1256;

    @Test
    void shouldCreateDefaultObjectWithoutFailing() {
        assertDoesNotThrow((Executable) IndexingClient::new);
    }

    @Test
    void shouldIndexAllDocumentsInBatchInBulksOfSpecifiedSize() throws IOException {
        RestHighLevelClientWrapper esClient = mock(RestHighLevelClientWrapper.class);
        IndexingClient client = new IndexingClient(esClient);
        var indexDocuments = IntStream.range(0, NUMBER_NOT_DIVIDABLE_BY_BLOCK_SIZE)
            .boxed()
            .map(i -> randomJson())
            .map(this::toIndexDocument)
            .collect(Collectors.toList());
        List<BulkResponse> provokeExecution = client.batchInsert(indexDocuments.stream()).collect(Collectors.toList());

        assertThat(provokeExecution, is(not(nullValue())));
        int expectedNumberOfBulkRequests = (int) Math.ceil(((double) indexDocuments.size()) / ((double) BULK_SIZE));
        verify(esClient, times(expectedNumberOfBulkRequests))
            .bulk(any(BulkRequest.class), any(RequestOptions.class));
    }

    @Test
    void shouldThrowSearchExceptionWhenIndexDocumentCannotBeIndexed() throws IOException {

        IndexDocument indexDocument = mock(IndexDocument.class);
        RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.index(any(), any())).thenThrow(new RuntimeException());
        IndexingClient indexingClient = new IndexingClient(restHighLevelClient);

        assertThrows(SearchException.class, () -> indexingClient.addDocumentToIndex(indexDocument));
    }

    @Test
    void shouldThrowExceptionWhenRemovingDocumentFails() throws IOException {

        IndexDocument indexDocument = mock(IndexDocument.class);
        doThrow(RuntimeException.class).when(indexDocument).toJsonString();
        RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.update(any(), any())).thenThrow(new RuntimeException());
        IndexingClient indexingClient = new IndexingClient(restHighLevelClient);
        new IndexingClient(restHighLevelClient);

        assertThrows(SearchException.class, () -> indexingClient.removeDocumentFromIndex(""));
    }

    @Test
    void shouldThrowExceptionWhenInsertingDocumentFails() throws IOException {
        IndexDocument indexDocument = mock(IndexDocument.class);
        doThrow(RuntimeException.class).when(indexDocument).toJsonString();
        RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.index(any(), any())).thenThrow(new RuntimeException());
        IndexingClient elasticSearchRestClient =
            new IndexingClient(restHighLevelClient);

        assertThrows(SearchException.class, () -> elasticSearchRestClient.addDocumentToIndex(indexDocument));
    }

    @Test
    void shouldThrowExceptionWhenDeletingDocumentFails() throws IOException {
        IndexDocument indexDocument = mock(IndexDocument.class);
        doThrow(RuntimeException.class).when(indexDocument).toJsonString();
        RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.update(any(), any())).thenThrow(new RuntimeException());
        IndexingClient indexingClient = new IndexingClient(restHighLevelClient);
        assertThrows(SearchException.class, () -> indexingClient.removeDocumentFromIndex(""));
    }

    @Test
    void shouldNotThrowExceptionWhenTryingToDeleteNonExistingDocument() throws IOException {
        RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        DeleteResponse nothingFoundResponse = mock(DeleteResponse.class);
        when(nothingFoundResponse.getResult()).thenReturn(DocWriteResponse.Result.NOT_FOUND);
        when(restHighLevelClient.delete(any(), any())).thenReturn(nothingFoundResponse);
        IndexingClient indexingClient = new IndexingClient(restHighLevelClient);
        assertDoesNotThrow(() -> indexingClient.removeDocumentFromIndex("1234"));
    }

    @Test
    void addDocumentToIndex() throws IOException, SearchException {
        UpdateResponse updateResponse = mock(UpdateResponse.class);
        IndexDocument mockDocument = mock(IndexDocument.class);
        when(mockDocument.toJsonString()).thenReturn("{}");
        when(mockDocument.getDocumentIdentifier()).thenReturn(SortableIdentifier.next().toString());
        RestHighLevelClientWrapper restHighLevelClient = mock(RestHighLevelClientWrapper.class);
        when(restHighLevelClient.update(any(), any())).thenReturn(updateResponse);
        IndexingClient indexingClient = new IndexingClient(restHighLevelClient);
        indexingClient.addDocumentToIndex(mockDocument);
    }

    private IndexDocument toIndexDocument(String jsonString) {
        var consumptionAttributes = new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
        var json = attempt(() -> objectMapper.readTree(jsonString)).orElseThrow();
        return new IndexDocument(consumptionAttributes, json);
    }
}
