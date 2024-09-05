package no.unit.nva.search.endpoint.model;

import static no.unit.nva.search.common.constant.Words.CHAR_UNDERSCORE;

import org.apache.commons.text.CaseUtils;

import java.util.Locale;

public interface ILabel {
    String name();

    String asCamelCase();

    String asLowerCase();

    static String asCamelCase(String name) {
        return CaseUtils.toCamelCase(name, false, CHAR_UNDERSCORE);
    }

    static String asLowerCase(String name) {
        return name.toLowerCase(Locale.getDefault());
    }
}
