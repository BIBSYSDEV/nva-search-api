package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.enums.ResourceParameter;
import org.junit.jupiter.api.Test;

class OpensearchQueryToolsTest {

    @Test
    void rangeQuery() {
        var queryRange = new OpensearchQueryRange<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE, "test")
        );
    }

}