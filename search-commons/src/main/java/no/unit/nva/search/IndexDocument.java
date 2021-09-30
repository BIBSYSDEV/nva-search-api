package no.unit.nva.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Objects;

import static nva.commons.core.JsonUtils.objectMapper;

public class IndexDocument implements JsonSerializable {

    private static final Logger logger = LoggerFactory.getLogger(IndexDocument.class);
    public static final String INSTANCE_TYPE_JSON_PTR = "/entityDescription/reference/publicationInstance/type";
    public static final String IDENTIFIER_JSON_PTR = "/identifier";
    public static final String MAIN_TITLE_JSON_PTR = "/entityDescription/mainTitle";
    private static final String SERIES_ID_JSON_PTR = "/entityDescription/reference/publicationContext/id";
    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String PROBLEM_SERIALIZING_MESSAGE = "Problem serializing IndexDocument";
    private final JsonNode root;

    public IndexDocument(JsonNode root) {
        this.root = root;
    }

    public static IndexDocument fromPublication(Publication publication) {
        return new IndexDocument(objectMapper.convertValue(publication, JsonNode.class));
    }

    @JacocoGenerated
    public void add(ObjectNode context) {
        ((ObjectNode) root).set("@context", context);
    }

    @JacocoGenerated
    public String getType() {
        return root.at(INSTANCE_TYPE_JSON_PTR).textValue();
    }

    @JacocoGenerated
    public SortableIdentifier getId() {
        return new SortableIdentifier(root.at(IDENTIFIER_JSON_PTR).textValue());
    }

    @JacocoGenerated
    public String getTitle() {
        return root.at(MAIN_TITLE_JSON_PTR).textValue();
    }

    public URI getPublicationContextUri() {
        return URI.create(root.at(SERIES_ID_JSON_PTR).textValue());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(root);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexDocument)) {
            return false;
        }
        IndexDocument that = (IndexDocument) o;
        return Objects.equals(root, that.root);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    /**
     * JsonString.
     *
     * @return JsonString
     */
    @Override
    @JacocoGenerated
    public String toJsonString() {
        try {
            return objectMapper.writeValueAsString(addContext(root));
        } catch (JsonProcessingException e) {
            logger.error(PROBLEM_SERIALIZING_MESSAGE, e);
            throw new RuntimeException(e);
        }
    }

    @JacocoGenerated
    private ObjectNode addContext(JsonNode root) {
        try {
            ObjectNode context = objectMapper.createObjectNode();
            context.put("@vocab", "https://bibsysdev.github.io/src/nva/ontology.ttl#");
            context.put("id", "@id");
            context.put("type", "@type");
            ObjectNode series = objectMapper.createObjectNode();
            series.put("@type", "@id");
            context.set("series", series);
            return ((ObjectNode) root).set("@context", context);
        } catch (Exception e) {
            logger.error(PROBLEM_SERIALIZING_MESSAGE, e);
            throw new RuntimeException(e);
        }
    }
}
