package no.unit.nva.search2.common;

import no.unit.nva.search2.enums.ResourceParameter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryToolsTest {

    @Test
    void rangeQuery() {
        var queryTools = new QueryTools<ResourceParameter>();
        assertThrows(IllegalArgumentException.class, () -> queryTools.rangeQuery(ResourceParameter.CONTEXT_TYPE, "0"));

//        assertThrows(IllegalArgumentException.class, () -> queryTools.buildQuery(ResourceParameter.PUBLISHED_SINCE, "0"));
    }
}