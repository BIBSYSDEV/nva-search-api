package no.unit.nva.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.search.models.SearchResponseDto;

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

    private ExportSearchResources() {
    }

    /**
     * Write search results to Text/CSV format.
     * @param searchResponseDto output ffrom search results
     */
    public static String exportSearchResults(SearchResponseDto searchResponseDto) throws IOException {

        List<String[]> textData = createTextDataFromSearchResult(searchResponseDto.getHits());
        StringWriter writer = new StringWriter();
        CSVWriter csvwriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
                                            CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        csvwriter.writeAll(textData);
        csvwriter.close();

        return writer.toString();
    }

    public static List<String[]> createTextDataFromSearchResult(List<JsonNode> searchResults) {

        String[] header = {"id", "mainTitle", "publicationYear", "publicationMonth", "publicationDay",
            "publicationInstance", "contributorNames"};

        List<String[]> createTextData = new ArrayList<>();
        createTextData.add(header);
        extractedJsonSearchResults(searchResults, createTextData);
        return createTextData;
    }

    private static void extractedJsonSearchResults(List<JsonNode> searchResults, List<String[]> createTextData) {

        searchResults.forEach(searchResult -> {

            var contributors = searchResult.get(ENTITY_DESCRIPTION).get(CONTRIBUTORS);
            String contributorsName = getContributorsName(contributors);

            String[] textData = {
                searchResult.get(ID).toString(),
                searchResult.get(ENTITY_DESCRIPTION).get(MAIN_TITLE).toString(),
                searchResult.get(ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(YEAR).toString(),
                searchResult.get(ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(MONTH).toString(),
                searchResult.get(ENTITY_DESCRIPTION).get(PUBLICATION_DATE).get(DAY).toString(),
                searchResult.get(ENTITY_DESCRIPTION).get(REFERENCE).get(PUBLICATION_INSTANCE).get(TYPE).toString(),
                contributorsName
            };
            createTextData.add(textData);
        });
    }

    private static String getContributorsName(JsonNode contributors) {
        ArrayList<String> extractContributors = new ArrayList<>();
        contributors.forEach(contributor -> {
            extractContributors.add(contributor.get(IDENTITY).get(NAME).toString());
        });

        return extractContributors.stream()
            .map(String::valueOf)
            .map(s -> s.replace("\"", ""))
            .collect(Collectors.joining(",", "\"", "\""));
    }
}