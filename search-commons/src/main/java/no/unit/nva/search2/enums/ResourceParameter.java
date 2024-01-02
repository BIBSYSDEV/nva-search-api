package no.unit.nva.search2.enums;

import no.unit.nva.search2.constant.Words;
import nva.commons.core.JacocoGenerated;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CATEGORY_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CATEGORY_NOT_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_CATEGORY_SHOULD_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_PUBLICATION_YEAR_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search2.constant.ResourceConstants.CONTRIBUTORS_AFFILIATION_ID_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.CONTRIBUTORS_IDENTITY_ID;
import static no.unit.nva.search2.constant.ResourceConstants.CONTRIBUTORS_IDENTITY_NAME_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION;
import static no.unit.nva.search2.constant.ResourceConstants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search2.constant.ResourceConstants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
import static no.unit.nva.search2.constant.ResourceConstants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN;
import static no.unit.nva.search2.constant.ResourceConstants.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER;
import static no.unit.nva.search2.constant.ResourceConstants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS;
import static no.unit.nva.search2.constant.ResourceConstants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.PARENT_PUBLICATION_ID;
import static no.unit.nva.search2.constant.ResourceConstants.PUBLICATION_CONTEXT_ISBN_LIST;
import static no.unit.nva.search2.constant.ResourceConstants.PUBLICATION_CONTEXT_TYPE_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.constant.ResourceConstants.REFERENCE_DOI_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search2.constant.ResourceConstants.VISIBLE_FOR_NON_OWNER;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.CREATED_DATE;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.constant.Words.PROJECTS_ID;
import static no.unit.nva.search2.constant.Words.PUBLISHED_DATE;
import static no.unit.nva.search2.constant.Words.Q;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.KEYWORD;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.TEXT;

/**
 * Enum for all the parameters that can be used to query the search index.
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 *
 */

