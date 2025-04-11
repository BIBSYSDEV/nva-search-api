package no.sikt.nva.search.eventhandlers;

public class EventHandlingException extends RuntimeException {
  public EventHandlingException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventHandlingException(String message) {
    super(message);
  }
}
