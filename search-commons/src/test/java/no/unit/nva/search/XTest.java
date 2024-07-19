package no.unit.nva.search;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XTest {

    private static final Logger logger = LoggerFactory.getLogger(XTest.class);


    @Test
    public void test() {
        logger.info("setting up stuff");
    }

    @AfterAll
    public static void tearDownAll() throws Exception {
        TestRoot.afterAll();
    }
}
