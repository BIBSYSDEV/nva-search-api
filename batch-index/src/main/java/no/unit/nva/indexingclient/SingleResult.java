package no.unit.nva.indexingclient;

import no.unit.nva.indexingclient.models.IndexDocument;

public record SingleResult(
    String messageBody, IndexDocument document, Exception exception, boolean success) {

  static SingleResult success(String messageBody, IndexDocument document) {
    return new SingleResult(messageBody, document, null, true);
  }

  static SingleResult failure(String messageBody, Exception exception) {
    return new SingleResult(messageBody, null, exception, false);
  }

  boolean isSuccess() {
    return success;
  }

  boolean isFailure() {
    return !success;
  }
}
