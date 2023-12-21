package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.search2.resource.ResourceParameter;
import org.junit.jupiter.api.Test;

class QueryToolsTest {

    @Test
    void rangeQuery() {
        var qt = new QueryTools<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> qt.rangeQuery(ResourceParameter.CONTEXT_TYPE, "test"));
    }

    @Test
    void getMatchQueryBuilder() {
        var qt = new QueryTools<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> qt.getMatchQueryBuilder(ResourceParameter.MODIFIED_SINCE, "test"));
    }
}