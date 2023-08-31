package no.unit.nva.search2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.IParameterKey;

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
    FIELDS(CUSTOM, "fields", null, "all", ERROR_MESSAGE_INVALID_FIELD_VALUE, KeyEncoding.NONE, Operator.EQUALS),
    PAGE(NUMBER,"page","from"),
    PER_PAGE(NUMBER,"per_page", "results"),
    SORT(STRING,"sort", "orderBy"),
    SORT_ORDER(CUSTOM,"order","sortOrder", "asc|desc", null, KeyEncoding.NONE, Operator.EQUALS),
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
    PROJECT_CODE(CUSTOM, "project_code", "fundings.identifier", ".", null, KeyEncoding.NONE, Operator.EQUALS),
    PUBLISHED_BEFORE(NUMBER,"published_before","entityDescription.publicationDate.year", null, null, KeyEncoding.NONE,
                     Operator.LESS_THAN),
    PUBLISHED_SINCE(NUMBER,"published_since","entityDescription.publicationDate.year", null, null, KeyEncoding.NONE,
                    Operator.GREATER_THAN_OR_EQUAL_TO),
    TITLE(STRING,"title","entityDescription.mainTitle"),
    UNIT(STRING,"unit","entityDescription.contributors.affiliation.id"),
    QUERY(STRING,"q","query"),
    USER(STRING,"user", "resourceOwner.owner"),
    YEAR_REPORTED(NUMBER,"year_reported","entityDescription.publicationDate.year"),
    SEARCH_ALL(STRING,"query", ""),
    LANG(CUSTOM, "lang", null, ".", "ignored", KeyEncoding.NONE, Operator.EQUALS);

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
        this(kind, key, null, null, null, KeyEncoding.NONE, Operator.EQUALS);
    }

    ResourceParameter(ParamKind kind, String key, String swsKey) {
        this(kind, key, swsKey, null, null, KeyEncoding.NONE, Operator.EQUALS);
    }

    ResourceParameter(ParamKind kind, String key, String swsKey, String pattern, String errorMessage,
                      KeyEncoding encode, Operator operator) {
        this.key = key;
        this.operator = operator;
        this.swsKeys = (swsKey != null) ? swsKey.split("\\|") : new String[]{key};
        this.encode = encode;
        this.pattern = switch (kind) {
            case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            case RANGE -> PATTERN_IS_RANGE;
            case STRING -> PATTERN_IS_NON_EMPTY;
            case CUSTOM -> pattern;
        };
        this.errorMessage = switch (kind) {
            case BOOLEAN -> ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;
            case DATE -> ERROR_MESSAGE_INVALID_DATE;
            case NUMBER -> ERROR_MESSAGE_INVALID_NUMBER;
            case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case STRING -> ERROR_MESSAGE_INVALID_VALUE;
            case CUSTOM -> errorMessage;
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