package no.unit.nva.search.service.resource.response;

import java.util.Set;

public record Contributor(
        Set<Affiliation> affiliations,
        boolean correspondingAuthor,
        Identity identity,
        String role,
        int sequence) {}
