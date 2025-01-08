package no.unit.nva.search.model.records;

import java.util.List;
import java.util.Map;

public record Aggregates(
    Aggregate license,
    List<Aggregate> contributors,
    Aggregate journal,
    Aggregate series,
    Aggregate publisher,
    List<Aggregate> files,
    Aggregate type,
    List<Aggregate> fundingSource,
    Aggregate scientificIndex,
    Aggregate topLevelOrganization) {

  public record Aggregate(String id, String key, Map<String, String> labels) {}
}
