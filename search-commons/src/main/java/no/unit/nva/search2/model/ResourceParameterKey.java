package no.unit.nva.search2.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import no.unit.nva.search2.ResourceSwsQuery;
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
    // Parameters converted to Lucene query
    CATEGORY(STRING, "category", "entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR(STRING, "contributor",
        "entityDescription.contributors.identity.id|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, LESS_THAN, "created_before", "created"),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "created_since", "created"),
    DOI(CUSTOM, EQUALS, "doi", "entityDescription.reference.doi", null, PATTERN_IS_NON_EMPTY),
    FUNDING(STRING, "funding", "fundings.identifier|source.identifier"),
    FUNDING_SOURCE(STRING, "funding_source", "fundings.source.identifier"),
    ID(STRING, "id", "identifier"),
    INSTITUTION(STRING, "institution",
        "entityDescription.contributors.affiliation.id|entityDescription.contributors.affiliation.name"),
    ISSN(STRING, "issn","entityDescription.reference.publicationContext.onlineIssn"
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
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(CUSTOM, NONE, "query", ""),
    FIELDS(STRING, EQUALS,"fields",null, null,PATTERN_IS_NON_EMPTY),
    // Pagination parameters
    PAGE(NUMBER, "page"),
    FROM(NUMBER, EQUALS, "from", null,"(?i)offset|from", null),
    SIZE(NUMBER, EQUALS, "size", null, "(?i)per.?page|results|limit|size", null),
    SORT(STRING_DECODE, EQUALS, "sort", null, "(?i)order.?by|sort", PATTERN_IS_NON_EMPTY),
    SORT_ORDER(CUSTOM, EQUALS, "sortOrder", null, "(?i)sort.?order|order", "asc|desc"),
    SEARCH_AFTER(CUSTOM, NONE, "search_after", null, "(?i)search.?after", PATTERN_IS_NON_EMPTY),
    // ignored parameter
    LANG(STRING, "lang");

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameterKey> VALID_LUCENE_PARAMETER_KEYS =
        Arrays.stream(ResourceParameterKey.values())
            .filter(ResourceParameterKey::isLucene)
            .sorted(ResourceSwsQuery::compareParameterKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));


    private final String queryKey;
    private final String patternOfKey;
    private final String[] theSwsKeys;
    private final String theErrorMessage;
    private final String valuePattern;
    private final KeyEncoding keyEncoding;
    private final Operator theOperator;
    private final ParamKind paramkind;

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
        this.keyEncoding = getEncoding(kind);
        this.patternOfKey = nonNull(keyPattern) ? keyPattern : key;
        this.paramkind = kind;
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
    public ParamKind kind() {
        return paramkind;
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
    private KeyEncoding getEncoding(ParamKind kind) {
        return switch (kind) {
            case SHORT_DATE, NUMBER -> KeyEncoding.NONE;
            case DATE, STRING_DECODE -> KeyEncoding.DECODE;
            case STRING -> KeyEncoding.ENCODE_DECODE;
            case CUSTOM -> KeyEncoding.NONE;
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

}