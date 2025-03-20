package no.unit.nva.search.common;

public class OpenSearchClientException extends RuntimeException {
  private final int statusCode;

  public OpenSearchClientException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
