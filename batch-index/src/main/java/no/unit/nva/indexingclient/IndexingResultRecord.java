package no.unit.nva.indexingclient;

import java.util.List;

public record IndexingResultRecord<T>(
    List<T> failedResults, String nextStartMarker, boolean truncated)
    implements IndexingResult<T> {}
