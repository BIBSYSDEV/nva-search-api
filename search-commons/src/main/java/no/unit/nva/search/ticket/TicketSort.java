package no.unit.nva.search.ticket;

import static no.unit.nva.constants.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import no.unit.nva.constants.Words;
import no.unit.nva.search.common.enums.SortKey;

import org.apache.commons.text.CaseUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Stig Norland
 */
public enum TicketSort implements SortKey {
    INVALID(EMPTY_STRING),
    RELEVANCE(Words.SCORE),
    CREATED_DATE(Words.CREATED_DATE),
    MODIFIED_DATE(Words.MODIFIED_DATE),
    STATUS(STATUS_KEYWORD),
    TYPE(TYPE_KEYWORD);

    private final String keyValidationRegEx;
    private final String path;

    TicketSort(String jsonPath) {
        this.keyValidationRegEx = SortKey.getIgnoreCaseAndUnderscoreKeyExpression(this.name());
        this.path = jsonPath;
    }

    public static TicketSort fromSortKey(String keyName) {
        var result =
                Arrays.stream(TicketSort.values())
                        .filter(SortKey.equalTo(keyName))
                        .collect(Collectors.toSet());
        return result.size() == 1 ? result.stream().findFirst().get() : INVALID;
    }

    public static Collection<String> validSortKeys() {
        return Arrays.stream(TicketSort.values())
                .sorted(SortKey::compareAscending)
                .skip(1) // skip INVALID
                .map(TicketSort::asLowerCase)
                .toList();
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
    public String keyPattern() {
        return keyValidationRegEx;
    }

    @Override
    public Stream<String> jsonPaths() {
        return Arrays.stream(path.split(PATTERN_IS_PIPE));
    }
}
