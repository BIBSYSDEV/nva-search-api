package no.unit.nva.search.utils;

import static no.unit.nva.search.IndexDocument.getPublicationContextUris;
import static no.unit.nva.search.IndexDocument.toJsonString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.ioutils.IoUtils;

public class IndexDocumentWrapperLinkedData {

    private final UriRetriever uriRetriever;

    public IndexDocumentWrapperLinkedData(UriRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
    }

    public String toFramedJsonLd(JsonNode indexDocument) throws IOException {
        String frame = SearchIndexFrame.fetchFrame();
        return new FramedJsonGenerator(getInputStreams(indexDocument), stringToStream(frame)).getFramedJson();
    }

    private List<InputStream> getInputStreams(JsonNode indexDocument) {
        final List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(stringToStream(toJsonString(indexDocument)));
        inputStreams.addAll(fetchAll(getPublicationContextUris(indexDocument)));
        return inputStreams;
    }

    private Collection<? extends InputStream> fetchAll(List<URI> publicationContextUris) {
        Stream<Optional<String>> uriContent = publicationContextUris.stream().map(this::fetch);
        return uriContent
            .flatMap(Optional::stream)
            .map(IoUtils::stringToStream)
            .collect(Collectors.toList());
    }

    private Optional<String> fetch(URI externalReference) {
        return uriRetriever.getRawContent(externalReference, APPLICATION_JSON_LD.toString());
    }
}
