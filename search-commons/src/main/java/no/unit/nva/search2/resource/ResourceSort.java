package no.unit.nva.search2.resource;

import static no.unit.nva.search2.common.constant.Patterns.*;
import static no.unit.nva.search2.common.constant.Words.*;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.SortKey;
import org.apache.commons.text.CaseUtils;

public enum ResourceSort implements SortKey {
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

    private final String keyValidationRegEx;
    private final String path;

    ResourceSort(String pattern, String jsonPath) {
        this.keyValidationRegEx = pattern;
        this.path = jsonPath;
    }

    ResourceSort(String jsonPath) {
        this.keyValidationRegEx = SortKey.getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.path = jsonPath;
    }


    public String asCamelCase() {
        return CaseUtils.toCamelCase(this.name(), false, CHAR_UNDERSCORE);
    }

    public String asLowerCase() {
        return this.name().toLowerCase(Locale.getDefault());
    }

    public String keyPattern() {
        return keyValidationRegEx;
    }

    @Override
    public String jsonPath() {
        return path;
    }

    @Override
    public String[] jsonPaths() {
        return path.split(PATTERN_IS_PIPE);
    }

    public static SortKey fromSortKey(String keyName) {
        var result = Arrays.stream(ResourceSort.values())
            .filter(SortKey.equalTo(keyName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<ResourceSort> equalTo(String name) {
        return key -> name.matches(key.keyPattern());
    }

    public static Collection<String> validSortKeys() {
        return Arrays.stream(ResourceSort.values())
                .sorted(SortKey::compareAscending)
                .skip(1)
                .map(ResourceSort::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    public static String sortToJson(String sortParameters) {
        return
                Arrays.stream(sortParameters.split(COMMA))
                        .flatMap(param -> {
                            var split = param.split(COLON_OR_SPACE);
                            var direction = split.length == 2 ? split[1] : "asc";
                            return Arrays.stream(fromSortKey(split[0]).jsonPaths())
                                    .map(path -> "{\"" + path + "\":{ \"order\": \"" + direction + "\",\"missing\": \"_last\"}}");
                        })
                        .collect(Collectors.joining(COMMA, "[", "]"));
    }
}
