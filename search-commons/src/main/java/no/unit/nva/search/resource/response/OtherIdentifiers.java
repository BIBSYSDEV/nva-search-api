package no.unit.nva.search.resource.response;

import java.util.Set;

public record OtherIdentifiers(
    Set<String> scopus,
    Set<String> cristin,
    Set<String> handle,
    Set<String> issn,
    Set<String> isbn) {}
