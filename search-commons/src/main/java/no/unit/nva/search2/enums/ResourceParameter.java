package no.unit.nva.search2.enums;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CATEGORY_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CATEGORY_NOT_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CATEGORY_SHOULD_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search2.constant.Resource.ASSOCIATED_ARTIFACTS_LICENSE;
import static no.unit.nva.search2.constant.Resource.ATTACHMENT_VISIBLE_FOR_NON_OWNER;
import static no.unit.nva.search2.constant.Resource.CONTRIBUTORS_AFFILIATION_ID_KEYWORD;
import static no.unit.nva.search2.constant.Resource.CONTRIBUTORS_IDENTITY_ID;
import static no.unit.nva.search2.constant.Resource.CONTRIBUTORS_IDENTITY_NAME_KEYWORD;
import static no.unit.nva.search2.constant.Resource.CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD;
import static no.unit.nva.search2.constant.Resource.ENTITY_ABSTRACT;
import static no.unit.nva.search2.constant.Resource.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION;
import static no.unit.nva.search2.constant.Resource.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search2.constant.Resource.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
import static no.unit.nva.search2.constant.Resource.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN;
import static no.unit.nva.search2.constant.Resource.ENTITY_DESCRIPTION_REFERENCE_SERIES;
import static no.unit.nva.search2.constant.Resource.ENTITY_TAGS;
import static no.unit.nva.search2.constant.Resource.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER;
import static no.unit.nva.search2.constant.Resource.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS;
import static no.unit.nva.search2.constant.Resource.IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.constant.Resource.PARENT_PUBLICATION_ID;
import static no.unit.nva.search2.constant.Resource.PUBLICATION_CONTEXT_ISBN_LIST;
import static no.unit.nva.search2.constant.Resource.PUBLICATION_CONTEXT_PUBLISHER;
import static no.unit.nva.search2.constant.Resource.PUBLICATION_CONTEXT_TYPE_KEYWORD;
import static no.unit.nva.search2.constant.Resource.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.constant.Resource.PUBLICATION_STATUS;
import static no.unit.nva.search2.constant.Resource.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search2.constant.Resource.REFERENCE_DOI_KEYWORD;
import static no.unit.nva.search2.constant.Resource.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD;
import static no.unit.nva.search2.constant.Resource.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search2.constant.Resource.TOP_LEVEL_ORG_ID;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.CREATED_DATE;
import static no.unit.nva.search2.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.constant.Words.PROJECTS_ID;
import static no.unit.nva.search2.constant.Words.PUBLISHED_DATE;
import static no.unit.nva.search2.constant.Words.Q;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.BETWEEN;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.FUZZY_TEXT;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.KEYWORD;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.TEXT;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

