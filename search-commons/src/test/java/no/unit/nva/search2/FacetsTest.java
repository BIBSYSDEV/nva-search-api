package no.unit.nva.search2;

import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static nva.commons.core.ioutils.IoUtils.streamToString;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search2.common.AggregationFormat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetsTest {
    protected static final Logger logger = LoggerFactory.getLogger(FacetsTest.class);

    @Test
    void shoulCheckMapping() {
        var jsonString = fileToString("sample_aggregations.json");
        var jsonNode = stringToJsonNode(jsonString);

        assert jsonNode != null;
        logger.info(AggregationFormat.apply(jsonNode).toPrettyString());
    }


    private String fileToString(String fileName) {
        return streamToString(inputStreamFromResources(fileName));
    }

    private static JsonNode stringToJsonNode(String aggregation) {
        return attempt(() -> objectMapperWithEmpty.readTree(aggregation)).orElseThrow();
    }
}
