package no.unit.nva.search.model.builder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PartOfQueryTest {

    @Test
    void checkPartOfQuery() {
        var partOfQuery = new PartOfQuery<FakeParameter>();
        assertNotNull(
                partOfQuery.buildQuery(FakeParameter.CONTRIBUTORS_OF_CHILD, "ewsrdf").findFirst());
    }
}
