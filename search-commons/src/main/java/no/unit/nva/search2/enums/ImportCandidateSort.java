package no.unit.nva.search2.enums;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.search2.constant.ImportCandidateFields;

public enum ImportCandidateSort {
    INVALID(""),
    COLLABORATION_TYPE(ImportCandidateFields.COLLABORATION_TYPE),
    CREATED_DATE(ImportCandidateFields.CREATED_DATE),
    INSTANCE_TYPE(ImportCandidateFields.INSTANCE_TYPE),
    PUBLICATION_YEAR(ImportCandidateFields.PUBLICATION_YEAR),
    TITLE(ImportCandidateFields.MAIN_TITLE),
    TYPE(ImportCandidateFields.TYPE);

    public static final Set<ImportCandidateSort> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(ImportCandidateSort.values())
            .sorted(ImportCandidateSort::compareAscending)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String keyValidationRegEx;
    private final String fieldName;

    ImportCandidateSort(String pattern, String fieldName) {
        this.keyValidationRegEx = pattern;
        this.fieldName = fieldName;
    }

    ImportCandidateSort(String fieldName) {
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.fieldName = fieldName;
    }

    public String getKeyPattern() {
        return keyValidationRegEx;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static ImportCandidateSort fromSortKey(String keyName) {
        var result = Arrays.stream(ImportCandidateSort.values())
            .filter(ImportCandidateSort.equalTo(keyName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<ImportCandidateSort> equalTo(String name) {
        return key -> name.matches(key.getKeyPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(ImportCandidateSort::name)
            .map(String::toLowerCase)
            .toList();
    }

    private static int compareAscending(ImportCandidateSort key1, ImportCandidateSort key2) {
        return key1.ordinal() - key2.ordinal();
    }

    private static String getIgnoreCaseAndUnderscoreKeyExpression(String keyName) {
        var keyNameIgnoreUnderscoreExpression =
            keyName.toLowerCase(Locale.getDefault())
                .replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        return "%s%s".formatted(PATTERN_IS_IGNORE_CASE, keyNameIgnoreUnderscoreExpression);
    }
}
