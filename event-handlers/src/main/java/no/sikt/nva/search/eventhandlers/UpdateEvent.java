package no.sikt.nva.search.eventhandlers;

public record UpdateEvent(String action, IdentifiedEntity oldData, IdentifiedEntity newData) {}
