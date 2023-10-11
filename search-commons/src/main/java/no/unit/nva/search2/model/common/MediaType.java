package no.unit.nva.search2.model.common;

public enum MediaType {
    JSON("application/json"),
    JSONLD("application/ld+json"),
    CSV("text/csv");

    final String asString;

    MediaType(String mediaType) {
        this.asString = mediaType;
    }

    @Override
    public String toString() {
        return asString;
    }
}
