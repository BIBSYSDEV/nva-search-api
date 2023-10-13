package no.unit.nva.search2.model;

import static no.unit.nva.search2.constant.ApplicationConstants.UNDERSCORE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
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
            .sorted(ResourceSortKeys::compareAscending)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String keyValidationRegEx;
    private final String fieldName;

    ResourceSortKeys(String pattern, String fieldName) {
        this.keyValidationRegEx = pattern;
        this.fieldName = fieldName;
    }

    ResourceSortKeys(String fieldName) {
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.fieldName = fieldName;
    }


    public String getKeyPattern() {
        return keyValidationRegEx;
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
        return key -> name.matches(key.getKeyPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(ResourceSortKeys::name)
            .map(String::toLowerCase)
            .toList();
    }

    private static int compareAscending(ResourceSortKeys key1, ResourceSortKeys key2) {
        return key1.ordinal() - key2.ordinal();
    }

    private static String getIgnoreCaseAndUnderscoreKeyExpression(String keyName) {
        var keyNameIgnoreUnderscoreExpression =
            keyName.toLowerCase(Locale.getDefault())
                .replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        return "%s%s".formatted(PATTERN_IS_IGNORE_CASE, keyNameIgnoreUnderscoreExpression);
    }
}
