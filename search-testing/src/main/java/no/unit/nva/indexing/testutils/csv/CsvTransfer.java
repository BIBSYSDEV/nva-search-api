package no.unit.nva.indexing.testutils.csv;


import no.unit.nva.search2.common.csv.ExportCsv;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class CsvTransfer {

    private List<ExportCsv> csvList;

    public CsvTransfer() {
    }

    public void setCsvList(List<ExportCsv> csvList) {
        this.csvList = csvList;
    }

    public List<ExportCsv> getCsvList() {
        if (nonNull(csvList)) {
            return csvList;
        }
        return new ArrayList<>();
    }
}

