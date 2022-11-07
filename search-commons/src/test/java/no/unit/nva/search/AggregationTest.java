package no.unit.nva.search;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class AggregationTest {

    private final OpenSearchContainer container = new OpenSearchContainer();

    @BeforeEach
    void setUp() {
        container.start();
    }

    @AfterEach
    void afterEach() {
        container.stop();
    }


}
