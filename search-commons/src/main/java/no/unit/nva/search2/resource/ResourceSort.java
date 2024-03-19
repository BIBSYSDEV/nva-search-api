package no.unit.nva.search2.resource;

import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.common.constant.Words.YEAR;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.constant.Words;

public enum ResourceSort {
    INVALID(EMPTY_STRING),
    CATEGORY(Constants.PUBLICATION_INSTANCE_TYPE),
    INSTANCE_TYPE(Constants.PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(Words.CREATED_DATE),
    MODIFIED_DATE(Words.MODIFIED_DATE),
    PUBLISHED_DATE(Words.PUBLISHED_DATE),
    PUBLICATION_DATE(ENTITY_DESCRIPTION + DOT + Words.PUBLICATION_DATE + DOT + YEAR + DOT + KEYWORD),
    TITLE(Constants.ENTITY_DESCRIPTION_MAIN_TITLE_KEYWORD),
    UNIT_ID(Constants.CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    USER("(?i)(user)|(owner)", Constants.RESOURCE_OWNER_OWNER_KEYWORD);

    public static final Set<ResourceSort> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(ResourceSort.values())
            .sorted(ResourceSort::compareAscending)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String keyValidationRegEx;
    private final String name;

    ResourceSort(String pattern, String fieldName) {
        this.keyValidationRegEx = pattern;
        this.name = fieldName;
    }

    ResourceSort(String fieldName) {
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.name = fieldName;
    }

    public String keyPattern() {
        return keyValidationRegEx;
    }

    public String fieldName() {
        return name;
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
        return key -> name.matches(key.keyPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(ResourceSort::fieldName)
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
