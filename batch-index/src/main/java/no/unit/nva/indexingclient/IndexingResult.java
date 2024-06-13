package no.unit.nva.indexingclient;

import java.util.List;

public interface IndexingResult<T> {

    List<T> getFailedResults();

    String getNextStartMarker();

    boolean isTruncated();


}
