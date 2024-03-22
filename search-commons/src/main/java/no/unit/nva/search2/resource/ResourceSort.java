package no.unit.nva.search2.resource;

import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.YEAR;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.SortKey;

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

    @Override
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

    public static Set<String> validSortKeys() {
        return Arrays.stream(ResourceSort.values())
            .sorted(SortKey::compareAscending)
            .skip(1)
            .map(ResourceSort::name)
            .collect(Collectors.toCollection(LinkedHashSet::new));
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
