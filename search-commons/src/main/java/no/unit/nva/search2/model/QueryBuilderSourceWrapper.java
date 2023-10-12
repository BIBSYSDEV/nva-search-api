package no.unit.nva.search2.model;

import java.net.URI;
import com.google.common.net.MediaType;
import org.opensearch.search.builder.SearchSourceBuilder;

public record QueryBuilderSourceWrapper(SearchSourceBuilder source, URI requestUri, MediaType mediaType) {

}
