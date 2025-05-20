package no.sikt.nva.oai.pmh.handler.oaipmh;

public record OaiPmhContext(
    String verb, String from, String until, String metadataPrefix, String resumptionToken) {}
