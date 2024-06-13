package no.unit.nva.search.common.csv;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Rurik Greenall
 * @author Aasarød
 * @author Eyob Teweldemedhin
 * @author Sondre Vestad
 */
public final class ResourceCsvTransformer {

    private static final String IDENTITY_NAME_JSON_POINTER = "/identity/name";
    private static final String IDENTITY_NAME_SOURCE_POINTER = "entityDescription.contributors.identity.name";
    private static final String CONTRIBUTORS_JSON_POINTER = "/entityDescription/contributors";
    private static final String ID_JSON_POINTER = "/id";
    private static final String ID_SOURCE_POINTER = "id";
    private static final String MAIN_TITLE_JSON_POINTER = "/entityDescription/mainTitle";
    private static final String MAIN_TITLE_SOURCE_POINTER = "entityDescription.mainTitle";
    private static final String PUBLICATION_DATE_YEAR_JSON_POINTER = "/entityDescription/publicationDate/year";
    private static final String PUBLICATION_DATE_YEAR_SOURCE_POINTER = "entityDescription.publicationDate.year";
    private static final String PUBLICATION_DATE_MONTH_JSON_POINTER = "/entityDescription/publicationDate/month";
    private static final String PUBLICATION_DATE_MONTH_SOURCE_POINTER = "entityDescription.publicationDate.month";
    private static final String PUBLICATION_DATE_DAY_JSON_POINTER = "/entityDescription/publicationDate/day";
    private static final String PUBLICATION_DATE_DAY_SOURCE_POINTER = "entityDescription.publicationDate.day";
    private static final String PUBLICATION_INSTANCE_TYPE_JSON_POINTER
        = "/entityDescription/reference/publicationInstance/type";
    private static final String PUBLICATION_INSTANCE_TYPE_SOURCE_POINTER
        = "entityDescription.reference.publicationInstance.type";
    private static final String EMPTY_STRING = "";
    private static final char UTF8_BOM = '\ufeff';
    private static final char QUOTE_CHAR = '"';
    private static final char SEPARATOR = ';';
    private static final String LINE_END = "\r\n";

    private ResourceCsvTransformer() {
    }

    public static String transform(List<JsonNode> hits) {
        var stringWriter = new StringWriter();
        stringWriter.append(UTF8_BOM);
        var lines = extractedJsonSearchResults(hits);
        var csvWriter = new StatefulBeanToCsvBuilder<ExportCsv>(stringWriter)
                            .withApplyQuotesToAll(true)
                            .withQuotechar(QUOTE_CHAR)
                            .withSeparator(SEPARATOR)
                            .withLineEnd(LINE_END)
                            .withMappingStrategy(new HeaderColumnNameAndOrderMappingStrategy<>(ExportCsv.class))
                            .build();
        try {
            csvWriter.write(lines);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new RuntimeException(e);
        }
        return stringWriter.toString();
    }

    public static List<String> getJsonFields() {
        return List.of(
            ID_SOURCE_POINTER,
            MAIN_TITLE_SOURCE_POINTER,
            PUBLICATION_DATE_YEAR_SOURCE_POINTER,
            PUBLICATION_DATE_MONTH_SOURCE_POINTER,
            PUBLICATION_DATE_DAY_SOURCE_POINTER,
            PUBLICATION_INSTANCE_TYPE_SOURCE_POINTER,
            IDENTITY_NAME_SOURCE_POINTER
        );
    }

    private static List<ExportCsv> extractedJsonSearchResults(List<JsonNode> searchResults) {
        return searchResults.stream().map(ResourceCsvTransformer::createLine).collect(Collectors.toList());
    }

    private static ExportCsv createLine(JsonNode searchResult) {
        var mainTitle = getMainTitle(searchResult);
        var year = extractYear(searchResult);
        var month = extractMonth(searchResult);
        var day = extractDay(searchResult);
        var publicationInstance = extractPublicationInstance(searchResult);
        var contributors = getContributorsName(searchResult);
        return new ExportCsv(
            extractId(searchResult),
            mainTitle, year, month, day, publicationInstance, contributors);
    }

    private static String extractPublicationInstance(JsonNode searchResult) {
        return extractText(searchResult, PUBLICATION_INSTANCE_TYPE_JSON_POINTER, EMPTY_STRING);
    }

    private static String extractDay(JsonNode searchResult) {
        return extractText(searchResult, PUBLICATION_DATE_DAY_JSON_POINTER, EMPTY_STRING);
    }

    private static String extractMonth(JsonNode searchResult) {
        return extractText(searchResult, PUBLICATION_DATE_MONTH_JSON_POINTER, EMPTY_STRING);
    }

    private static String extractYear(JsonNode searchResult) {
        return extractText(searchResult, PUBLICATION_DATE_YEAR_JSON_POINTER, EMPTY_STRING);
    }

    private static String getMainTitle(JsonNode searchResult) {
        return extractText(searchResult, MAIN_TITLE_JSON_POINTER, EMPTY_STRING);
    }

    private static String extractId(JsonNode searchResult) {
        return extractText(searchResult, ID_JSON_POINTER, EMPTY_STRING);
    }

    private static List<String> getContributorsName(JsonNode document) {
        var contributors = document.at(CONTRIBUTORS_JSON_POINTER);
        return StreamSupport.stream(contributors.spliterator(), false)
                   .map(ResourceCsvTransformer::extractName)
                   .collect(Collectors.toList());
    }

    private static String extractName(JsonNode contributor) {
        return extractText(contributor, IDENTITY_NAME_JSON_POINTER, null);
    }

    private static String extractText(JsonNode node, String pointer, String defaultValue) {
        var value = node.at(pointer);
        return !value.isMissingNode() ? value.asText() : defaultValue;
    }
}