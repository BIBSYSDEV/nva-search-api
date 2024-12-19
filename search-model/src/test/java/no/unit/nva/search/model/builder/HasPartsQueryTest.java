package no.unit.nva.search.model.builder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HasPartsQueryTest {
    @Test
    void checkHasPartsQuery() {
        var hasPartsQuery = new HasPartsQuery<FakeParameter>();
        assertNotNull(
                hasPartsQuery
                        .buildQuery(FakeParameter.CONTRIBUTORS_OF_CHILD, "ewsrdf")
                        .findFirst());
    }
}
