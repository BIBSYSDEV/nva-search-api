package no.unit.nva.search.common.builder;

import static org.junit.Assert.assertNotNull;

import no.unit.nva.search.resource.ResourceParameter;

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
