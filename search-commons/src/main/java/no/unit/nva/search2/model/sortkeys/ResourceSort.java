package no.unit.nva.search2.model.sortkeys;

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

public enum ResourceSort {
    INVALID(""),
    CATEGORY("entityDescription.reference.publicationInstance.type.keyword"),
    CREATED_DATE("createdDate"),
    MODIFIED_DATE("modifiedDate"),
    PUBLISHED_DATE("publishedDate"),
    TITLE("entityDescription.mainTitle.keyword"),
    UNIT_ID("entityDescription.contributors.affiliations.id.keyword"),
    USER("(?i)(user)|(owner)", "resourceOwner.owner.keyword");

    public static final Set<ResourceSort> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(ResourceSort.values())
            .sorted(ResourceSort::compareAscending)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String keyValidationRegEx;
    private final String fieldName;

    ResourceSort(String pattern, String fieldName) {
        this.keyValidationRegEx = pattern;
        this.fieldName = fieldName;
    }

    ResourceSort(String fieldName) {
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.fieldName = fieldName;
    }

    public String getKeyPattern() {
        return keyValidationRegEx;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static ResourceSort fromSortKey(String keyName) {
        var result = Arrays.stream(ResourceSort.values())
            .filter(ResourceSort.equalTo(keyName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<ResourceSort> equalTo(String name) {
        return key -> name.matches(key.getKeyPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(ResourceSort::name)
            .map(String::toLowerCase)
            .toList();
    }

    private static int compareAscending(ResourceSort key1, ResourceSort key2) {
        return key1.ordinal() - key2.ordinal();
    }

    private static String getIgnoreCaseAndUnderscoreKeyExpression(String keyName) {
        var keyNameIgnoreUnderscoreExpression =
            keyName.toLowerCase(Locale.getDefault())
                .replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        return "%s%s".formatted(PATTERN_IS_IGNORE_CASE, keyNameIgnoreUnderscoreExpression);
    }
}
