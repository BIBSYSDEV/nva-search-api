package no.unit.nva.search.models;

import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spotify.hamcrest.jackson.JsonMatchers;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.indexing.testutils.SearchResponseUtil;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchResponse;

class SearchResponseDtoTest {

    public static final String SAMPLE_OPENSEARCH_RESPONSE_JSON = "sample_opensearch_response.json";
    public static final String EXPECTED_AGGREGATIONS = "sample_opensearch_response_searchresponsedto_aggregations.json";

    @Test
    void builderReturnsObjectWithoutAnyEmptyField() {
        SearchResponseDto response = randomResponse();
        assertThat(response, doesNotHaveEmptyValues());
    }

    @Test
    void jsonSerializationShouldKeepDeprecatedFieldsUntilFrontendHasMigrated() throws JsonProcessingException {
        SearchResponseDto searchResponse = randomResponse();
        var serialized = dtoObjectMapper.writeValueAsString(searchResponse);
        var json = (ObjectNode) dtoObjectMapper.readTree(serialized);
        // took and total are the deprecated fields
        assertThat(json, is(jsonObject().where("took", JsonMatchers.jsonLong(searchResponse.getProcessingTime()))));
        assertThat(json, is(jsonObject().where("total", JsonMatchers.jsonLong(searchResponse.getSize()))));
    }

    @Test
    void searchResponseShouldFormatAggregationsCorrectly() throws IOException {
        var searchResponse = getSearchResponse();
        var aggregations = SearchResponseDto.fromSearchResponse(searchResponse, randomUri()).getAggregations();

        var expected = objectMapperWithEmpty.readValue(inputStreamFromResources(EXPECTED_AGGREGATIONS),
                                                       JsonNode.class);

        assertThat(aggregations, is(equalTo(expected)));
    }

    private SearchResponse getSearchResponse() throws IOException {
        String jsonResponse = stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_JSON));
        return SearchResponseUtil.getSearchResponseFromJson(jsonResponse);
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
