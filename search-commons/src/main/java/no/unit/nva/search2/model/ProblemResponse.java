package no.unit.nva.search2.model;

public record ProblemResponse(
    String detail,
    Long status,
    String title,
    String instance,
    String requestId,
    String type) {

}
