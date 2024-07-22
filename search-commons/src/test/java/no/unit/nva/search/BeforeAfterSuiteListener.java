package no.unit.nva.search;


import no.unit.nva.search.common.Containers;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BeforeAfterSuiteListener implements TestExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(ResourceClientTest.class);

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestExecutionListener.super.testPlanExecutionStarted(testPlan);
        try {
            logger.info("before all...");
            Containers.setup();
        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        TestExecutionListener.super.testPlanExecutionFinished(testPlan);
        try {
            logger.info("after all...");
            Containers.afterAll();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }


}
