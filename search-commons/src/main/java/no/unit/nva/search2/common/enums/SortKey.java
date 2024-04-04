package no.unit.nva.search2.common.enums;

import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface SortKey {

    String name();

    String keyPattern();

    String jsonPath();

    Stream<String> jsonPaths();

    String asCamelCase();

    String asLowerCase();

    static Predicate<SortKey> equalTo(String name) {
        return key -> name.matches(key.keyPattern());
    }

    static int compareAscending(Enum<?> key1, Enum<?> key2) {
        return key1.ordinal() - key2.ordinal();
    }

    static String getIgnoreCaseAndUnderscoreKeyExpression(String keyName) {
        var keyNameIgnoreUnderscoreExpression =
            keyName.toLowerCase(Locale.getDefault())
                .replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        return "%s%s".formatted(PATTERN_IS_IGNORE_CASE, keyNameIgnoreUnderscoreExpression);
    }
}
