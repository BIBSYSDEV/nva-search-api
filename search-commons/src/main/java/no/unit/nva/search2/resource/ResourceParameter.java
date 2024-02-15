package no.unit.nva.search2.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.enums.FieldOperator;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ParameterKind;
import no.unit.nva.search2.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_CATEGORY_KEYS;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_CATEGORY_NOT_KEYS;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_CATEGORY_SHOULD_KEYS;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.CREATED_DATE;
import static no.unit.nva.search2.common.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.common.constant.Words.PROJECTS_ID;
import static no.unit.nva.search2.common.constant.Words.PUBLISHED_DATE;
import static no.unit.nva.search2.common.constant.Words.Q;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.common.enums.FieldOperator.ALL;
import static no.unit.nva.search2.common.enums.FieldOperator.ANY;
import static no.unit.nva.search2.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search2.common.enums.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.common.enums.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.common.enums.FieldOperator.NONE;
import static no.unit.nva.search2.common.enums.ParameterKind.BOOLEAN;
import static no.unit.nva.search2.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search2.common.enums.ParameterKind.DATE;
import static no.unit.nva.search2.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.FUZZY_TEXT;
import static no.unit.nva.search2.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search2.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search2.resource.Constants.ASSOCIATED_ARTIFACTS_LICENSE;
import static no.unit.nva.search2.resource.Constants.ATTACHMENT_VISIBLE_FOR_NON_OWNER;
import static no.unit.nva.search2.resource.Constants.CONTRIBUTORS_AFFILIATION_ID_KEYWORD;
import static no.unit.nva.search2.resource.Constants.CONTRIBUTORS_IDENTITY_ID;
import static no.unit.nva.search2.resource.Constants.CONTRIBUTORS_IDENTITY_NAME_KEYWORD;
import static no.unit.nva.search2.resource.Constants.CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD;
import static no.unit.nva.search2.resource.Constants.COURSE_CODE_KEYWORD;
import static no.unit.nva.search2.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN;
import static no.unit.nva.search2.resource.Constants.ENTITY_DESCRIPTION_REFERENCE_SERIES;
import static no.unit.nva.search2.resource.Constants.ENTITY_TAGS;
import static no.unit.nva.search2.resource.Constants.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER;
import static no.unit.nva.search2.resource.Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS;
import static no.unit.nva.search2.resource.Constants.HANDLE_KEYWORD;
import static no.unit.nva.search2.resource.Constants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.resource.Constants.PARENT_PUBLICATION_ID;
import static no.unit.nva.search2.resource.Constants.PUBLICATION_CONTEXT_ISBN_LIST;
import static no.unit.nva.search2.resource.Constants.PUBLICATION_CONTEXT_PUBLISHER;
import static no.unit.nva.search2.resource.Constants.PUBLICATION_CONTEXT_TYPE_KEYWORD;
import static no.unit.nva.search2.resource.Constants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.resource.Constants.PUBLICATION_STATUS;
import static no.unit.nva.search2.resource.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search2.resource.Constants.REFERENCE_DOI_KEYWORD;
import static no.unit.nva.search2.resource.Constants.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD;
import static no.unit.nva.search2.resource.Constants.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search2.resource.Constants.TOP_LEVEL_ORG_ID;
import static no.unit.nva.search2.resource.ResourceQuery.PHI;
import static no.unit.nva.search2.resource.ResourceQuery.PI;

/**
 * Enum for all the parameters that can be used to query the search index.
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 *
 */

