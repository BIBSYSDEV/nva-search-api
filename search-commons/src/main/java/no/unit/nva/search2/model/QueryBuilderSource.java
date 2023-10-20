package no.unit.nva.search2.model;

import java.net.URI;
import org.opensearch.search.builder.SearchSourceBuilder;

public record QueryBuilderSource(SearchSourceBuilder source, URI requestUri, String mediaType) {

}