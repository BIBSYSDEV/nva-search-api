package no.unit.nva.search;


import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeAfterSuiteListener implements TestExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(ResourceClientTest.class);

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestExecutionListener.super.testPlanExecutionStarted(testPlan);
        logger.info("before all");

    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        TestExecutionListener.super.testPlanExecutionFinished(testPlan);
        logger.info("after all");
    }


}