public enum ResourceParameter implements ParameterKey {
    INVALID(ParameterKind.INVALID),
    // Parameters used for filtering
    ABSTRACT(FUZZY_TEXT, ENTITY_ABSTRACT),
    ABSTRACT_NOT(TEXT, NONE, ENTITY_ABSTRACT),
    ABSTRACT_SHOULD(FUZZY_TEXT, ANY, ENTITY_ABSTRACT),
    CONTEXT_TYPE(KEYWORD, ALL, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_NOT(KEYWORD, NONE, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_SHOULD(FUZZY_KEYWORD, ANY, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTRIBUTOR(FUZZY_KEYWORD, ALL, CONTRIBUTORS_IDENTITY_ID, PHI),
    CONTRIBUTOR_NOT(KEYWORD, NONE, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null),
    CONTRIBUTOR_SHOULD(FUZZY_KEYWORD, ANY, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, PHI),
    CONTRIBUTOR_NAME(FUZZY_KEYWORD, ALL, CONTRIBUTORS_IDENTITY_NAME_KEYWORD, PHI),
    CONTRIBUTOR_NAME_NOT(FUZZY_KEYWORD, NONE, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    CONTRIBUTOR_NAME_SHOULD(FUZZY_KEYWORD, ANY, CONTRIBUTORS_IDENTITY_NAME_KEYWORD, PHI),
    COURSE(KEYWORD, ALL, COURSE_CODE_KEYWORD),
    COURSE_NOT(KEYWORD, NONE, COURSE_CODE_KEYWORD),
    COURSE_SHOULD(FUZZY_KEYWORD, ANY, COURSE_CODE_KEYWORD),
    CREATED_BEFORE(DATE, LESS_THAN, CREATED_DATE),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, CREATED_DATE),
    CRISTIN_IDENTIFIER(KEYWORD),
    DOI(KEYWORD, ANY, REFERENCE_DOI_KEYWORD),
    DOI_NOT(KEYWORD, NONE, REFERENCE_DOI_KEYWORD),
    DOI_SHOULD(TEXT, ANY, REFERENCE_DOI_KEYWORD),
    FUNDING(KEYWORD, ALL, FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER, null, PATTERN_IS_FUNDING, null),
    FUNDING_SOURCE(TEXT, ALL, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(TEXT, NONE, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(TEXT, ANY, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    HANDLE(KEYWORD, ALL, HANDLE_KEYWORD),
    HANDLE_NOT(KEYWORD, NONE, HANDLE_KEYWORD),
    HANDLE_SHOULD(KEYWORD, ANY, HANDLE_KEYWORD),
    HAS_FILE(BOOLEAN, ALL, ATTACHMENT_VISIBLE_FOR_NON_OWNER),
    HAS_FILE_SHOULD(BOOLEAN, ANY, ATTACHMENT_VISIBLE_FOR_NON_OWNER),
    ID(KEYWORD, IDENTIFIER_KEYWORD),
    ID_NOT(KEYWORD, NONE, IDENTIFIER_KEYWORD),
    ID_SHOULD(TEXT, ANY, IDENTIFIER_KEYWORD),
    INSTANCE_TYPE(KEYWORD, ALL, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_KEYS, null, 1F),
    INSTANCE_TYPE_NOT(KEYWORD, NONE, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_NOT_KEYS, null, 1F),
    INSTANCE_TYPE_SHOULD(KEYWORD, ANY, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_SHOULD_KEYS, null, 1F),
    INSTITUTION(TEXT, ALL, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_NOT(TEXT, NONE, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_SHOULD(TEXT, ANY, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    ISBN(FUZZY_KEYWORD, ANY, PUBLICATION_CONTEXT_ISBN_LIST, PHI),
    ISBN_NOT(FUZZY_KEYWORD, NONE, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(TEXT, ANY, PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(FUZZY_KEYWORD, ANY, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN, PHI),
    ISSN_NOT(FUZZY_KEYWORD, NONE, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_SHOULD(TEXT, ANY, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    LICENSE(KEYWORD, ALL, ASSOCIATED_ARTIFACTS_LICENSE),
    LICENSE_NOT(KEYWORD, NONE, ASSOCIATED_ARTIFACTS_LICENSE),
    LICENSE_SHOULD(KEYWORD, ANY, ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED_BEFORE(DATE, LESS_THAN, MODIFIED_DATE),
    MODIFIED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, MODIFIED_DATE),
    ORCID(KEYWORD, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_NOT(KEYWORD, NONE, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_SHOULD(TEXT, ANY, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    PARENT_PUBLICATION(KEYWORD, ALL, PARENT_PUBLICATION_ID),
    PARENT_PUBLICATION_SHOULD(TEXT, ANY, PARENT_PUBLICATION_ID),
    PROJECT(KEYWORD, PROJECTS_ID),
    PROJECT_NOT(KEYWORD, NONE, PROJECTS_ID),
    PROJECT_SHOULD(KEYWORD, ANY, PROJECTS_ID),
    PUBLICATION_YEAR_BEFORE(NUMBER, LESS_THAN, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLICATION_YEAR_SHOULD(NUMBER, ANY, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                            PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS, null, null),
    PUBLICATION_YEAR_SINCE(NUMBER, GREATER_THAN_OR_EQUAL_TO, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLISHED_BEFORE(DATE, LESS_THAN, PUBLISHED_DATE),
    PUBLISHED_BETWEEN(DATE, BETWEEN, PUBLISHED_DATE),
    PUBLISHED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, PUBLISHED_DATE),
    PUBLISHER(FUZZY_KEYWORD, ALL, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_NOT(FUZZY_KEYWORD, NONE, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_ID(FUZZY_KEYWORD, ALL, PUBLISHER_ID_KEYWORD),
    PUBLISHER_ID_NOT(FUZZY_KEYWORD, NONE, PUBLISHER_ID_KEYWORD),
    PUBLISHER_ID_SHOULD(TEXT, ANY, PUBLISHER_ID_KEYWORD),
    SCOPUS_IDENTIFIER(KEYWORD),
    SERIES(FUZZY_KEYWORD, ALL, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    SERIES_NOT(FUZZY_KEYWORD, NONE, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    SERIES_SHOULD(FUZZY_KEYWORD, ANY, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    STATUS(KEYWORD, ALL, PUBLICATION_STATUS),
    STATUS_NOT(KEYWORD, NONE, PUBLICATION_STATUS),
    STATUS_SHOULD(KEYWORD, ANY, PUBLICATION_STATUS),
    TAGS(TEXT, ENTITY_TAGS),
    TAGS_NOT(TEXT, NONE, ENTITY_TAGS),
    TAGS_SHOULD(TEXT, ANY, ENTITY_TAGS),
    TITLE(FUZZY_TEXT, ALL, ENTITY_DESCRIPTION_MAIN_TITLE, PI),
    TITLE_NOT(TEXT, NONE, ENTITY_DESCRIPTION_MAIN_TITLE),
    TITLE_SHOULD(FUZZY_TEXT, ANY, ENTITY_DESCRIPTION_MAIN_TITLE),
    TOP_LEVEL_ORGANIZATION(KEYWORD, ALL, TOP_LEVEL_ORG_ID),
    UNIT(KEYWORD, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_NOT(KEYWORD, NONE, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_SHOULD(TEXT, ANY, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    USER(KEYWORD, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_AFFILIATION(KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_NOT(KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_SHOULD(TEXT, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_NOT(KEYWORD, NONE, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_SHOULD(TEXT, ANY, RESOURCE_OWNER_OWNER_KEYWORD),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, ALL, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    FIELDS(CUSTOM),
    // Pagination parameters
    AGGREGATION(CUSTOM),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SORT(ParameterKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(CUSTOM, ALL, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
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
    private final ParameterKind paramkind;
    private final Float boost;

    ResourceParameter(ParameterKind kind) {
        this(kind, ALL, null, null, null, null);
    }

    ResourceParameter(ParameterKind kind, String fieldsToSearch) {
        this(kind, ALL, fieldsToSearch, null, null, null);
    }

    ResourceParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ResourceParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch, Float boost) {
        this(kind, operator, fieldsToSearch, null, null, boost);
    }


    ResourceParameter(
        ParameterKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern, String valuePattern,
        Float boost) {

        this.key = this.name().toLowerCase(Locale.getDefault());
        this.fieldOperator = operator;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
            ? fieldsToSearch.split("\\|")
            : new String[]{key};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern = nonNull(keyPattern)
            ? keyPattern
            : PATTERN_IS_IGNORE_CASE + key.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
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
    public ParameterKind fieldType() {
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