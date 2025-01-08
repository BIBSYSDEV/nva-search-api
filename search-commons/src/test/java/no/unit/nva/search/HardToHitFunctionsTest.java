package no.unit.nva.search;

import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.SPACE;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.common.builder.PartOfQuery;
import no.unit.nva.search.common.builder.RangeQuery;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.importcandidate.ImportCandidateParameter;
import no.unit.nva.search.importcandidate.ImportCandidateSort;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.ticket.TicketParameter;
import no.unit.nva.search.ticket.TicketSort;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
