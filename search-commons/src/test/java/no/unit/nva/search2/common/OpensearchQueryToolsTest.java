package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.enums.ResourceParameter;
import org.junit.jupiter.api.Test;

class OpensearchQueryToolsTest {

    @Test
    void rangeQuery() {
        var qt = new OpensearchQueryRange<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> qt.buildQuery(ResourceParameter.CONTEXT_TYPE, "test"));
    }

    @Test
    void getMatchQueryBuilder() {
        var qt = new OpensearchQueryText<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> qt.getMatchQueryBuilder(ResourceParameter.MODIFIED_SINCE, "test"));
    }
}