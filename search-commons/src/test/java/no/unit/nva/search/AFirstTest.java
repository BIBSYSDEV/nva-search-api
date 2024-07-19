package no.unit.nva.search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AFirstTest {

    private static final Logger logger = LoggerFactory.getLogger(AFirstTest.class);

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        TestRoot.setup();
    }

    @Test
    public void test() {
        logger.info("setting up stuff");
    }

}
