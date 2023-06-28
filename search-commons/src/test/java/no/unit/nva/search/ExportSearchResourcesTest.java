package no.unit.nva.search;


import no.unit.nva.indexing.testutils.FakeSearchResponse;
import no.unit.nva.indexing.testutils.csv.CsvUtil;
import no.unit.nva.search.models.SearchResponseDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class ExportSearchResourcesTest {

    private static final String COMMA_DELIMITER = ", ";

    @Test
    void shouldAllowCreationOfCsv() throws IOException {
        var expected = List.of(csvWithYearOnly(), csvWithYearAndMonthOnly(), csvWithFullDate());
        var json = FakeSearchResponse.generateSearchResponseString(expected);
        var searchResponse = SearchResponseDto.fromSearchResponse(getSearchResponseFromJson(json), randomUri());

        var value = ExportSearchResources.exportSearchResults(searchResponse);
        var actual = CsvUtil.toExportCsv(value);
        assertThat(actual, is(equalTo(expected)));
    }



    private ExportCsv csvWithYearOnly() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022";

        var exportCsv = new ExportCsv();
        exportCsv.setId(id);
        exportCsv.setMainTitle(title);
        exportCsv.setPublicationInstance(type);
        exportCsv.setPublicationDate(date);
        exportCsv.setContributors(String.join(COMMA_DELIMITER, contributors));
        return exportCsv;
    }

    private ExportCsv csvWithYearAndMonthOnly() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022-01";

        var exportCsv = new ExportCsv();
        exportCsv.setId(id);
        exportCsv.setMainTitle(title);
        exportCsv.setPublicationInstance(type);
        exportCsv.setPublicationDate(date);
        exportCsv.setContributors(String.join(COMMA_DELIMITER, contributors));
        return exportCsv;
    }

    private static ExportCsv csvWithFullDate() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022-01-22";

        var exportCsv = new ExportCsv();
        exportCsv.setId(id);
        exportCsv.setMainTitle(title);
        exportCsv.setPublicationInstance(type);
        exportCsv.setPublicationDate(date);
        exportCsv.setContributors(String.join(COMMA_DELIMITER, contributors));
        return exportCsv;
    }
}