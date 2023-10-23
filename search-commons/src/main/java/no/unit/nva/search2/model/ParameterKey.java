package no.unit.nva.search2.model;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ADD_SLASH;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SPECIAL_CHARACTERS;
import java.util.Collection;
import java.util.function.Predicate;

public interface ParameterKey {

    String fieldName();

    ParamKind fieldKind();

    String fieldPattern();

    String valuePattern();

    KeyEncoding valueEncoding();

    Collection<String> searchFields();

    FieldOperator searchOperator();

    String errorMessage();

    static Predicate<ParameterKey> equalTo(String name) {
        return key -> name.matches(key.fieldPattern());
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

    enum FieldOperator {
        NONE, EQUALS, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESS_THAN_OR_EQUAL_TO
    }
}