package no.unit.nva.search2.common.records;

import java.net.URI;
import org.opensearch.search.builder.SearchSourceBuilder;

public record QueryContentWrapper(SearchSourceBuilder source, URI requestUri) {

}
