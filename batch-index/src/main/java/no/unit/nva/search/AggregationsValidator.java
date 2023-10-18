package no.unit.nva.search;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

public class AggregationsValidator {

    public static final String ENTITY_DESCRIPTION_POINTER = "/entityDescription";
    public static final String REFERENCE_POINTER = "/entityDescription/reference";
    public static final String PUBLICATION_CONTEXT_POINTER = "/entityDescription/reference/publicationContext";
    public static final String PUBLICATION_INSTANCE_POINTER = "/entityDescription/reference/publicationInstance";
    public static final String DELIMITER = ", ";
    public static final String IDENTIFIER_POINTER = "/identifier";
    public static final String REPORT_TEMPLATE = "Document %s has missing fields %s";
    private final List<String> report;
    private final JsonNode document;

    public AggregationsValidator(JsonNode document) {
        this.document = document;
        this.report = new ArrayList<>();
    }

    public String getReport() {
        return String.format(REPORT_TEMPLATE,
                             document.at(IDENTIFIER_POINTER).textValue(),
                             String.join(DELIMITER, report));
    }

    public boolean isValid() {
        List.of(ENTITY_DESCRIPTION_POINTER, REFERENCE_POINTER, PUBLICATION_CONTEXT_POINTER,
                PUBLICATION_INSTANCE_POINTER).forEach(this::validateNode);
        return report.isEmpty();
    }

    private static boolean isNotValidNode(JsonNode node) {
        return !node.isObject();
    }

    private void validateNode(String value) {
        if (isNotValidNode(document.at(value))) {
            report.add(value);
        }
    }
}
