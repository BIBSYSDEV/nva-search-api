package no.unit.nva.search2.common.csv;

import com.opencsv.bean.CsvBindByName;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

import java.util.List;
import java.util.Objects;

import static no.unit.nva.search2.common.constant.Words.COMMA;

@CsvBindByNameOrder({"url", "title", "publicationDate", "type", "contributors"})
public record ExportCsv(
    @CsvBindByName(column = "url")
    String id,
    @CsvBindByName(column = "title")
    String mainTitle,
    @CsvBindByName(column = "publicationDate")
    String publicationDate,
    @CsvBindByName(column = "type")
    String publicationInstance,
    @CsvBindByName(column = "contributors")
    String contributors
) {
    public static final String DATE_SEPARATOR = "-";

    public ExportCsv(String id,
                     String mainTitle,
                     String year,
                     String month,
                     String day,
                     String publicationInstance,
                     List<String> contributors) {
        this(id, mainTitle, createPublicationDate(year, month, day),
            publicationInstance, createContributorsString(contributors));
    }

    static String createPublicationDate(String year, String month, String day) {
        var stringBuilder = new StringBuilder();

        if (StringUtils.isNotBlank(year)) {
            stringBuilder.append(year);
        }

        if (StringUtils.isNotBlank(month) && StringUtils.isNotBlank(year)) {
            stringBuilder.append(DATE_SEPARATOR).append(month);
        }

        if (StringUtils.isNotBlank(day) && StringUtils.isNotBlank(month) && StringUtils.isNotBlank(year)) {
            stringBuilder.append(DATE_SEPARATOR).append(day);
        }
        return stringBuilder.toString();
    }

    static String createContributorsString(List<String> contributors) {
        return String.join(COMMA, contributors);
    }

}
