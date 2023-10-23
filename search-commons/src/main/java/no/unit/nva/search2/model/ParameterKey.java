package no.unit.nva.search2.model;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ADD_SLASH;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SPECIAL_CHARACTERS;
import java.util.Collection;
import java.util.function.Predicate;

public interface ParameterKey {

    Operator operator();

    String key();

    Collection<String> swsKey();

    String pattern();

    String keyPattern();

    String errorMessage();

    KeyEncoding encoding();

    ParamKind kind();

    static Predicate<ParameterKey> equalTo(String name) {
        return key -> name.matches(key.keyPattern());
    }

    static String escapeSearchString(String value) {
        return value.replaceAll(PATTERN_IS_SPECIAL_CHARACTERS, PATTERN_IS_ADD_SLASH);
    }

    enum KeyEncoding {
        NONE, DECODE
    }

    enum ParamKind {
        DATE, DATE_STRING, NUMBER, STRING, SORT_STRING, CUSTOM
    }

    enum Operator {
        NONE("%s%s"),
        EQUALS("(%s:%s)"),
        GREATER_THAN("(%s:>%s)"),
        GREATER_THAN_OR_EQUAL_TO("(%s:>=%s)"),
        LESS_THAN("(%s:<%s)"),
        LESS_THAN_OR_EQUAL_TO("(%s:<=%s)"),
        ;

        private final String format;

        Operator(String format) {
            this.format = format;
        }

        public String format() {
            return format;
        }
    }
}