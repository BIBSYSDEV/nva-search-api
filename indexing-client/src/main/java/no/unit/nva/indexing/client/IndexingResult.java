package no.unit.nva.indexing.client;

import java.util.List;

public interface IndexingResult<T> {

    List<T> failedResults();

    String nextStartMarker();

    boolean truncated();
}
