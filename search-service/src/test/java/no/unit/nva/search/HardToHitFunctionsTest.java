package no.unit.nva.search;

import static no.unit.nva.search.model.constant.Words.COMMA;
import static no.unit.nva.search.model.constant.Words.SPACE;
import static no.unit.nva.search.model.enums.FieldOperator.BETWEEN;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.unit.nva.search.model.builder.PartOfQuery;
import no.unit.nva.search.model.builder.RangeQuery;
import no.unit.nva.search.model.enums.ParameterKey;
import no.unit.nva.search.model.enums.SortKey;
import no.unit.nva.search.service.importcandidate.ImportCandidateParameter;
import no.unit.nva.search.service.importcandidate.ImportCandidateSort;
import no.unit.nva.search.service.resource.ResourceParameter;
import no.unit.nva.search.service.resource.ResourceSort;
import no.unit.nva.search.service.ticket.TicketParameter;
import no.unit.nva.search.service.ticket.TicketSort;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class HardToHitFunctionsTest {
    private static final Logger logger = LoggerFactory.getLogger(HardToHitFunctionsTest.class);
    private static final String TEST = "test";

    @Test
    void invalidRangeQueryMust() {
        var queryRange = new RangeQuery<ResourceParameter>();
        assertThrows(
                IllegalArgumentException.class,
                () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE, TEST));
    }

    @Test
    void invalidResourceParameterFailsWhenInvokingBuildQuery() {
        var queryRange = new PartOfQuery<ResourceParameter>();
        var fakeResourceParameter = mock(ResourceParameter.class);
        when(fakeResourceParameter.subQuery()).thenReturn(ResourceParameter.CREATED_BEFORE);
        when(fakeResourceParameter.searchOperator()).thenReturn(BETWEEN);
        assertThrows(
                IllegalStateException.class,
                () -> queryRange.buildQuery(fakeResourceParameter, "2021-01-01"));
    }

    @Test
    void invalidRangeQueryMustNot() {
        var queryRange = new RangeQuery<ResourceParameter>();
        assertThrows(
                IllegalArgumentException.class,
                () -> queryRange.buildQuery(ResourceParameter.CONTEXT_TYPE_NOT, TEST));
    }

    @Test
    void checkTicketSort() {
        assertNotNull(TicketSort.STATUS.asCamelCase());
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
                                key.searchFields().collect(Collectors.joining(COMMA + SPACE))));
    }

    private void printEnumSort(Stream<SortKey> parameterSortKeyStream) {
        parameterSortKeyStream.forEach(
                key ->
                        logger.info(
                                "|{}|{}|{}|{}|",
                                key.asLowerCase(),
                                key.asCamelCase(),
                                key.keyPattern(),
                                key.jsonPaths().collect(Collectors.joining(COMMA + SPACE))));
    }
}
