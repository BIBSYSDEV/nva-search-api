package no.unit.nva.search2.importcandidate;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.search2.constant.Words;

enum SortParameter {
    INVALID(EMPTY_STRING),
    COLLABORATION_TYPE(Constants.COLLABORATION_TYPE_KEYWORD),
    CREATED_DATE(Words.CREATED_DATE),
    INSTANCE_TYPE(Constants.INSTANCE_TYPE_KEYWORD),
    PUBLICATION_YEAR(Constants.PUBLICATION_YEAR_KEYWORD),
    TITLE(Constants.MAIN_TITLE_KEYWORD),
    TYPE(Constants.TYPE_KEYWORD);

    public static final Set<SortParameter> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(SortParameter.values())
            .sorted(SortParameter::compareAscending)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String keyValidationRegEx;
    private final String fieldName;

    SortParameter(String fieldName) {
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.fieldName = fieldName;
    }

    public String getKeyPattern() {
        return keyValidationRegEx;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static SortParameter fromSortKey(String keyName) {
        var result = Arrays.stream(SortParameter.values())
            .filter(SortParameter.equalTo(keyName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<SortParameter> equalTo(String name) {
        return key -> name.matches(key.getKeyPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(SortParameter::name)
            .map(String::toLowerCase)
            .toList();
    }

    private static int compareAscending(SortParameter key1, SortParameter key2) {
        return key1.ordinal() - key2.ordinal();
    }

    private static String getIgnoreCaseAndUnderscoreKeyExpression(String keyName) {
        var keyNameIgnoreUnderscoreExpression =
            keyName.toLowerCase(Locale.getDefault())
                .replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        return "%s%s".formatted(PATTERN_IS_IGNORE_CASE, keyNameIgnoreUnderscoreExpression);
    }
}
