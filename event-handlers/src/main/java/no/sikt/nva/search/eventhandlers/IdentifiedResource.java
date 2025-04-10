package no.sikt.nva.search.eventhandlers;

import java.net.URI;

public record IdentifiedResource(String type, URI id, String identifier) {}
