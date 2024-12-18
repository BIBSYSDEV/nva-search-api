package no.unit.nva.search.service.scroll;

import static no.unit.nva.search.model.constant.ErrorMessages.NOT_IMPLEMENTED_FOR;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.model.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search.model.constant.Words.COLON;
import static no.unit.nva.search.model.constant.Words.UNDERSCORE;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import no.unit.nva.search.model.enums.FieldOperator;
import no.unit.nva.search.model.enums.ParameterKey;
import no.unit.nva.search.model.enums.ParameterKind;
import no.unit.nva.search.model.enums.ValueEncoding;

import nva.commons.core.JacocoGenerated;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.CaseUtils;

import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to
 * implement these parameters <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin
 * API</a>
 *
 * @author Sondre Vestad
 */
public enum ScrollParameter implements ParameterKey<ScrollParameter> {
    INVALID(ParameterKind.INVALID);

    private final ParameterKind paramkind;

    ScrollParameter(ParameterKind kind) {
        this.paramkind = kind;
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
    public Float fieldBoost() {
        return 1F;
    }

    @Override
    public ParameterKind fieldType() {
        return paramkind;
    }

    @Override
    public String fieldPattern() {
        return PATTERN_IS_IGNORE_CASE + name().replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
    }

    @Override
    public String valuePattern() {
        return ParameterKey.getValuePattern(paramkind, null);
    }

    @Override
    public ValueEncoding valueEncoding() {
        return ParameterKey.getEncoding(paramkind);
    }

    @Override
    public Stream<String> searchFields(boolean... isKeyWord) {
        return Stream.of(EMPTY_STRING);
    }

    @Override
    public FieldOperator searchOperator() {
        return FieldOperator.NA;
    }

    @Override
    public String errorMessage() {
        return ParameterKey.getErrorMessage(paramkind);
    }

    @Override
    @JacocoGenerated
    public ScrollParameter subQuery() {
        throw new NotImplementedException(NOT_IMPLEMENTED_FOR + this.getClass().getName());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return new StringJoiner(COLON, "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(asCamelCase())
                .toString();
    }
}
