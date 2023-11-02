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
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SORT_STRING;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.STRING;

/**
 * Enum for all the parameters that can be used to query the search index.
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 *
 */

public enum ResourceParameterKey implements ParameterKey {
    INVALID(STRING),
    // Parameters converted to Lucene query
    CATEGORY(STRING, "entityDescription.reference.publicationInstance.type"),
    CATEGORY_NOT(STRING, MUST_NOT, "entityDescription.reference.publicationInstance.type"),
    CATEGORY_SHOULD(STRING, SHOULD, "entityDescription.reference.publicationInstance.type"),
    CONTRIBUTOR_ID(CUSTOM, "entityDescription.contributors.identity.id"),
    CONTRIBUTOR(STRING, "entityDescription.contributors.identity.id"
                        + "|entityDescription.contributors.identity.name"),
    CONTRIBUTOR_NOT(STRING, MUST_NOT, "entityDescription.contributors.identity.id"
                        + "|entityDescription.contributors.identity.name"),
    CONTRIBUTOR_SHOULD(STRING, SHOULD, "entityDescription.contributors.identity.id"
                                      + "|entityDescription.contributors.identity.name"),
    CREATED_BEFORE(DATE, LESS_THAN, "createdDate"),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "createdDate"),
    DOI(STRING, "entityDescription.reference.doi"),
    DOI_NOT(STRING, MUST_NOT, "entityDescription.reference.doi"),
    DOI_SHOULD(STRING, SHOULD, "entityDescription.reference.doi"),
    FUNDING(STRING, MUST, "fundings.identifier|fundings.source.identifier",
            null, PATTERN_IS_FUNDING, null),
    FUNDING_SOURCE(STRING, "fundings.source.identifier|fundings.source.labels"),
    FUNDING_SOURCE_NOT(STRING, MUST_NOT, "fundings.source.identifier|fundings.source.labels"),
    FUNDING_SOURCE_SHOULD(STRING, SHOULD, "fundings.source.identifier|fundings.source.labels"),
    ID(STRING, "identifier"),
    ID_NOT(STRING, MUST_NOT, "identifier"),
    ID_SHOULD(STRING, MUST_NOT, "identifier"),
    INSTITUTION(STRING, "entityDescription.contributors.affiliation.id"
                        + "|entityDescription.contributors.affiliation.name"),
    INSTITUTION_NOT(STRING, MUST_NOT, "entityDescription.contributors.affiliation.id"
                                      + "|entityDescription.contributors.affiliation.name"),
    INSTITUTION_SHOULD(STRING, SHOULD, "entityDescription.contributors.affiliation.id"
                                      + "|entityDescription.contributors.affiliation.name"),
    ISBN(STRING, "entityDescription.reference.publicationContext.isbnList"),
    ISBN_NOT(STRING, MUST_NOT, "entityDescription.reference.publicationContext.isbnList"),
    ISBN_SHOULD(STRING, SHOULD, "entityDescription.reference.publicationContext.isbnList"),
    ISSN(STRING, "entityDescription.reference.publicationContext.onlineIssn"
                 + "|entityDescription.reference.publicationContext.printIssn"),
    ISSN_NOT(STRING, MUST_NOT, "entityDescription.reference.publicationContext.onlineIssn"
                               + "|entityDescription.reference.publicationContext.printIssn"),
    ISSN_SHOULD(STRING, SHOULD, "entityDescription.reference.publicationContext.onlineIssn"
                               + "|entityDescription.reference.publicationContext.printIssn"),
    ORCID(STRING, "entityDescription.contributors.identity.orcId"),
    ORCID_NOT(STRING, MUST_NOT, "entityDescription.contributors.identity.orcId"),
    ORCID_SHOULD(STRING, SHOULD, "entityDescription.contributors.identity.orcId"),
    MODIFIED_BEFORE(DATE, LESS_THAN, "modifiedDate"),
    MODIFIED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "modifiedDate"),
    PROJECT(STRING, "projects.id"),
    PROJECT_NOT(STRING, MUST_NOT, "projects.id"),
    PROJECT_SHOULD(STRING, SHOULD, "projects.id"),
    PUBLISHED_BEFORE(DATE, LESS_THAN, "publishedDate"),
    PUBLISHED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, "publishedDate"),
    TITLE(STRING, "entityDescription.mainTitle", 2F),
    TITLE_NOT(STRING, MUST_NOT, "entityDescription.mainTitle"),
    TITLE_SHOULD(STRING, SHOULD, "entityDescription.mainTitle"),
    UNIT(STRING, "entityDescription.contributors.affiliation.id"),
    UNIT_NOT(STRING, MUST_NOT,"entityDescription.contributors.affiliation.id"),
    UNIT_SHOULD(STRING, SHOULD,"entityDescription.contributors.affiliation.id"),
    USER(STRING, "resourceOwner.owner"),
    USER_NOT(STRING, MUST_NOT,"resourceOwner.owner"),
    USER_SHOULD(STRING, SHOULD,"resourceOwner.owner"),
    PUBLICATION_YEAR(NUMBER, MUST, "entityDescription.publicationDate.year",
                     "(?i)year.?reported|publication.?year", null, null),
    PUBLICATION_YEAR_SHOULD(NUMBER, SHOULD, "entityDescription.publicationDate.year",
                     "(?i)year.?reported|publication.?year", null, null),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(STRING, MUST, "q", "(?i)search.?all|query", null, null),
    FIELDS(CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, "(?i)offset|from", null, null),
    SIZE(NUMBER, null, null, "(?i)per.?page|results|limit|size", null, null),
    SORT(SORT_STRING, null, null, "(?i)order.?by|sort", null, null),
    SORT_ORDER(CUSTOM, MUST, null, "(?i)sort.?order|order", "(?i)asc|desc", null),
    SEARCH_AFTER(CUSTOM),
    // ignored parameter
    LANG(STRING);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameterKey> VALID_LUCENE_PARAMETER_KEYS =
        Arrays.stream(ResourceParameterKey.values())
            .filter(ResourceParameterKey::isSearchField)
            .sorted(ResourceParameterKey::compareAscending)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final String key;
    private final ValueEncoding encoding;
    private final String keyPattern;
    private final String validValuePattern;
    private final String[] fieldsToSearch;
    private final FieldOperator fieldOperator;
    private final String errorMsg;
    private final ParamKind paramkind;
    private final Float boost;

    ResourceParameterKey(ParamKind kind) {
        this(kind, MUST, null, null, null, null);
    }

    ResourceParameterKey(ParamKind kind, String fieldsToSearch) {
        this(kind, MUST, fieldsToSearch, null, null, null);
    }

    ResourceParameterKey(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, MUST, fieldsToSearch, null, null, boost);
    }

    ResourceParameterKey(ParamKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ResourceParameterKey(
        ParamKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern, String valuePattern,
        Float boost) {

        this.key = this.name().toLowerCase(Locale.getDefault());
        this.fieldOperator = operator;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
                                  ? fieldsToSearch.split("\\|")
                                  : new String[]{key};
        this.validValuePattern = getValuePattern(kind, valuePattern);
        this.errorMsg = getErrorMessage(kind);
        this.encoding = getEncoding(kind);
        this.keyPattern = nonNull(keyPattern)
                              ? keyPattern
                              : PATTERN_IS_IGNORE_CASE + key.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE) + "*";
        this.paramkind = kind;
    }

    @Override
    public String fieldName() {
        return key;
    }

    @Override
    public Float fieldBoost() {
        return boost;
    }

    @Override
    public ParamKind fieldType() {
        return paramkind;
    }

    @Override
    public String fieldPattern() {
        return keyPattern;
    }

    @Override
    public String valuePattern() {
        return validValuePattern;
    }

    @Override
    public ValueEncoding valueEncoding() {
        return encoding;
    }

    @Override
    public Collection<String> searchFields() {
        return Arrays.stream(fieldsToSearch).toList();
    }

    @Override
    public FieldOperator searchOperator() {
        return fieldOperator;
    }

    @Override
    public String errorMessage() {
        return errorMsg;
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
    @JacocoGenerated
    private ValueEncoding getEncoding(ParamKind kind) {
        return switch (kind) {
            case NUMBER, CUSTOM -> ValueEncoding.NONE;
            case DATE,  STRING, SORT_STRING -> ValueEncoding.DECODE;
        };
    }

    @JacocoGenerated
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

    @JacocoGenerated
    private String getValuePattern(ParamKind kind, String pattern) {
        return
            nonNull(pattern) ? pattern
                : switch (kind) {
                    // case BOOLEAN -> PATTERN_IS_BOOLEAN;
                    case DATE -> PATTERN_IS_DATE;
                    case NUMBER -> PATTERN_IS_NUMBER;
                    // case RANGE -> PATTERN_IS_RANGE;
                    case CUSTOM, STRING, SORT_STRING -> PATTERN_IS_NON_EMPTY;
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

    private static boolean isSearchField(ResourceParameterKey f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX && f.ordinal() < SEARCH_ALL.ordinal();
    }

    private static int compareAscending(ResourceParameterKey key1, ResourceParameterKey key2) {
        return key1.ordinal() - key2.ordinal();
    }
}