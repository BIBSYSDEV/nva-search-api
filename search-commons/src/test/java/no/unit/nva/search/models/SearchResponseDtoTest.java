package no.unit.nva.search.models;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Test;

class SearchResponseDtoTest {

    @Test
    void builderReturnsObjectWithoutAnyEmptyField() {
        SearchResponseDto response = randomResponse();
        assertThat(response, doesNotHaveEmptyValues());
    }



    private SearchResponseDto randomResponse() {
        return SearchResponseDto.builder()
            .withContext(randomUri())
            .withId(randomUri())
            .withSize(randomInteger())
            .withProcessingTime(randomInteger())
            .withHits(randomJsonList())
            .withAggregations(randomJsonNode())
            .build();
    }

    private List<JsonNode> randomJsonList() {
        return Stream.of(randomJson(), randomJson())
            .map(attempt(dtoObjectMapper::readTree))
            .flatMap(Try::stream)
            .collect(Collectors.toList());
    }

    private JsonNode randomJsonNode() {
        return attempt(() -> objectMapper.readTree(randomJson())).orElseThrow();
    }
}
