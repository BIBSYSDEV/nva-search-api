package no.unit.nva.search.service.resource.response;

import java.net.URI;

public record PublishingDetails(URI id, String type, Series series, String name, URI doi) {}
