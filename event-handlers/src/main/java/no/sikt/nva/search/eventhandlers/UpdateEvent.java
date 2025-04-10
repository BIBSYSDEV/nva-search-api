package no.sikt.nva.search.eventhandlers;

public record UpdateEvent(String action, IdentifiedResource oldData, IdentifiedResource newData) {}
