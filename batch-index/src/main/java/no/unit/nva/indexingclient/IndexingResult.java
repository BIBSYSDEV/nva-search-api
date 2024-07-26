package no.unit.nva.indexingclient;

import java.util.List;

public interface IndexingResult<T> {

    List<T> failedResults();

    String nextStartMarker();

    boolean truncated();


}
