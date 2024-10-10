package no.umit.nva.search.model.builder;

import static org.junit.Assert.assertNotNull;


import org.junit.jupiter.api.Test;

class PartOfQueryTest {

    @Test
    void checkPartOfQuery() {
        var partOfQuery = new PartOfQuery<ResourceParameter>();
        assertNotNull(
                partOfQuery.buildMatchAnyValueQuery(
                        ResourceParameter.CONTRIBUTORS_OF_CHILD, "ewsrdf"));
    }
}
