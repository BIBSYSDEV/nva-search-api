package no.unit.nva.search2.model;

import static no.unit.nva.search2.constant.ApplicationConstants.UNDERSCORE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CASE_INSENSITIVE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum ResourceSortKeys {
    INVALID(""),
    CATEGORY("entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR_NAME("entityDescription.contributors.identity.name"),
    CREATED_DATE("createdDate"),
    INSTITUTION_NAME("entityDescription.contributors.affiliation.name"),
    MODIFIED_DATE("modifiedDate"),
    PUBLISHED_DATE("publishedDate"),
    TITLE("entityDescription.mainTitle"),
    UNIT_ID("entityDescription.contributors.affiliation.id"),
    USER("(?i)(user)|(owner)", "resourceOwner.owner");

    public static final Set<ResourceSortKeys> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(ResourceSortKeys.values())
            .sorted(ResourceSortKeys::compareParameterKey)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String pattern;
    private final String fieldName;

    ResourceSortKeys(String pattern, String fieldName) {
        this.pattern = pattern;
        this.fieldName = fieldName;
    }

    ResourceSortKeys(String fieldName) {
        var name = this.name().toLowerCase(Locale.getDefault());
        this.pattern =  PATTERN_IS_CASE_INSENSITIVE + name.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.fieldName = fieldName;
    }


    public String getPattern() {
        return pattern;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static ResourceSortKeys keyFromString(String paramName) {
        var result = Arrays.stream(ResourceSortKeys.values())
            .filter(ResourceSortKeys.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<ResourceSortKeys> equalTo(String name) {
        return key -> name.matches(key.getPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(ResourceSortKeys::name)
            .map(String::toLowerCase)
            .toList();
    }

    private static int compareParameterKey(ResourceSortKeys key1, ResourceSortKeys key2) {
        return key1.ordinal() - key2.ordinal();
    }
}
