package no.unit.nva.search.common;

import static org.junit.jupiter.api.Assertions.*;

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
}
