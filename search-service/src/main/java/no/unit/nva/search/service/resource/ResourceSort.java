package no.unit.nva.search.service.resource;

import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search.model.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search.model.constant.Words.DOT;
import static no.unit.nva.search.model.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.model.constant.Words.KEYWORD;
import static no.unit.nva.search.model.constant.Words.YEAR;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import no.unit.nva.search.model.constant.Words;
import no.unit.nva.search.model.enums.SortKey;

import org.apache.commons.text.CaseUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum for sorting resources.
 *
 * @author Stig Norland
 */
public enum ResourceSort implements SortKey {
    INVALID(EMPTY_STRING),
    RELEVANCE(Words.SCORE),
    CATEGORY(Constants.PUBLICATION_INSTANCE_TYPE),
    INSTANCE_TYPE(Constants.PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(Words.CREATED_DATE),
    MODIFIED_DATE(Words.MODIFIED_DATE),
    PUBLISHED_DATE(Words.PUBLISHED_DATE),
    PUBLICATION_DATE(
            Words.ENTITY_DESCRIPTION + Words.DOT + Words.PUBLICATION_DATE + Words.DOT + Words.YEAR + Words.DOT + Words.KEYWORD),
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

    public static ResourceSort fromSortKey(String keyName) {
        var result =
                Arrays.stream(values())
                        .filter(SortKey.equalTo(keyName))
                        .collect(Collectors.toSet());
        return result.size() == 1 ? result.stream().findFirst().get() : INVALID;
    }

    public static Collection<String> validSortKeys() {
        return Arrays.stream(values())
                .sorted(SortKey::compareAscending)
                .skip(1)
                .map(SortKey::asLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String asCamelCase() {
        return CaseUtils.toCamelCase(this.name(), false, Words.CHAR_UNDERSCORE);
    }

    @Override
    public String asLowerCase() {
        return this.name().toLowerCase(Locale.getDefault());
    }

    @Override
    public String keyPattern() {
        return keyValidationRegEx;
    }

    @Override
    public Stream<String> jsonPaths() {
        return Arrays.stream(path.split(PATTERN_IS_PIPE));
    }
}
