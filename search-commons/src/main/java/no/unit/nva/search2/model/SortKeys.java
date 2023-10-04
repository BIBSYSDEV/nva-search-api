package no.unit.nva.search2.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum SortKeys {
    INVALID("", ""),
    CATEGORY("(?i)category", "entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR_ID("(?i)contributor.?id", "entityDescription.contributors.identity.id"),
    CONTRIBUTOR_NAME("(?i)contributor.?name", "entityDescription.contributors.identity.name"),
    CREATED("(?i)created.?date", "created"),
    FUNDING("(?i)funding", "fundings.identifier|source.identifier"),
    INSTITUTION_ID("(?i)institution.?id", "entityDescription.contributors.affiliation.id"),
    INSTITUTION_NAME("(?i)institution.?name", "entityDescription.contributors.affiliation.name"),
    MODIFIED("(?i)modified.?date", "modified"),
    PUBLISHED_YEAR("(?i)published.?year", "entityDescription.publicationDate.year"),
    TITLE("(?i)title", "entityDescription.mainTitle"),
    UNIT_ID("(?i)unit.?id", "entityDescription.contributors.affiliation.id"),
    USER("(?i)(user)|(owner)", "resourceOwner.owner");

    public static final Set<SortKeys> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(SortKeys.values())
            .sorted(SortKeys::compareParameterKey)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String pattern;
    private final String luceneField;

    SortKeys(String pattern, String luceneField) {
        this.pattern = pattern;
        this.luceneField = luceneField;
    }

    public String getPattern() {
        return pattern;
    }

    public String getLuceneField() {
        return luceneField;
    }

    public static SortKeys keyFromString(String paramName) {
        var result = Arrays.stream(SortKeys.values())
            .filter(SortKeys.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<SortKeys> equalTo(String name) {
        return key -> name.matches(key.getPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(SortKeys::name)
            .map(String::toLowerCase)
            .toList();
    }

    private static int compareParameterKey(SortKeys key1, SortKeys key2) {
        return key1.ordinal() - key2.ordinal();
    }
}
