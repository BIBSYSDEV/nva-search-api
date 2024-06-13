package no.unit.nva.search2.importcandidate;

import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search2.common.constant.Words.CHAR_UNDERSCORE;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.SortKey;
import org.apache.commons.text.CaseUtils;

/**
 * @author Stig Norland
 */
public enum ImportCandidateSort implements SortKey {
    INVALID(EMPTY_STRING),
    RELEVANCE(Words.SCORE),
    COLLABORATION_TYPE(Constants.COLLABORATION_TYPE_KEYWORD),
    CREATED_DATE(Words.CREATED_DATE),
    INSTANCE_TYPE(Constants.TYPE_KEYWORD),
    PUBLICATION_YEAR(Constants.PUBLICATION_YEAR_KEYWORD),
    TITLE(Constants.MAIN_TITLE_KEYWORD),
    TYPE(Constants.TYPE_KEYWORD);

    private final String keyValidationRegEx;
    private final String path;
    private final String scriptValue;

    ImportCandidateSort(String jsonPath) {
        this.keyValidationRegEx = SortKey.getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.path = jsonPath;
        this.scriptValue = String.format("doc['%s'].value", jsonPath);
    }

    @Override
    public String asCamelCase() {
        return CaseUtils.toCamelCase(this.name(), false, CHAR_UNDERSCORE);
    }

    @Override
    public String asLowerCase() {
        return this.name().toLowerCase(Locale.getDefault());
    }

    @Override
    public String scriptValue() {
        return
            switch (this) {
                case RELEVANCE -> path;
                case CREATED_DATE -> scriptValue + ".getMillis()";
                default -> scriptValue;
            };
    }

    @Override
    public String keyPattern() {
        return keyValidationRegEx;
    }

    @Override
    public Stream<String> jsonPaths() {
        return Arrays.stream(path.split(PATTERN_IS_PIPE));
    }

    public static ImportCandidateSort fromSortKey(String keyName) {
        var result = Arrays.stream(ImportCandidateSort.values())
            .filter(SortKey.equalTo(keyName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Collection<String> validSortKeys() {
        return
            Arrays.stream(ImportCandidateSort.values())
                .sorted(SortKey::compareAscending)
                .skip(1)    // skip INVALID
                .map(SortKey::asLowerCase)
            .toList();
    }
}