public enum ResourceParameter implements ParameterKey {
    INVALID(ParamKind.INVALID),
    // Parameters used for filtering
    CONTEXT_TYPE(KEYWORD, MUST, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_NOT(KEYWORD, MUST_NOT, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_SHOULD(KEYWORD, SHOULD, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTRIBUTOR(KEYWORD, MUST, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null),
    CONTRIBUTOR_NOT(KEYWORD, MUST_NOT, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null),
    CONTRIBUTOR_SHOULD(KEYWORD, SHOULD, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null),
    CONTRIBUTOR_NAME(TEXT, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    CONTRIBUTOR_NAME_NOT(TEXT, MUST_NOT, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    CONTRIBUTOR_NAME_SHOULD(TEXT, SHOULD, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    CREATED_BEFORE(ParamKind.DATE, FieldOperator.LESS_THAN, CREATED_DATE),
    CREATED_SINCE(ParamKind.DATE, FieldOperator.GREATER_THAN_OR_EQUAL_TO, CREATED_DATE),
    DOI(KEYWORD, REFERENCE_DOI_KEYWORD),
    DOI_NOT(KEYWORD, MUST_NOT, REFERENCE_DOI_KEYWORD),
    DOI_SHOULD(TEXT, SHOULD, REFERENCE_DOI_KEYWORD),
    FUNDING(KEYWORD, MUST, FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER, null,
            PATTERN_IS_FUNDING, null),
    FUNDING_SOURCE(TEXT, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(TEXT, MUST_NOT, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(TEXT, SHOULD, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    HAS_FILE(ParamKind.BOOLEAN, MUST, VISIBLE_FOR_NON_OWNER),
    HAS_FILE_SHOULD(ParamKind.BOOLEAN, SHOULD, VISIBLE_FOR_NON_OWNER),
    ID(KEYWORD, IDENTIFIER_KEYWORD),
    ID_NOT(KEYWORD, MUST_NOT, IDENTIFIER_KEYWORD),
    ID_SHOULD(TEXT, SHOULD, IDENTIFIER_KEYWORD),
    INSTANCE_TYPE(KEYWORD, MUST, PUBLICATION_INSTANCE_TYPE,
                  PATTERN_IS_CATEGORY_KEYS, null, null),
    INSTANCE_TYPE_NOT(KEYWORD, MUST_NOT, PUBLICATION_INSTANCE_TYPE,
                      PATTERN_IS_CATEGORY_NOT_KEYS, null, null),
    INSTANCE_TYPE_SHOULD(KEYWORD, SHOULD, PUBLICATION_INSTANCE_TYPE,
                         PATTERN_IS_CATEGORY_SHOULD_KEYS, null, null),
    INSTITUTION(TEXT, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_NOT(TEXT, MUST_NOT, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_SHOULD(TEXT, SHOULD, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    ISBN(TEXT, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_NOT(TEXT, MUST_NOT, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(TEXT, SHOULD, PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(TEXT, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_NOT(TEXT, MUST_NOT, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_SHOULD(TEXT, SHOULD, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ORCID(KEYWORD, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_NOT(KEYWORD, MUST_NOT, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_SHOULD(TEXT, SHOULD, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    MODIFIED_BEFORE(ParamKind.DATE, FieldOperator.LESS_THAN, MODIFIED_DATE),
    MODIFIED_SINCE(ParamKind.DATE, FieldOperator.GREATER_THAN_OR_EQUAL_TO, MODIFIED_DATE),
    PARENT_PUBLICATION(KEYWORD, MUST, PARENT_PUBLICATION_ID),
    PARENT_PUBLICATION_SHOULD(TEXT, SHOULD, PARENT_PUBLICATION_ID),
    PROJECT(KEYWORD, PROJECTS_ID),
    PROJECT_NOT(KEYWORD, MUST_NOT, PROJECTS_ID),
    PROJECT_SHOULD(KEYWORD, SHOULD, PROJECTS_ID),
    PUBLISHED_BEFORE(ParamKind.DATE, FieldOperator.LESS_THAN, PUBLISHED_DATE),
    PUBLISHED_SINCE(ParamKind.DATE, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLISHED_DATE),
    TITLE(TEXT, ENTITY_DESCRIPTION_MAIN_TITLE, 2F),
    TITLE_NOT(TEXT, MUST_NOT, ENTITY_DESCRIPTION_MAIN_TITLE),
    TITLE_SHOULD(TEXT, SHOULD, ENTITY_DESCRIPTION_MAIN_TITLE),
    TOP_LEVEL_ORGANIZATION(KEYWORD, MUST, TOP_LEVEL_ORGANIZATIONS + DOT + Words.ID + DOT + Words.KEYWORD),
    UNIT(KEYWORD, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_NOT(KEYWORD, MUST_NOT, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_SHOULD(TEXT, SHOULD, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    USER(KEYWORD, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_NOT(KEYWORD, MUST_NOT, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_SHOULD(TEXT, SHOULD, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_AFFILIATION(KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_NOT(KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_SHOULD(TEXT, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    PUBLICATION_YEAR(NUMBER, MUST, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                     PATTERN_IS_PUBLICATION_YEAR_KEYS, null, null),
    PUBLICATION_YEAR_SHOULD(NUMBER, SHOULD, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                            PATTERN_IS_PUBLICATION_YEAR_SHOULD_KEYS, null, null),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, MUST, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    FIELDS(ParamKind.CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SORT(ParamKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(ParamKind.CUSTOM, MUST, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    SEARCH_AFTER(ParamKind.CUSTOM),
    // ignored parameter
    LANG(ParamKind.CUSTOM);

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

    ResourceParameter(ParamKind kind) {
        this(kind, MUST, null, null, null, null);
    }

    ResourceParameter(ParamKind kind, String fieldsToSearch) {
        this(kind, MUST, fieldsToSearch, null, null, null);
    }

    ResourceParameter(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, MUST, fieldsToSearch, null, null, boost);
    }

    ResourceParameter(ParamKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ResourceParameter(
        ParamKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern, String valuePattern,
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