package no.unit.nva.search2.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.PIPE;
import static no.unit.nva.search2.constant.ApplicationConstants.UNDERSCORE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_DATE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_NUMBER;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_DATE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NON_EMPTY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NUMBER;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.KEYWORD;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SORT_KEY;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.TEXT;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

/**
 * Enum for all the parameters that can be used to query the search index.
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 *
 */

public enum ResourceParameterKey implements ParameterKey {
    INVALID(TEXT),
    // Parameters converted to Lucene query
    CATEGORY(KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CONTRIBUTOR_ID(KEYWORD, MUST, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID,null, PATTERN_IS_URI, null),
    CONTRIBUTOR(KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
                        + PIPE + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
                                      + PIPE + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
                                       + PIPE + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME),
    CREATED_BEFORE(DATE, LESS_THAN, Constants.CREATED_DATE),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, Constants.CREATED_DATE),
    DOI(TEXT, Constants.ENTITY_DESCRIPTION_REFERENCE_DOI),
    DOI_NOT(TEXT, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_DOI),
    DOI_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_DOI),
    FUNDING(KEYWORD, MUST, Constants.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER,
            null, PATTERN_IS_FUNDING, null),
    FUNDING_SOURCE(TEXT, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(TEXT, MUST_NOT, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(KEYWORD, SHOULD, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    ID(TEXT, Constants.IDENTIFIER),
    ID_NOT(TEXT, MUST_NOT, Constants.IDENTIFIER),
    ID_SHOULD(TEXT, SHOULD, Constants.IDENTIFIER),
    INSTITUTION(TEXT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID + PIPE
        + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME),
    INSTITUTION_NOT(TEXT, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID + PIPE
                                    + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME),
    INSTITUTION_SHOULD(TEXT, SHOULD,  Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID + PIPE
        + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME),
    ISBN(TEXT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_NOT(TEXT, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(TEXT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
                 + PIPE + Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN),
    ISSN_NOT(TEXT, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
                               + PIPE + Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN),
    ISSN_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
                                + PIPE + Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN),
    ORCID(TEXT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    ORCID_NOT(TEXT, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    ORCID_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    MODIFIED_BEFORE(DATE, LESS_THAN, Constants.MODIFIED_DATE),
    MODIFIED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, Constants.MODIFIED_DATE),
    PROJECT(TEXT, Constants.PROJECTS_ID),
    PROJECT_NOT(TEXT, MUST_NOT, Constants.PROJECTS_ID),
    PROJECT_SHOULD(TEXT, SHOULD, Constants.PROJECTS_ID),
    PUBLISHED_BEFORE(DATE, LESS_THAN, Constants.PUBLISHED_DATE),
    PUBLISHED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, Constants.PUBLISHED_DATE),
    TITLE(TEXT, Constants.MAIN_TITLE, 2F),
    TITLE_NOT(TEXT, MUST_NOT, Constants.MAIN_TITLE),
    TITLE_SHOULD(TEXT, SHOULD, Constants.MAIN_TITLE),
    UNIT(KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    UNIT_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    UNIT_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    USER(TEXT, Constants.RESOURCE_OWNER),
    USER_NOT(TEXT, MUST_NOT, Constants.RESOURCE_OWNER),
    USER_SHOULD(TEXT, SHOULD, Constants.RESOURCE_OWNER),
    PUBLICATION_YEAR(NUMBER, MUST, Constants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                     "(?i)year.?reported|publication.?year", null, null),
    PUBLICATION_YEAR_SHOULD(NUMBER, SHOULD, Constants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                            "(?i)year.?reported.?should|publication.?year.?should", null, null),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, MUST, "q", "(?i)search.?all|query", null, null),
    FIELDS(CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, "(?i)offset|from", null, null),
    SIZE(NUMBER, null, null, "(?i)per.?page|results|limit|size", null, null),
    SORT(SORT_KEY, null, null, "(?i)order.?by|sort", null, null),
    SORT_ORDER(CUSTOM, MUST, null, "(?i)sort.?order|order", "(?i)asc|desc", null),
    SEARCH_AFTER(CUSTOM),
    // ignored parameter
    LANG(TEXT);

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
            case DATE, KEYWORD, TEXT, SORT_KEY -> ValueEncoding.DECODE;
        };
    }

    @JacocoGenerated
    private String getErrorMessage(ParamKind kind) {
        return switch (kind) {
            // case BOOLEAN -> ERROR_MESSAGE_TEMPLATE_INVALID_QUERY_PARAMETERS;
            case DATE -> INVALID_DATE;
            case NUMBER -> INVALID_NUMBER;
            // case RANGE -> ERROR_MESSAGE_INVALID_VALUE_WITH_RANGE;
            case SORT_KEY -> INVALID_VALUE_WITH_SORT;
            case KEYWORD, TEXT, CUSTOM -> INVALID_VALUE;
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
                    case KEYWORD, CUSTOM, TEXT, SORT_KEY -> PATTERN_IS_NON_EMPTY;
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

    private static class Constants {

        public static final String CREATED_DATE = "createdDate";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID =
            "entityDescription.contributors.affiliations.id.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME =
            "entityDescription.contributors.affiliations.labels.*.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID =
            "entityDescription.contributors.identity.id";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME =
            "entityDescription.contributors.identity.name.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID =
            "entityDescription.contributors.identity.orcId";
        public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
            "entityDescription.publicationDate.year";
        public static final String ENTITY_DESCRIPTION_REFERENCE_DOI =
            "entityDescription.reference.doi";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST =
            "entityDescription.reference.publicationContext.isbnList";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN =
            "entityDescription.reference.publicationContext.onlineIssn";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN =
            "entityDescription.reference.publicationContext.printIssn";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE =
            "entityDescription.reference.publicationInstance.type";
        public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER =
            "fundings.identifier|fundings.source.identifier";
        public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS =
            "fundings.source.labels.nb.keyword|fundings.source.labels.en.keyword";
        public static final String IDENTIFIER = "identifier";
        public static final String MAIN_TITLE = "entityDescription.mainTitle";
        public static final String MODIFIED_DATE = "modifiedDate";
        public static final String PROJECTS_ID = "projects.id";
        public static final String PUBLISHED_DATE = "publishedDate";
        public static final String RESOURCE_OWNER = "resourceOwner.owner";
    }
}