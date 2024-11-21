package no.unit.nva.search.resource.response;

public record Contributor(
        Affiliation affiliation,
        boolean correspondingAuthor,
        Identity identity,
        String role,
        int sequence,
        String type) {}
