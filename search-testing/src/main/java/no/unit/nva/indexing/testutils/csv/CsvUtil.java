package no.unit.nva.indexing.testutils.csv;

import com.opencsv.bean.CsvToBeanBuilder;
import no.unit.nva.search.ExportCsv;

import java.io.StringReader;
import java.util.List;

public class CsvUtil {

    public static final char SEPARATOR = ';';
    public static final char QUOTE_CHAR = '"';

    public static List<ExportCsv> toExportCsv(String string) {
        var csvTransfer = new CsvTransfer();

        try (var reader = new StringReader(string)) {
            var csvCsvToBeanBuilder = new CsvToBeanBuilder<ExportCsv>(reader)
                                          .withType(ExportCsv.class)
                                          .withSeparator(SEPARATOR)
                                          .withQuoteChar(QUOTE_CHAR)
                                          .build();

            csvTransfer.setCsvList(csvCsvToBeanBuilder.parse());
            return csvTransfer.getCsvList();
        }
    }
}
