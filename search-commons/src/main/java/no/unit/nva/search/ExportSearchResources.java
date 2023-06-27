package no.unit.nva.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.core.StringUtils;

public final class ExportSearchResources {

    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String CONTRIBUTORS = "contributors";
    public static final String NAME = "name";
    public static final String ID = "id";
    public static final String MAIN_TITLE = "mainTitle";
    public static final String IDENTITY = "identity";
    public static final String REFERENCE = "reference";
    public static final String PUBLICATION_INSTANCE = "publicationInstance";
    public static final String PUBLICATION_DATE = "publicationDate";
    public static final String TYPE = "type";
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";
    public static final String PUBLICATION_YEAR = "publicationYear";
    public static final String PUBLICATION_MONTH = "publicationMonth";
    public static final String PUBLICATION_DAY = "publicationDay";

    private ExportSearchResources() {
    }

    /**
     * Write search results to Text/CSV format.
     *
     * @param searchResponseDto output ffrom search results
     */
    public static String exportSearchResults(SearchResponseDto searchResponseDto) throws IOException {

        List<String[]> textData = createTextDataFromSearchResult(searchResponseDto.getHits());
        return toCsvFormat(textData);
    }

    public static List<String[]> createTextDataFromSearchResult(List<JsonNode> searchResults) {

        String[] header = {ID, MAIN_TITLE, PUBLICATION_YEAR, PUBLICATION_MONTH, PUBLICATION_DAY,
            PUBLICATION_INSTANCE, CONTRIBUTORS};

        List<String[]> createTextData = new ArrayList<>();
        createTextData.add(header);
        extractedJsonSearchResults(searchResults, createTextData);
        return createTextData;
    }

    private static String toCsvFormat(List<String[]> textData) throws IOException {
        StringWriter writer = new StringWriter();
        CSVWriter csvwriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
                                            CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        csvwriter.writeAll(textData);
        csvwriter.close();

        return writer.toString();
    }

    private static void extractedJsonSearchResults(List<JsonNode> searchResults, List<String[]> createTextData) {

        searchResults.forEach(searchResult -> {

            var contributors = searchResult.get(ENTITY_DESCRIPTION).get(CONTRIBUTORS);
            String contributorsName = getContributorsName(contributors);

            String[] textData = {
                getId(searchResult),
                getMainTitle(searchResult),
                getYear(searchResult),
                getMonth(searchResult),
                getDay(searchResult),
                getPublicationInstanceType(searchResult),
                contributorsName
            };
            createTextData.add(textData);
        });
    }

    private static String getId(JsonNode searchResult) {
        return searchResult.get(ID) != null ? searchResult.get(ID).toString() : StringUtils.EMPTY_STRING;
    }

    private static String getMainTitle(JsonNode searchResult) {
        return searchResult.get(ENTITY_DESCRIPTION).get(MAIN_TITLE) != null ? searchResult.get(ENTITY_DESCRIPTION)
            .get(MAIN_TITLE)
            .toString() : StringUtils.EMPTY_STRING;
    }

    private static String getYear(JsonNode searchResult) {
        return searchResult.get(ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(YEAR) != null ? searchResult.get(
            ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(YEAR).toString() : StringUtils.EMPTY_STRING;
    }

    private static String getMonth(JsonNode searchResult) {
        return searchResult.get(ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(MONTH) != null ? searchResult.get(
            ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(MONTH).toString() : StringUtils.EMPTY_STRING;
    }

    private static String getDay(JsonNode searchResult) {
        return searchResult.get(ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(DAY) != null ? searchResult.get(
            ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(DAY).toString() : StringUtils.EMPTY_STRING;
    }

    private static String getPublicationInstanceType(JsonNode searchResult) {
        return searchResult.get(ENTITY_DESCRIPTION).get(REFERENCE).get(PUBLICATION_INSTANCE).get(TYPE) != null
                   ? searchResult.get(ENTITY_DESCRIPTION).get(REFERENCE).get(PUBLICATION_INSTANCE).get(TYPE).toString()
                   : StringUtils.EMPTY_STRING;
    }

    private static String getContributorsName(JsonNode contributors) {
        ArrayList<String> extractContributors = new ArrayList<>();
        contributors.forEach(contributor -> {
            extractContributors.add(getContributor(contributor));
        });

        return extractContributors.stream()
            .map(String::valueOf)
            .map(s -> s.replace("\"", ""))
            .collect(Collectors.joining(",", "\"", "\""));
    }

    private static String getContributor(JsonNode contributor) {
        return contributor.get(IDENTITY).get(NAME) != null ? contributor.get(IDENTITY).get(NAME).toString()
                   : StringUtils.EMPTY_STRING;
    }
}