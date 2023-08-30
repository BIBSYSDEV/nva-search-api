package no.unit.nva.search2;

import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceQueryTest {


    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void builder(Map<String, String> parameters) throws BadRequestException {
        var test = ResourceQuery.builder()
            .fromQueryParameters(parameters)
            .build();

        var uri = test.toURI()


    }
}