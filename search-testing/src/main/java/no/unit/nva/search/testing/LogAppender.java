package no.unit.nva.search.testing;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.SPACE;

import java.util.List;
import nva.commons.core.JacocoGenerated;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.CloseResource"})
public final class LogAppender {

  @JacocoGenerated
  private LogAppender() {}

  public static String logToString(ListAppender appender) {
    return String.join(SPACE, eventsToStrings(appender));
  }

  public static ListAppender getAppender(Class<?> clazz) {

    var loggerContext = LoggerContext.getContext(false);
    if (loggerContext.getConfiguration().getAppenders().containsKey(clazz.getSimpleName())) {
      return (ListAppender)
          loggerContext.getConfiguration().getAppenders().get(clazz.getSimpleName());
    } else {
      logHasStarted(clazz);
      var logger = (Logger) loggerContext.getLogger(clazz);
      var appender = new ListAppender(clazz.getSimpleName());
      appender.start();
      loggerContext.getConfiguration().addLoggerAppender(logger, appender);
      return appender;
    }
  }

  private static void logHasStarted(Class<?> clazz) {
    var logAppenderlogger = LoggerFactory.getLogger(LogAppender.class);
    logAppenderlogger.info("Getting appender for class: {}", clazz.getSimpleName());
  }

  private static List<String> eventsToStrings(ListAppender appender) {
    return appender.getEvents().stream().map(LogAppender::eventToString).toList();
  }

  private static String eventToString(LogEvent event) {
    if (nonNull(event.getThrown())) {
      return event.getMessage() + SPACE + event.getThrown().getMessage();
    }
    return event.getMessage().getFormattedMessage();
  }
}
