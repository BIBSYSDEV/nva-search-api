package no.unit.nva.search2.model;

import java.util.Locale;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_DATE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_NUMBER;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.ERROR_MESSAGE_INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SHORT_DATE;
import static no.unit.nva.search2.model.ParameterKey.Operator.EQUALS;
import static no.unit.nva.search2.model.ParameterKey.Operator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.ParameterKey.Operator.LESS_THAN;
import static no.unit.nva.search2.model.ParameterKey.Operator.NONE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SHORT_DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SORT_STRING;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.STRING;

public enum ResourceParameterKey implements ParameterKey {
    INVALID(STRING),
    // Parameters converted to Lucene query
    CATEGORY(STRING, "entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR(STRING, "entityDescription.contributors.identity.id|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, LESS_THAN, "createdDate", "(?i)created.?before"),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "createdDate", "(?i)created.?since"),
    DOI(CUSTOM, "entityDescription.reference.doi"),
    FUNDING(STRING, "fundings.identifier|source.identifier"),
    FUNDING_SOURCE(STRING, "fundings.source.identifier"),
    ID(STRING, "identifier"),
    INSTITUTION(STRING,
                "entityDescription.contributors.affiliation.id|entityDescription.contributors.affiliation.name"),
    ISSN(STRING, "entityDescription.reference.publicationContext.onlineIssn"
                 + "|entityDescription.reference.publicationContext.printIssn"),
    MODIFIED_BEFORE(SHORT_DATE, LESS_THAN, "modifiedDate", "(?i)modified.?before"),
    MODIFIED_SINCE(SHORT_DATE, GREATER_THAN_OR_EQUAL_TO, "modifiedDate", "(?i)modified.?since"),
    PROJECT_CODE(STRING, "fundings.identifier", "(?i)project.?code"),
    PUBLISHED_BEFORE(NUMBER, LESS_THAN, "entityDescription.publicationDate.year",
                     "(?i)published.?before"),
    PUBLISHED_SINCE(NUMBER, GREATER_THAN_OR_EQUAL_TO, "entityDescription.publicationDate.year",
                    "(?i)published.?since"),
    TITLE(STRING, "entityDescription.mainTitle"),
    UNIT(STRING, "entityDescription.contributors.affiliation.id"),
    USER(STRING, "resourceOwner.owner"),
    YEAR_REPORTED(NUMBER, "entityDescription.publicationDate.year", "(?i)year.?reported"),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(CUSTOM, "q", "(?i)search.?all|query"),
    FIELDS(STRING),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, "(?i)offset|from"),
    SIZE(NUMBER, null, "(?i)per.?page|results|limit|size"),
    SORT(SORT_STRING, null, "(?i)order.?by|sort"),
    SORT_ORDER(CUSTOM, EQUALS, null, "(?i)sort.?order|order", "(?i)asc|desc"),
    SEARCH_AFTER(CUSTOM, NONE, null, "(?i)search.?after"),
    // ignored parameter
    LANG(STRING);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameterKey> VALID_LUCENE_PARAMETER_KEYS =
        Arrays.stream(ResourceParameterKey.values())
            .filter(ResourceParameterKey::isLucene)
            .sorted(ResourceParameterKey::compareParameterKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String theKey;
    private final String theKeyPattern;
    private final String[] theSwsKeys;
    private final String theErrorMessage;
    private final String theValuePattern;
    private final KeyEncoding theKeyEncoding;
    private final Operator theOperator;
    private final ParamKind paramkind;

    ResourceParameterKey(ParamKind kind) {
        this(kind, EQUALS, null, null, null);
    }

    ResourceParameterKey(ParamKind kind, String swsKey) {
        this(kind, EQUALS, swsKey, null, null);
    }

    ResourceParameterKey(ParamKind kind, String swsKey, String keyPattern) {
        this(kind, EQUALS, swsKey, keyPattern, null);
    }

    ResourceParameterKey(ParamKind kind, Operator operator, String swsKey, String keyPattern) {
        this(kind, operator, swsKey, keyPattern, null);
    }

    ResourceParameterKey(ParamKind kind, Operator operator, String swsKey, String keyPattern,
                         String valuePattern) {
        this.theKey = this.name().toLowerCase(Locale.getDefault());
        this.theOperator = operator;
        this.theSwsKeys = nonNull(swsKey) ? swsKey.split("\\|") : new String[]{theKey};
        this.theValuePattern = getPattern(kind, valuePattern);
        this.theErrorMessage = getErrorMessage(kind);
        this.theKeyEncoding = getEncoding(kind);
        this.theKeyPattern = nonNull(keyPattern) ? keyPattern : theKey;
        this.paramkind = kind;
    }

    @Override
    public Operator operator() {
        return theOperator;
    }

    @Override
    public String key() {
        return theKey;
    }

    @Override
    public Collection<String> swsKey() {
        return Arrays.stream(theSwsKeys).toList();
    }

    @Override
    public String pattern() {
        return theValuePattern;
    }

    @Override
    public String keyPattern() {
        return theKeyPattern;
    }

    @Override
    public String errorMessage() {
        return theErrorMessage;
    }

    @Override
    public KeyEncoding encoding() {
        return theKeyEncoding;
    }

    @Override
    public ParamKind kind() {
        return paramkind;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return
            new StringJoiner(COLON, "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(name().toLowerCase(Locale.ROOT))
                .toString();
    }

    @NotNull
    private KeyEncoding getEncoding(ParamKind kind) {
        return switch (kind) {
            case SHORT_DATE, NUMBER, CUSTOM, SORT_STRING -> KeyEncoding.NONE;
            case DATE, STRING_DECODE -> KeyEncoding.DECODE;
            case STRING -> KeyEncoding.ENCODE_DECODE;
        };
    }

    private String getErrorMessage(ParamKind kind) {
        return switch (kind) {
            // case BOOLEAN -> ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;
            case DATE, SHORT_DATE -> ERROR_MESSAGE_INVALID_DATE;
            case NUMBER -> ERROR_MESSAGE_INVALID_NUMBER;
            // case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case SORT_STRING -> ERROR_MESSAGE_INVALID_VALUE_WITH_SORT;
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
            case STRING, STRING_DECODE, SORT_STRING -> PATTERN_IS_NON_EMPTY;
            case CUSTOM -> nonNull(pattern) ? pattern : PATTERN_IS_NON_EMPTY;
        };
    }

    public static ResourceParameterKey keyFromString(String paramName) {
        var result = Arrays.stream(ResourceParameterKey.values())
                         .filter(ResourceParameterKey::ignoreInvalidKey)
                         .filter(ParameterKey.equalTo(paramName))
                         .collect(Collectors.toSet());
        return result.size() == 1
                   ? result.stream().findFirst().get()
                   : INVALID;
    }

    private static boolean ignoreInvalidKey(ResourceParameterKey f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isLucene(ResourceParameterKey f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX && f.ordinal() < SEARCH_ALL.ordinal();
    }

    private static int compareParameterKey(ResourceParameterKey key1, ResourceParameterKey key2) {
        return key1.ordinal() - key2.ordinal();
    }
}