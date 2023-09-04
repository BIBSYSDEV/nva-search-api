package no.unit.nva.search2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.IParameterKey;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constants.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE;
import static no.unit.nva.search2.constants.Patterns.PATTERN_IS_BOOLEAN;
import static no.unit.nva.search2.constants.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constants.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constants.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.constants.Patterns.PATTERN_IS_RANGE;
import static no.unit.nva.search2.common.IParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.common.IParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.common.IParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.common.IParameterKey.ParamKind.STRING;
import static no.unit.nva.search2.constants.ErrorMessages.ERROR_MESSAGE_INVALID_DATE;
import static no.unit.nva.search2.constants.ErrorMessages.ERROR_MESSAGE_INVALID_FIELD_VALUE;
import static no.unit.nva.search2.constants.ErrorMessages.ERROR_MESSAGE_INVALID_NUMBER;
import static no.unit.nva.search2.constants.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
import static no.unit.nva.search2.constants.ErrorMessages.ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;

public enum ResourceParameter implements IParameterKey {
    INVALID(STRING,null),
    FIELDS(CUSTOM, "fields", null, Operator.EQUALS, "all", ERROR_MESSAGE_INVALID_FIELD_VALUE, KeyEncoding.NONE),
    PAGE(NUMBER,"page","from"),
    PER_PAGE(NUMBER,"per_page", "results"),
    SORT(STRING,"sort", "orderBy"),
    SORT_ORDER(CUSTOM,"order","sortOrder", Operator.EQUALS, "asc|desc", null, null),
    CATEGORY(STRING, "category","entityDescription.reference.publicationInstance"),
    CONTRIBUTOR(STRING, "contributor","entityDescription.contributors.identity.id"
                                      + "|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, "created_before"),
    CREATED_SINCE(DATE,"created_since"),
    DOI(CUSTOM,"doi"),
    FUNDING(STRING,"funding","fundings.identifier|source.identifier"),
    FUNDING_SOURCE(STRING,"funding_source","fundings.source.identifier"),
    ID(STRING,"id", "identifier"),
    INSTITUTION(STRING,"institution","entityDescription.contributors.affiliation.id"
                                     + "|entityDescription.contributors.affiliation.name"),
    ISSN(STRING,"issn","entityDescription.reference.publicationContext.onlineIssn"
                       + "|entityDescription.reference.publicationContext.printIssn"),
    MODIFIED_BEFORE(DATE,"modified_before"),
    MODIFIED_SINCE(DATE,"modified_since"),
    PROJECT_CODE(CUSTOM, "project_code", "fundings.identifier", Operator.EQUALS, ".", null, KeyEncoding.NONE),
    PUBLISHED_BEFORE(NUMBER,"published_before","entityDescription.publicationDate.year",Operator.LESS_THAN),
    PUBLISHED_SINCE(NUMBER,"published_since","entityDescription.publicationDate.year",
                    Operator.GREATER_THAN_OR_EQUAL_TO,null, null, KeyEncoding.NONE),
    TITLE(STRING,"title","entityDescription.mainTitle"),
    UNIT(STRING,"unit","entityDescription.contributors.affiliation.id"),
    USER(STRING,"user", "resourceOwner.owner"),
    YEAR_REPORTED(NUMBER,"year_reported","entityDescription.publicationDate.year"),
    SEARCH_ALL(STRING,"query", ""),
    LANG(STRING,"lang");

    public static final int IGNORE_PATH_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameter> VALID_LUCENE_PARAMETERS =
        Arrays.stream(ResourceParameter.values())
            .filter(ResourceParameter::ignorePathKeys)
            .collect(Collectors.toUnmodifiableSet());

    public static final Set<String> VALID_LUCENE_PARAMETER_KEYS =
        VALID_LUCENE_PARAMETERS.stream()
            .sorted()
            .map(ResourceParameter::getKey)
            .collect(Collectors.toUnmodifiableSet());

    private final String key;
    private final String[] swsKeys;
    private final String errorMessage;
    private final String pattern;
    private final KeyEncoding encode;
    private final Operator operator;

    ResourceParameter(ParamKind kind, String key) {
        this(kind, key, null, Operator.EQUALS, null, null, null);
    }

    ResourceParameter(ParamKind kind, String key, String swsKey) {
        this(kind, key, swsKey, Operator.EQUALS, null, null, null);
    }

    ResourceParameter(ParamKind paramKind, String key, String swsKey, Operator operator) {
        this(paramKind, key, swsKey, operator, null, null, null);
    }

    ResourceParameter(ParamKind kind, String key, String swsKey, Operator operator, String pattern, String errorMessage,
                      KeyEncoding encode) {
        this.key = key;
        this.operator = operator;
        this.swsKeys = (swsKey != null) ? swsKey.split("\\|") : new String[]{key};
        this.pattern = getPattern(kind, pattern);
        this.errorMessage = getErrorMessage(kind, errorMessage);
        this.encode = getEncoding(kind, encode);
    }


    @NotNull
    private static KeyEncoding getEncoding(ParamKind kind, KeyEncoding encode) {
        return switch (kind) {
            case BOOLEAN, NUMBER, RANGE -> KeyEncoding.NONE;
            case DATE, STRING -> KeyEncoding.ENCODE_DECODE;
            case CUSTOM -> nonNull(encode) ? encode : KeyEncoding.NONE;
        };
    }

    private static String getErrorMessage(ParamKind kind, String errorMessage) {
        return switch (kind) {
            case BOOLEAN -> ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;
            case DATE -> ERROR_MESSAGE_INVALID_DATE;
            case NUMBER -> ERROR_MESSAGE_INVALID_NUMBER;
            case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case STRING -> ERROR_MESSAGE_INVALID_VALUE;
            case CUSTOM -> errorMessage;
        };
    }

    private static String getPattern(ParamKind kind, String pattern) {
        return switch (kind) {
            case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            case RANGE -> PATTERN_IS_RANGE;
            case STRING -> PATTERN_IS_NON_EMPTY;
            case CUSTOM -> pattern;
        };
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Collection<String> getSwsKey() {
        return  Arrays.stream(swsKeys).toList();
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public KeyEncoding encoding() {
        return encode;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return
            new StringJoiner(":", "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(name())
                .toString();
    }

    public static ResourceParameter keyFromString(String paramName, String value) {
        var result = Arrays.stream(ResourceParameter.values())
            .filter(ResourceParameter::ignorePathKeys)
            .filter(IParameterKey.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : result.stream()
            .filter(IParameterKey.hasValidValue(value))
            .findFirst()
            .orElse(INVALID);
    }

    private static boolean ignorePathKeys(ResourceParameter f) {
        return f.ordinal() > IGNORE_PATH_PARAMETER_INDEX;
    }
}