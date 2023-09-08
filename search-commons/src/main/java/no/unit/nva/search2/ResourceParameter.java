package no.unit.nva.search2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.model.IParameterKey;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_DATE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_FIELD_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_NUMBER;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SHORT_DATE;
import static no.unit.nva.search2.model.IParameterKey.Operator.EQUALS;
import static no.unit.nva.search2.model.IParameterKey.Operator.GREATER_THAN;
import static no.unit.nva.search2.model.IParameterKey.Operator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.IParameterKey.Operator.LESS_THAN;
import static no.unit.nva.search2.model.IParameterKey.Operator.NONE;
import static no.unit.nva.search2.model.IParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.IParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.IParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.IParameterKey.ParamKind.SHORT_DATE;
import static no.unit.nva.search2.model.IParameterKey.ParamKind.STRING;

public enum ResourceParameter implements IParameterKey {
    INVALID(STRING, null),
    CATEGORY(STRING, "category", "entityDescription.reference.publicationInstance"),
    CONTRIBUTOR(STRING, "contributor", "entityDescription.contributors.identity.id"
                                       + "|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, LESS_THAN, "created_before", "created"),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "created_since", "created"),
    DOI(CUSTOM, "doi"),
    FUNDING(STRING, "funding", "fundings.identifier|source.identifier"),
    FUNDING_SOURCE(STRING, "funding_source", "fundings.source.identifier"),
    ID(STRING, "id", "identifier"),
    INSTITUTION(STRING, "institution", "entityDescription.contributors.affiliation.id"
                                       + "|entityDescription.contributors.affiliation.name"),
    ISSN(STRING, "issn", "entityDescription.reference.publicationContext.onlineIssn"
                         + "|entityDescription.reference.publicationContext.printIssn"),
    MODIFIED_BEFORE(SHORT_DATE, LESS_THAN, "modified_before", "modified"),
    MODIFIED_SINCE(SHORT_DATE, GREATER_THAN_OR_EQUAL_TO, "modified_since", "modified"),
    PROJECT_CODE(STRING, "project_code", "fundings.identifier"),
    PUBLISHED_BEFORE(NUMBER, LESS_THAN, "published_before", "entityDescription.publicationDate.year"),
    PUBLISHED_SINCE(NUMBER, GREATER_THAN, "published_since", "entityDescription.publicationDate.year"),
    TITLE(STRING, "title", "entityDescription.mainTitle"),
    UNIT(STRING, "unit", "entityDescription.contributors.affiliation.id"),
    USER(STRING, "user", "resourceOwner.owner"),
    YEAR_REPORTED(NUMBER, "year_reported", "entityDescription.publicationDate.year"),
    SEARCH_ALL(CUSTOM, NONE, "query", ""),
    SEARCH_AFTER(CUSTOM, "search_after"),
    FIELDS(CUSTOM, EQUALS, "fields", null, null, "all"),
    PAGE(NUMBER,"page"),
    OFFSET(NUMBER, EQUALS, "offset", "from", "offset|from", null),
    PER_PAGE(NUMBER, EQUALS, "per_page", "results", "per_page|results", null),
    SORT(CUSTOM, "sort", "orderBy"),
    SORT_ORDER(CUSTOM, EQUALS, "order", "sortOrder", null, "asc|desc"),
    LANG(STRING, "lang");

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameter> VALID_LUCENE_PARAMETERS =
        Arrays.stream(ResourceParameter.values())
            .filter(ResourceParameter::ignorePathKeys)
            .collect(Collectors.toUnmodifiableSet());

    public static final Set<String> VALID_LUCENE_PARAMETER_KEYS =
        VALID_LUCENE_PARAMETERS.stream()
            .sorted()
            .map(ResourceParameter::key)
            .collect(Collectors.toUnmodifiableSet());

    private final String queryKey;
    private final String patternOfKey;
    private final String[] theSwsKeys;
    private final String theErrorMessage;
    private final String valuePattern;
    private final KeyEncoding keyEncoding;
    private final Operator theOperator;

    ResourceParameter(ParamKind kind, String key) {
        this(kind, EQUALS, key, null, null, null);
    }

    ResourceParameter(ParamKind kind, String key, String swsKey) {
        this(kind, EQUALS, key, swsKey, null, null);
    }

    ResourceParameter(ParamKind paramKind, Operator operator, String key, String swsKey) {
        this(paramKind, operator, key, swsKey, null, null);
    }

    ResourceParameter(ParamKind kind, Operator operator, String key, String swsKey, String keyPattern,
                      String valuePattern) {
        this.queryKey = key;
        this.theOperator = operator;
        this.theSwsKeys = nonNull(swsKey) ? swsKey.split("\\|") : new String[]{key};
        this.valuePattern = getPattern(kind, valuePattern);
        this.theErrorMessage = getErrorMessage(kind);
        this.keyEncoding = getEncoding(kind, null);
        this.patternOfKey = nonNull(keyPattern) ? keyPattern : key;
    }

    @Override
    public Operator operator() {
        return theOperator;
    }

    @Override
    public String key() {
        return queryKey;
    }

    @Override
    public Collection<String> swsKey() {
        return Arrays.stream(theSwsKeys).toList();
    }

    @Override
    public String pattern() {
        return valuePattern;
    }

    @Override
    public String keyPattern() {
        return patternOfKey;
    }

    @Override
    public String errorMessage() {
        return theErrorMessage;
    }

    @Override
    public KeyEncoding encoding() {
        return keyEncoding;
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


    @NotNull
    private KeyEncoding getEncoding(ParamKind kind, KeyEncoding encode) {
        return switch (kind) {
            case SHORT_DATE, NUMBER -> KeyEncoding.NONE;
            case DATE -> KeyEncoding.DECODE;
            case STRING -> nonNull(encode) ? encode : KeyEncoding.ENCODE_DECODE;
            case CUSTOM -> nonNull(encode) ? encode : KeyEncoding.NONE;
        };
    }

    private String getErrorMessage(ParamKind kind) {
        return switch (kind) {
            // case BOOLEAN -> ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;
            case DATE, SHORT_DATE -> ERROR_MESSAGE_INVALID_DATE;
            case NUMBER -> ERROR_MESSAGE_INVALID_NUMBER;
            // case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case STRING -> ERROR_MESSAGE_INVALID_VALUE;
            case CUSTOM -> ERROR_MESSAGE_INVALID_FIELD_VALUE;
        };
    }

    private String getPattern(ParamKind kind, String pattern) {
        return switch (kind) {
            // case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case SHORT_DATE -> PATTERN_IS_SHORT_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            // case RANGE -> PATTERN_IS_RANGE;
            case STRING -> PATTERN_IS_NON_EMPTY;
            case CUSTOM -> nonNull(pattern) ? pattern : PATTERN_IS_NON_EMPTY;
        };
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
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }
}