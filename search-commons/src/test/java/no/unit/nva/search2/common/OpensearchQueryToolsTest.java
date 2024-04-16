package no.unit.nva.search2.common;

import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.importcandidate.ImportCandidateParameter;
import no.unit.nva.search2.ticket.TicketParameter;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.resource.ResourceParameter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class OpensearchQueryToolsTest {
    private static final Logger logger = LoggerFactory.getLogger(OpensearchQueryToolsTest.class);

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

    @Test
    @Disabled
    void printResourceParameter() {
        printEnum(Arrays.stream(ResourceParameter.values()));
    }

    @Test
    @Disabled
    void printTicketParameter() {
        printEnum(Arrays.stream(TicketParameter.values()));
    }

    @Test
    @Disabled
    void printImportCandidateParameter() {
        printEnum(Arrays.stream(ImportCandidateParameter.values()));
    }


    private void printEnum(Stream<ParameterKey> parameterKeyStream) {
        parameterKeyStream.forEach(key ->
            logger.info("|{}|{}|{}|{}|{}|",
                key.asLowerCase(),
                key.asCamelCase(),
                key.fieldType().asCamelCase(),
                key.searchOperator().asLowerCase(),
                key.searchFields().collect(Collectors.joining(", "))
            )
        );
    }
}