package no.sikt.nva.search.eventhandlers;

import java.io.IOException;
import java.util.Map;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.IndexDocument;

public class FailingIndexingClient extends IndexingClient {
  private final IOException failingException;

  public FailingIndexingClient(IOException failingException) {
    super(null, null);
    this.failingException = failingException;
  }

  @Override
  public void refreshIndex(String indexName) throws IOException {
    throw failingException;
  }

  @Override
  public Void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
    throw failingException;
  }

  @Override
  public void removeDocumentFromIndex(String identifier, String index) throws IOException {
    throw failingException;
  }

  @Override
  public Void createIndex(String indexName) throws IOException {
    throw failingException;
  }

  @Override
  public Void createIndex(String indexName, Map<String, ?> mappings) throws IOException {
    throw failingException;
  }

  @Override
  public Void createIndex(String indexName, Map<String, ?> mappings, Map<String, ?> settings)
      throws IOException {
    throw failingException;
  }

  @Override
  public Void deleteIndex(String indexName) throws IOException {
    throw failingException;
  }
}
