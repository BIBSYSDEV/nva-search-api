package no.unit.nva.search2.model;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ADD_SLASH;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SPECIAL_CHARACTERS;
import java.util.Collection;
import java.util.function.Predicate;

public interface ParameterKey {

    String fieldName();

    Float fieldBoost();

    ParamKind fieldType();

    String fieldPattern();

    String valuePattern();

    ValueEncoding valueEncoding();

    Collection<String> searchFields();

    FieldOperator searchOperator();

    String errorMessage();

    static Predicate<ParameterKey> equalTo(String name) {
        return key -> name.matches(key.fieldPattern());
    }

    static String escapeSearchString(String value) {
        return value.replaceAll(PATTERN_IS_SPECIAL_CHARACTERS, PATTERN_IS_ADD_SLASH);
    }

    enum ValueEncoding {
        NONE, DECODE
    }

    enum ParamKind {
        DATE, NUMBER, STRING, SORT_STRING, CUSTOM
    }

    enum FieldOperator {
        MUST(""),
        MUST_NOT("NOT"),
        SHOULD("SHOULD"),
        GREATER_THAN_OR_EQUAL_TO("SINCE"),
        LESS_THAN("BEFORE");

        private final String keyPattern;

        FieldOperator(String pattern) {
            this.keyPattern = PATTERN_IS_IGNORE_CASE + PATTERN_IS_NONE_OR_ONE + pattern;
        }

        public String pattern() {
            return keyPattern;
        }
    }
}