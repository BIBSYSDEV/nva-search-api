package no.unit.nva.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InjectedTestSettings implements TestExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(InjectedTestSettings.class);

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    TestExecutionListener.super.testPlanExecutionStarted(testPlan);
    Configurator.setAllLevels("", Level.WARN);
    logger.info("Setting up Opensearch server");
    Containers.setup();
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    TestExecutionListener.super.testPlanExecutionFinished(testPlan);
    try {
      logger.info("Closing Opensearch server");
      Containers.afterAll();
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
  }
}
