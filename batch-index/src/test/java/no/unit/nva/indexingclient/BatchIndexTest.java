package no.unit.nva.indexingclient;

import static no.unit.nva.indexingclient.TestConstants.RESOURCE_INDEX_NAME;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexingclient.models.IndexDocument;
import org.opensearch.action.DocWriteRequest.OpType;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkItemResponse.Failure;
import org.opensearch.action.bulk.BulkResponse;

public class BatchIndexTest {

  public static final Context CONTEXT = mock(Context.class);
  private static final Random RANDOM = new Random();
  private static final int ARBITRARY_QUERY_TIME = 123;

  protected FakeIndexingClient failingOpenSearchClient() {
    return new FakeIndexingClient() {
      @Override
      public Stream<BulkResponse> batchInsert(Stream<IndexDocument> indexDocuments) {
        List<BulkItemResponse> itemResponses =
            indexDocuments
                .map(IndexDocument::getDocumentIdentifier)
                .map(id -> createFailure(id))
                .map(fail -> new BulkItemResponse(randomNumber(), OpType.UPDATE, fail))
                .toList();
        BulkResponse response =
            new BulkResponse(itemResponses.toArray(BulkItemResponse[]::new), ARBITRARY_QUERY_TIME);
        return Stream.of(response);
      }
    };
  }

  private Failure createFailure(String identifier) {
    return new Failure(RESOURCE_INDEX_NAME, identifier, new Exception("failingBulkIndexMessage"));
  }

  private int randomNumber() {
    return RANDOM.nextInt();
  }
}
