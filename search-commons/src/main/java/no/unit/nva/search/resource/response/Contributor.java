package no.unit.nva.search.resource.response;

import java.util.Set;

public record Contributor(
    Set<Affiliation> affiliations,
    boolean correspondingAuthor,
    Identity identity,
    String role,
    int sequence) {}
