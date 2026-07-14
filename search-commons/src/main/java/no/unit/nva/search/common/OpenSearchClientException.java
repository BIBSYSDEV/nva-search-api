package no.unit.nva.search.common;

import static java.util.Objects.nonNull;

import java.util.concurrent.CompletionException;
import nva.commons.apigateway.exceptions.BadRequestException;

public class OpenSearchClientException extends RuntimeException {
  private static final String TOO_MANY_NESTED_CLAUSES = "too_many_nested_clauses";
  private static final String TOO_MANY_CLAUSES = "too_many_clauses";
  private static final String SEARCH_TOO_COMPLEX_MESSAGE =
      "The search generated too many query clauses. Simplify the search and try again.";

  private final int statusCode;

  public OpenSearchClientException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public static RuntimeException asBadRequestIfTooManyClauses(CompletionException exception)
      throws BadRequestException {
    if (exception.getCause() instanceof OpenSearchClientException cause
        && cause.isTooManyClausesError()) {
      throw new BadRequestException(SEARCH_TOO_COMPLEX_MESSAGE);
    }
    return exception;
  }

  private boolean isTooManyClausesError() {
    return nonNull(getMessage())
        && (getMessage().contains(TOO_MANY_NESTED_CLAUSES)
            || getMessage().contains(TOO_MANY_CLAUSES));
  }
}
