package no.unit.nva.search.common;

import static no.unit.nva.search.common.OpenSearchClientException.asBadRequestIfTooManyClauses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletionException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

class OpenSearchClientExceptionTest {

  @Test
  void shouldReturnStatusCodeAndMessage() {
    int expectedStatusCode = 500;
    var expectedMessage = "Internal Server Error";

    var exception = new OpenSearchClientException(expectedStatusCode, expectedMessage);

    assertEquals(expectedStatusCode, exception.getStatusCode());
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestWhenCauseIsTooManyNestedClauses() {
    var cause =
        new OpenSearchClientException(
            400, "too_many_nested_clauses: Query contains too many nested clauses");
    var completionException = new CompletionException(cause);

    assertThrows(
        BadRequestException.class, () -> asBadRequestIfTooManyClauses(completionException));
  }

  @Test
  void shouldThrowBadRequestWhenCauseIsTooManyClauses() {
    var cause = new OpenSearchClientException(400, "too_many_clauses: maxClauseCount is set");
    var completionException = new CompletionException(cause);

    assertThrows(
        BadRequestException.class, () -> asBadRequestIfTooManyClauses(completionException));
  }

  @Test
  void shouldReturnOriginalExceptionWhenCauseIsAnotherOpenSearchError() throws BadRequestException {
    var cause = new OpenSearchClientException(503, "cluster unavailable");
    var completionException = new CompletionException(cause);

    assertSame(completionException, asBadRequestIfTooManyClauses(completionException));
  }

  @Test
  void shouldReturnOriginalExceptionWhenCauseHasNoMessage() throws BadRequestException {
    var cause = new OpenSearchClientException(500, null);
    var completionException = new CompletionException(cause);

    assertSame(completionException, asBadRequestIfTooManyClauses(completionException));
  }

  @Test
  void shouldReturnOriginalExceptionWhenCauseIsNotOpenSearchClientException()
      throws BadRequestException {
    var completionException = new CompletionException(new IllegalStateException("boom"));

    assertSame(completionException, asBadRequestIfTooManyClauses(completionException));
  }
}
