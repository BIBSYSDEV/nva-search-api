package no.unit.nva.search.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.unit.nva.search.common.builder.RangeQuery;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.importcandidate.ImportCandidateParameter;
import no.unit.nva.search.importcandidate.ImportCandidateSort;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.ticket.TicketParameter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class UntouchedHeroesTest {
    private static final Logger logger = LoggerFactory.getLogger(UntouchedHeroesTest.class);

    @Test
    void invalidRangeQueryMust() {
        var queryRange = new RangeQuery<ResourceParameter>();
        assertThrows(
                IllegalArgumentException.class,
                () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE, "test"));
    }

    @Test
    void invalidRangeQueryMustNot() {
        var queryRange = new RangeQuery<ResourceParameter>();
        assertThrows(
                IllegalArgumentException.class,
                () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE_NOT, "test"));
    }

    @Test
    void invalidSortQueryMust() {
        assertNotNull(ImportCandidateSort.RELEVANCE.asLowerCase());
    }

    @Test
    void printResourceParameter() {
        printEnum(Arrays.stream(ResourceParameter.values()));
    }

    @Test
    void printTicketParameter() {
        printEnum(Arrays.stream(TicketParameter.values()));
    }

    @Test
    void printImportCandidateParameter() {
        printEnum(Arrays.stream(ImportCandidateParameter.values()));
    }

    @Test
    void printSortResourceParameter() {
        printEnumSort(Arrays.stream(ResourceSort.values()));
    }

    private void printEnum(Stream<ParameterKey<?>> parameterKeyStream) {
        parameterKeyStream.forEach(
                key ->
                        logger.info(
                                "|{}|{}|{}|{}|{}|",
                                key.asLowerCase(),
                                key.asCamelCase(),
                                key.fieldType().asCamelCase(),
                                key.searchOperator().asLowerCase(),
                                key.searchFields().collect(Collectors.joining(", "))));
    }

    private void printEnumSort(Stream<SortKey> parameterSortKeyStream) {
        parameterSortKeyStream.forEach(
                key ->
                        logger.info(
                                "|{}|{}|{}|{}|",
                                key.asLowerCase(),
                                key.asCamelCase(),
                                key.keyPattern(),
                                key.jsonPaths().collect(Collectors.joining(", "))));
    }
}
