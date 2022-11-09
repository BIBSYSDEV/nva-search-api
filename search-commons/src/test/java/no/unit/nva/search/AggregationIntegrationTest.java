package no.unit.nva.search;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.testutils.RandomDataGenerator;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Map;

import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Testcontainers
public class AggregationIntegrationTest {

    public static final String INDEX_NAME = RandomDataGenerator.randomString().toLowerCase();
    public static final long DELAY_AFTER_INDEXING = 1000L;

    private SearchClient searchClient;
    private IndexingClient indexingClient;
    private AggregationClient aggregationClient;
    private final OpenSearchContainer container = new OpenSearchContainer();

    @BeforeEach
    void setUp() {
        container.start();

        var httpHostAddress = container.getHttpHostAddress();

        var restClientBuilder = RestClient.builder(HttpHost.create(httpHostAddress));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);

        searchClient = new SearchClient(restHighLevelClientWrapper);
        indexingClient = new IndexingClient(restHighLevelClientWrapper);
        aggregationClient = new AggregationClient(restHighLevelClientWrapper);
    }

    @AfterEach
    void afterEach() {
        container.stop();
    }


    @Test
    void aggregatesOnBidragsyter() throws IOException, InterruptedException {
        var indexDocument = createSampleIndexDocument("publication.json");

        indexingClient.addDocumentToIndex(indexDocument);
        Thread.sleep(DELAY_AFTER_INDEXING);

        var response = aggregationClient.aggregate(Map.of(
                "Bidragsyter", "entityDescription.contributors.identity.name.keyword",
                "Kategori", "entityDescription.reference.publicationInstance.type.keyword"
        ));

        var aggregation = response.getAggregations().asMap();

        assertThat(aggregation.get("Bidragsyter").getName(), is(equalTo("Bidragsyter")));
    }

    private IndexDocument createSampleIndexDocument(String jsonFile) throws IOException {
        var eventConsumptionAttributes = new EventConsumptionAttributes(
                INDEX_NAME,
                SortableIdentifier.next()
        );
        var jsonNode = objectMapperWithEmpty.readValue(inputStreamFromResources(jsonFile),
                JsonNode.class);

        return new IndexDocument(eventConsumptionAttributes, jsonNode);
    }

}
