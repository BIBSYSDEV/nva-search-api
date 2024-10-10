package no.unit.nva.common;

import static no.unit.nva.search.model.constant.Words.AMPERSAND;
import static no.unit.nva.search.model.constant.Words.EQUAL;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;

import static java.util.Objects.nonNull;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;

public final class EntrySetTools {

    @JacocoGenerated
    public EntrySetTools() {}

    public static Collection<Entry<String, String>> queryToMapEntries(URI uri) {
        return queryToMapEntries(uri.getQuery());
    }

    public static Collection<Entry<String, String>> queryToMapEntries(String query) {
        return nonNull(query)
                ? Arrays.stream(query.split(AMPERSAND))
                        .map(keyValue -> keyValue.split(EQUAL))
                        .map(EntrySetTools::stringsToEntry)
                        .toList()
                : Collections.emptyList();
    }

    public static Entry<String, String> stringsToEntry(String... strings) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return strings[0];
            }

            @Override
            public String getValue() {
                return attempt(() -> strings[1]).orElse((f) -> EMPTY_STRING);
            }

            @Override
            @JacocoGenerated
            public String setValue(String value) {
                return null;
            }
        };
    }
}
