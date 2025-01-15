package no.unit.nva.search.resource.response;

import java.net.URI;

public record PublishingDetails(
    URI id,
    String type,
    String name,
    URI doi,
    ScientificRating series,
    ScientificRating publisher) {}
