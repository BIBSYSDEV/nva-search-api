package no.unit.nva.search.service.resource.response;

import java.net.URI;

public record Identity(URI id, String name, URI orcId) {}
