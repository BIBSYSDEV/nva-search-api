package no.unit.nva.search2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.model.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_DATE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_NUMBER;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SHORT_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DOI_URL;
import static no.unit.nva.search2.model.ParameterKey.Operator.EQUALS;
import static no.unit.nva.search2.model.ParameterKey.Operator.GREATER_THAN;
import static no.unit.nva.search2.model.ParameterKey.Operator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.ParameterKey.Operator.LESS_THAN;
import static no.unit.nva.search2.model.ParameterKey.Operator.NONE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SHORT_DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.STRING;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.STRING_DECODE;

public enum ResourceParameterKey implements ParameterKey {
    INVALID(STRING, null),
    CATEGORY(STRING, "category", "entityDescription.reference.publicationInstance"),
    CONTRIBUTOR(STRING, "contributor", "entityDescription.contributors.identity.id"
                                       + "|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, LESS_THAN, "created_before", "created"),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "created_since", "created"),
    DOI(CUSTOM, EQUALS, "doi", "entityDescription.reference.doi", null,PATTERN_IS_DOI_URL),
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
    PAGE(NUMBER,"page"),
    OFFSET(NUMBER, EQUALS, "offset", "from", "offset|from", null),
    PER_PAGE(NUMBER, EQUALS, "results", "size", "per.page|results|limit", null),
    SORT(STRING_DECODE, EQUALS,"sort", null, "(?i)orderBy|sort", PATTERN_IS_NON_EMPTY),
    SORT_ORDER(CUSTOM, EQUALS, "sortOrder", null, "(?i)order|sortOrder", "asc|desc"),
    SEARCH_AFTER(CUSTOM, "search_after"),
    FIELDS(CUSTOM, EQUALS, "fields", null, null, "all"),
    LANG(STRING, "lang");

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameterKey> VALID_LUCENE_PARAMETERS =
        Arrays.stream(ResourceParameterKey.values())
            .filter(ResourceParameterKey::ignorePathKeys)
            .collect(Collectors.toUnmodifiableSet());

    public static final Set<String> VALID_LUCENE_PARAMETER_KEYS =
        VALID_LUCENE_PARAMETERS.stream()
            .sorted()
            .map(ResourceParameterKey::key)
            .collect(Collectors.toUnmodifiableSet());

    private final String queryKey;
    private final String patternOfKey;
    private final String[] theSwsKeys;
    private final String theErrorMessage;
    private final String valuePattern;
    private final KeyEncoding keyEncoding;
    private final Operator theOperator;

    ResourceParameterKey(ParamKind kind, String key) {
        this(kind, EQUALS, key, null, null, null);
    }

    ResourceParameterKey(ParamKind kind, String key, String swsKey) {
        this(kind, EQUALS, key, swsKey, null, null);
    }

    ResourceParameterKey(ParamKind paramKind, Operator operator, String key, String swsKey) {
        this(paramKind, operator, key, swsKey, null, null);
    }

    ResourceParameterKey(ParamKind kind, Operator operator, String key, String swsKey, String keyPattern,
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
            case DATE, STRING_DECODE -> KeyEncoding.DECODE;
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
            case STRING, STRING_DECODE, CUSTOM -> ERROR_MESSAGE_INVALID_VALUE;
        };
    }

    private String getPattern(ParamKind kind, String pattern) {
        return switch (kind) {
            // case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case SHORT_DATE -> PATTERN_IS_SHORT_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            // case RANGE -> PATTERN_IS_RANGE;
            case STRING, STRING_DECODE -> PATTERN_IS_NON_EMPTY;
            case CUSTOM -> nonNull(pattern) ? pattern : PATTERN_IS_NON_EMPTY;
        };
    }

    public static ResourceParameterKey keyFromString(String paramName, String value) {
        var result = Arrays.stream(ResourceParameterKey.values())
                         .filter(ResourceParameterKey::ignorePathKeys)
                         .filter(ParameterKey.equalTo(paramName))
                         .collect(Collectors.toSet());
        return result.size() == 1
                   ? result.stream().findFirst().get()
                   : result.stream()
                         .filter(ParameterKey.hasValidValue(value))
                         .findFirst()
                         .orElse(INVALID);
    }

    private static boolean ignorePathKeys(ResourceParameterKey f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }
}