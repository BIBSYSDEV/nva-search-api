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
import static no.unit.nva.search2.constant.ApplicationConstants.UNDERSCORE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_DATE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_NUMBER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CASE_INSENSITIVE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.model.ParameterKey.Operator.EQUALS;
import static no.unit.nva.search2.model.ParameterKey.Operator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.ParameterKey.Operator.LESS_THAN;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SORT_STRING;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.STRING;

public enum ResourceParameterKey implements ParameterKey {
    INVALID(STRING),
    // Parameters converted to Lucene query
    CATEGORY(STRING, "entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR(STRING, "entityDescription.contributors.identity.id"
                        + "|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, LESS_THAN, "createdDate"),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "createdDate"),
    DOI(CUSTOM, "entityDescription.reference.doi"),
    FUNDING(STRING, "fundings.identifier|source.identifier"),
    FUNDING_SOURCE(STRING, "fundings.source.identifier"),
    ID(STRING, "identifier"),
    INSTITUTION(STRING, "entityDescription.contributors.affiliation.id"
                        + "|entityDescription.contributors.affiliation.name"),
    ISBN(STRING, "entityDescription.reference.publicationContext.isbnList"),
    ISSN(STRING, "entityDescription.reference.publicationContext.onlineIssn"
                 + "|entityDescription.reference.publicationContext.printIssn"),
    ORCID(STRING, "entityDescription.contributors.identity.orcId"),
    MODIFIED_BEFORE(DATE, LESS_THAN, "modifiedDate"),
    MODIFIED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "modifiedDate"),
    PROJECT_CODE(STRING, "fundings.identifier"),
    PUBLISHED_BEFORE(NUMBER, LESS_THAN, "entityDescription.publicationDate.year"),
    PUBLISHED_SINCE(NUMBER, GREATER_THAN_OR_EQUAL_TO, "entityDescription.publicationDate.year"),
    TITLE(STRING, "entityDescription.mainTitle"),
    UNIT(STRING, "entityDescription.contributors.affiliation.id"),
    USER(STRING, "resourceOwner.owner"),
    YEAR_REPORTED(NUMBER, "entityDescription.publicationDate.year"),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(CUSTOM, EQUALS, "q", "(?i)search.?all|query", null),
    FIELDS(STRING),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, "(?i)offset|from", null),
    SIZE(NUMBER, null, null, "(?i)per.?page|results|limit|size", null),
    SORT(SORT_STRING, null, null, "(?i)order.?by|sort", null),
    SORT_ORDER(CUSTOM, EQUALS, null, "(?i)sort.?order|order", "(?i)asc|desc"),
    SEARCH_AFTER(CUSTOM),
    // ignored parameter
    LANG(STRING);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameterKey> VALID_LUCENE_PARAMETER_KEYS =
        Arrays.stream(ResourceParameterKey.values())
            .filter(ResourceParameterKey::isLucene)
            .sorted(ResourceParameterKey::compareAscending)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String theKey;
    private final String theKeyPattern;
    private final String[] theFieldsToSearch;
    private final String theErrorMessage;
    private final String theValuePattern;
    private final KeyEncoding theKeyEncoding;
    private final Operator theOperator;
    private final ParamKind paramkind;

    ResourceParameterKey(ParamKind kind) {
        this(kind, EQUALS, null, null, null);
    }

    ResourceParameterKey(ParamKind kind, String fieldsToSearch) {
        this(kind, EQUALS, fieldsToSearch, null, null);
    }

    ResourceParameterKey(ParamKind kind, Operator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null);
    }

    ResourceParameterKey(
        ParamKind kind, Operator operator, String fieldsToSearch, String keyPattern, String valuePattern) {

        this.theKey = this.name().toLowerCase(Locale.getDefault());
        this.theOperator = operator;
        this.theFieldsToSearch = nonNull(fieldsToSearch)
                                     ? fieldsToSearch.split("\\|")
                                     : new String[]{theKey};
        this.theValuePattern = getValuePattern(kind, valuePattern);
        this.theErrorMessage = getErrorMessage(kind);
        this.theKeyEncoding = getEncoding(kind);
        this.theKeyPattern = nonNull(keyPattern)
                                 ? keyPattern
                                 : PATTERN_IS_CASE_INSENSITIVE + theKey.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
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
        return Arrays.stream(theFieldsToSearch).toList();
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
            case NUMBER, CUSTOM -> KeyEncoding.NONE;
            case DATE, STRING, SORT_STRING -> KeyEncoding.DECODE;
        };
    }

    private String getErrorMessage(ParamKind kind) {
        return switch (kind) {
            // case BOOLEAN -> ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;
            case DATE -> INVALID_DATE;
            case NUMBER -> INVALID_NUMBER;
            // case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case SORT_STRING -> INVALID_VALUE_WITH_SORT;
            case STRING, CUSTOM -> INVALID_VALUE;
        };
    }

    private String getValuePattern(ParamKind kind, String pattern) {
        return switch (kind) {
            // case BOOLEAN -> PATTERN_IS_BOOLEAN;
            case DATE -> PATTERN_IS_DATE;
            case NUMBER -> PATTERN_IS_NUMBER;
            // case RANGE -> PATTERN_IS_RANGE;
            case STRING, SORT_STRING -> PATTERN_IS_NON_EMPTY;
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

    private static int compareAscending(ResourceParameterKey key1, ResourceParameterKey key2) {
        return key1.ordinal() - key2.ordinal();
    }
}