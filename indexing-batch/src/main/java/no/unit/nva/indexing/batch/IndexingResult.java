package no.unit.nva.indexing.batch;

import java.util.List;

public interface IndexingResult<T> {

    List<T> failedResults();

    String nextStartMarker();

    boolean truncated();
}
