package no.unit.nva.search2.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.resource.ResourceParameter;
import org.junit.jupiter.api.Test;

class OpensearchQueryToolsTest {

    @Test
    void invalidRangeQueryMust() {
        var queryRange = new OpensearchQueryRange<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE, "test")
        );
    }

    @Test
    void invalidRangeQueryMustNot() {
        var queryRange = new OpensearchQueryRange<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE_NOT, "test")
        );
    }

    @Test
    void invalidRangeQueryShould() {
        var queryRange = new OpensearchQueryRange<ResourceParameter>();
        assertThrows(
            IllegalArgumentException.class,
            () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE_SHOULD, "test")
        );
    }

}