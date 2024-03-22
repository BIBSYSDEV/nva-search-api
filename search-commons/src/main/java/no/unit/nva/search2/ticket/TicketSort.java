package no.unit.nva.search2.ticket;

import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.TYPE_KEYWORD;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.constant.Words;
import org.apache.commons.text.CaseUtils;

public enum TicketSort {
    INVALID(EMPTY_STRING),
    CREATED_DATE(Words.CREATED_DATE),
    MODIFIED_DATE(Words.MODIFIED_DATE),
    STATUS(STATUS_KEYWORD),
    TYPE(TYPE_KEYWORD);

    public static final Set<TicketSort> VALID_SORT_PARAMETER_KEYS =
        Arrays.stream(TicketSort.values())
            .sorted(TicketSort::compareAscending)
            .skip(1)    // skip INVALID
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String keyValidationRegEx;
    private final String path;
    TicketSort(String jsonPath) {
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
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

    public String jsonPath() {
        return path;
    }

    public static TicketSort fromSortKey(String keyName) {
        var result = Arrays.stream(TicketSort.values())
            .filter(TicketSort.equalTo(keyName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    public static Predicate<TicketSort> equalTo(String name) {
        return key -> name.matches(key.keyPattern());
    }

    public static Collection<String> validSortKeys() {
        return VALID_SORT_PARAMETER_KEYS.stream()
            .map(TicketSort::asLowerCase)
            .toList();
    }

    private static int compareAscending(TicketSort key1, TicketSort key2) {
        return key1.ordinal() - key2.ordinal();
    }

    private static String getIgnoreCaseAndUnderscoreKeyExpression(String keyName) {
        var keyNameIgnoreUnderscoreExpression =
            keyName.toLowerCase(Locale.getDefault())
                .replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        return "%s%s".formatted(PATTERN_IS_IGNORE_CASE, keyNameIgnoreUnderscoreExpression);
    }
}
