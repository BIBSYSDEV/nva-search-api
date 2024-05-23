package no.unit.nva.indexing.testutils.csv;

import com.opencsv.bean.CsvToBeanBuilder;
import no.unit.nva.search2.common.csv.ExportCsv;

import java.io.StringReader;
import java.util.List;

public class CsvUtil {
    private static final char UTF8_BOM = '\ufeff';

    public static final char SEPARATOR = ';';
    public static final char QUOTE_CHAR = '"';

    public static List<ExportCsv> toExportCsv(String csvContent) {
        var content = removeUtfBomIfPresent(csvContent);
        var csvTransfer = new CsvTransfer();

        try (var reader = new StringReader(content)) {
            var csvCsvToBeanBuilder = new CsvToBeanBuilder<ExportCsv>(reader)
                                          .withType(ExportCsv.class)
                                          .withSeparator(SEPARATOR)
                                          .withQuoteChar(QUOTE_CHAR)
                                          .build();

            csvTransfer.setCsvList(csvCsvToBeanBuilder.parse());
            return csvTransfer.getCsvList();
        }
    }

    private static String removeUtfBomIfPresent(String csvContent) {
        if (csvContent != null && csvContent.length() > 0 && csvContent.charAt(0) == UTF8_BOM) {
            return csvContent.substring(1);
        }
        return csvContent;
    }
}
