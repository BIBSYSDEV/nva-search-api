package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record OtherIdentifiers(
        Set<String> scopus,
        Set<String> cristin,
        Set<String> handle,
        Set<String> issn,
        Set<String> isbn) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> scopus = new ArrayList<>();
        private final List<String> cristin = new ArrayList<>();
        private final List<String> handle = new ArrayList<>();
        private final List<String> issn = new ArrayList<>();
        private final List<String> isbn = new ArrayList<>();

        private Builder() {}

        public Builder addScopus(String value) {
            scopus.add(value);
            return this;
        }

        public Builder addCristin(String value) {
            cristin.add(value);
            return this;
        }

        public Builder addHandle(String value) {
            handle.add(value);
            return this;
        }

        public Builder addIssn(String value) {
            issn.add(value);
            return this;
        }

        public Builder addIsbn(String value) {
            isbn.add(value);
            return this;
        }

        public Builder addIsbns(JsonNode node) {
            if (node.isArray()) {
                node.elements().forEachRemaining(element -> addIsbn(element.asText()));
            }
            return this;
        }

        public OtherIdentifiers build() {
            return new OtherIdentifiers(
                    Set.copyOf(scopus),
                    Set.copyOf(cristin),
                    Set.copyOf(handle),
                    Set.copyOf(issn),
                    Set.copyOf(isbn));
        }
    }
}