/**
 * Enum for all the parameters that can be used to query the search index.
 * <p>
 *     Parameter values can be read as camelCase or snake_case (e.g. 'createdSince' or 'created_since').
 * </p>
 * <p>
 *     Values are: (INVALID,) ABSTRACT, CONTEXT_TYPE, CONTRIBUTOR, CONTRIBUTOR_NAME, CREATED_BEFORE, CREATED_SINCE,
 *     DOI, FUNDING, FUNDING_SOURCE, HAS_FILE, ID, INSTANCE_TYPE, INSTITUTION, ISBN, ISSN, LICENSE, MODIFIED_BEFORE,
 *     MODIFIED_SINCE, PARENT_PUBLICATION, PROJECT, PUBLISH_STATUS, PUBLISHED_BETWEEN, PUBLISHED_BEFORE,
 *     PUBLISHED_SINCE, PUBLISHER, TAGS, TITLE, TOP_LEVEL_ORGANIZATION, UNIT, USER, USER_AFFILIATION,
 *     PUBLICATION_YEAR_BEFORE, PUBLICATION_YEAR_SINCE, SERIES, SEARCH_ALL, FIELDS, AGGREGATION, PAGE, FROM, SIZE,
 *     SORT, SORT_ORDER, SEARCH_AFTER (, LANG)
 * </p>
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */
public enum ResourceParameter implements ParameterKey {
    INVALID(ParamKind.INVALID),
    // Parameters used for filtering
    CRISTIN_IDENTIFIER(CUSTOM),
    SCOPUS_IDENTIFIER(CUSTOM),
    ABSTRACT(FUZZY_TEXT, ENTITY_ABSTRACT),
    ABSTRACT_NOT(TEXT, MUST_NOT, ENTITY_ABSTRACT),
    ABSTRACT_SHOULD(FUZZY_TEXT, SHOULD, ENTITY_ABSTRACT),
    CONTEXT_TYPE(KEYWORD, MUST, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_NOT(KEYWORD, MUST_NOT, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_SHOULD(KEYWORD, SHOULD, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTRIBUTOR(KEYWORD, MUST, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null, false),
    CONTRIBUTOR_NOT(KEYWORD, MUST_NOT, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null, true),
    CONTRIBUTOR_SHOULD(KEYWORD, SHOULD, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null, true),
    CONTRIBUTOR_NAME(TEXT, MUST, CONTRIBUTORS_IDENTITY_NAME_KEYWORD, false),
    CONTRIBUTOR_NAME_NOT(TEXT, MUST_NOT, CONTRIBUTORS_IDENTITY_NAME_KEYWORD, true),
    CONTRIBUTOR_NAME_SHOULD(FUZZY_TEXT, SHOULD, CONTRIBUTORS_IDENTITY_NAME_KEYWORD, true),
    CREATED_BEFORE(ParamKind.DATE, LESS_THAN, CREATED_DATE),
    CREATED_SINCE(ParamKind.DATE, GREATER_THAN_OR_EQUAL_TO, CREATED_DATE),
    DOI(KEYWORD, REFERENCE_DOI_KEYWORD),
    DOI_NOT(KEYWORD, MUST_NOT, REFERENCE_DOI_KEYWORD),
    DOI_SHOULD(TEXT, SHOULD, REFERENCE_DOI_KEYWORD),
    FUNDING(KEYWORD, MUST, FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER, null, PATTERN_IS_FUNDING, null, null),
    FUNDING_SOURCE(TEXT, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(TEXT, MUST_NOT, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(TEXT, SHOULD, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    HAS_FILE(KEYWORD, MUST, ATTACHMENT_VISIBLE_FOR_NON_OWNER),
    HAS_FILE_SHOULD(KEYWORD, SHOULD, ATTACHMENT_VISIBLE_FOR_NON_OWNER),
    ID(KEYWORD, IDENTIFIER_KEYWORD),
    ID_NOT(KEYWORD, MUST_NOT, IDENTIFIER_KEYWORD),
    ID_SHOULD(TEXT, SHOULD, IDENTIFIER_KEYWORD),
    INSTANCE_TYPE(KEYWORD, MUST, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_KEYS),
    INSTANCE_TYPE_NOT(KEYWORD, MUST_NOT, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_NOT_KEYS),
    INSTANCE_TYPE_SHOULD(KEYWORD, SHOULD, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_SHOULD_KEYS),
    INSTITUTION(TEXT, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_NOT(TEXT, MUST_NOT, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_SHOULD(TEXT, SHOULD, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    ISBN(KEYWORD, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_NOT(KEYWORD, MUST_NOT, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(TEXT, SHOULD, PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(KEYWORD, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_NOT(KEYWORD, MUST_NOT, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_SHOULD(TEXT, SHOULD, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    LICENSE(KEYWORD, MUST, ASSOCIATED_ARTIFACTS_LICENSE),
    LICENSE_NOT(KEYWORD, MUST_NOT, ASSOCIATED_ARTIFACTS_LICENSE),
    LICENSE_SHOULD(KEYWORD, SHOULD, ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED_BEFORE(ParamKind.DATE, LESS_THAN, MODIFIED_DATE),
    MODIFIED_SINCE(ParamKind.DATE, GREATER_THAN_OR_EQUAL_TO, MODIFIED_DATE),
    ORCID(KEYWORD, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_NOT(KEYWORD, MUST_NOT, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_SHOULD(TEXT, SHOULD, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    PARENT_PUBLICATION(KEYWORD, MUST, PARENT_PUBLICATION_ID),
    PARENT_PUBLICATION_SHOULD(TEXT, SHOULD, PARENT_PUBLICATION_ID),
    PROJECT(KEYWORD, PROJECTS_ID),
    PROJECT_NOT(KEYWORD, MUST_NOT, PROJECTS_ID),
    PROJECT_SHOULD(KEYWORD, SHOULD, PROJECTS_ID),
    PUBLICATION_YEAR_BEFORE(NUMBER, LESS_THAN, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLICATION_YEAR_SHOULD(NUMBER, SHOULD, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
        PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS),
    PUBLICATION_YEAR_SINCE(NUMBER, GREATER_THAN_OR_EQUAL_TO, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLISHED_BEFORE(ParamKind.DATE, LESS_THAN, PUBLISHED_DATE),
    PUBLISHED_BETWEEN(ParamKind.DATE, BETWEEN, PUBLISHED_DATE),
    PUBLISHED_SINCE(ParamKind.DATE, GREATER_THAN_OR_EQUAL_TO, PUBLISHED_DATE),
    PUBLISHER(KEYWORD, MUST, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_NOT(KEYWORD, MUST_NOT, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_SHOULD(KEYWORD, SHOULD, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_ID(KEYWORD, MUST, PUBLISHER_ID_KEYWORD),
    PUBLISHER_ID_NOT(KEYWORD, MUST_NOT, PUBLISHER_ID_KEYWORD),
    PUBLISHER_ID_SHOULD(KEYWORD, SHOULD, PUBLISHER_ID_KEYWORD),
    SERIES(KEYWORD, MUST, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    SERIES_NOT(KEYWORD, MUST_NOT, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    SERIES_SHOULD(KEYWORD, SHOULD, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    STATUS(KEYWORD, MUST, PUBLICATION_STATUS),
    STATUS_NOT(KEYWORD, MUST_NOT, PUBLICATION_STATUS),
    STATUS_SHOULD(KEYWORD, SHOULD, PUBLICATION_STATUS),
    TAGS(TEXT, MUST, ENTITY_TAGS, false),
    TAGS_NOT(TEXT, MUST_NOT, ENTITY_TAGS, true),
    TAGS_SHOULD(TEXT, SHOULD, ENTITY_TAGS, true),
    TITLE(FUZZY_TEXT, ENTITY_DESCRIPTION_MAIN_TITLE, 2F),
    TITLE_NOT(TEXT, MUST_NOT, ENTITY_DESCRIPTION_MAIN_TITLE),
    TITLE_SHOULD(FUZZY_TEXT, SHOULD, ENTITY_DESCRIPTION_MAIN_TITLE),
    TOP_LEVEL_ORGANIZATION(KEYWORD, MUST, TOP_LEVEL_ORG_ID, true),
    UNIT(KEYWORD, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_NOT(KEYWORD, MUST_NOT, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_SHOULD(TEXT, SHOULD, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    USER(KEYWORD, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_AFFILIATION(KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_NOT(KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_SHOULD(TEXT, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_NOT(KEYWORD, MUST_NOT, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_SHOULD(TEXT, SHOULD, RESOURCE_OWNER_OWNER_KEYWORD),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, MUST, Q, PATTERN_IS_SEARCH_ALL_KEY),
    FIELDS(CUSTOM),
    // Pagination parameters
    AGGREGATION(CUSTOM),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY),
    SORT(ParamKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY),
    SORT_ORDER(CUSTOM, MUST, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null, null),
    SEARCH_AFTER(CUSTOM),
    // ignored parameter
    LANG(CUSTOM);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameter> VALID_SEARCH_PARAMETER_KEYS =
        Arrays.stream(ResourceParameter.values())
            .filter(ResourceParameter::isSearchField)
            .sorted(ParameterKey::compareAscending)
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
    private final Boolean isNested;

    ResourceParameter(ParamKind kind) {
        this(kind, MUST, null, null, null, null, null);
    }

    ResourceParameter(ParamKind kind, String fieldsToSearch) {
        this(kind, MUST, fieldsToSearch, null, null, null, null);
    }

    ResourceParameter(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, MUST, fieldsToSearch, null, null, boost, null);
    }

    ResourceParameter(ParamKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null, null);
    }

    ResourceParameter(ParamKind paramKind, FieldOperator operator, String fieldsToSearch, String keyPattern) {
        this(paramKind, operator, fieldsToSearch, keyPattern, null, null, null);
    }

    ResourceParameter(ParamKind paramKind, FieldOperator operator, String fieldsToSearch, boolean isNested) {
        this(paramKind, operator, fieldsToSearch, null, null, null, isNested);
    }

    ResourceParameter(ParamKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern,
                      String valuePattern, Float boost, Boolean isNested) {

        this.key = this.name().toLowerCase(Locale.getDefault());
        this.fieldOperator = operator;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
            ? fieldsToSearch.split("\\|")
            : new String[] {key};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern = nonNull(keyPattern)
            ? keyPattern
            : PATTERN_IS_IGNORE_CASE + key.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.paramkind = kind;
        this.isNested = !isNull(isNested) && isNested;
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
    public Boolean isNested() {
        return isNested;
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

    public static ResourceParameter keyFromString(String paramName) {
        var result = Arrays.stream(ResourceParameter.values())
            .filter(ResourceParameter::ignoreInvalidKey)
            .filter(ParameterKey.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    private static boolean ignoreInvalidKey(ResourceParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(ResourceParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX && enumParameter.ordinal() < SEARCH_ALL.ordinal();
    }
}