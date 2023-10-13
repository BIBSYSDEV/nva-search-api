package no.unit.nva.opensearch.model.common;

import java.net.URI;
import com.google.common.net.MediaType;
import org.opensearch.search.builder.SearchSourceBuilder;

public record QueryBuilderSourceWrapper(SearchSourceBuilder source, URI requestUri, MediaType mediaType) {

}
