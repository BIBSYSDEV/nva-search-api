package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.CHAR_UNDERSCORE;
import static no.unit.nva.constants.Words.COLON;
import static no.unit.nva.constants.Words.CREATED_DATE;
import static no.unit.nva.constants.Words.MODIFIED_DATE;
import static no.unit.nva.constants.Words.PHI;
import static no.unit.nva.constants.Words.PI;
import static no.unit.nva.constants.Words.PROJECTS_ID;
import static no.unit.nva.constants.Words.PUBLISHED_DATE;
import static no.unit.nva.constants.Words.Q;
import static no.unit.nva.constants.Words.UNDERSCORE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_CATEGORY_KEYS;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_CATEGORY_NOT_KEYS;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FUNDING_IDENTIFIER;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FUNDING_IDENTIFIER_NOT;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FUNDING_IDENTIFIER_SHOULD;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search.common.enums.FieldOperator.ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.ANY_OF;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search.common.enums.FieldOperator.LESS_THAN;
import static no.unit.nva.search.common.enums.FieldOperator.NA;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ANY_OF;
import static no.unit.nva.search.common.enums.ParameterKind.ACROSS_FIELDS;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.DATE;
import static no.unit.nva.search.common.enums.ParameterKind.EXISTS;
import static no.unit.nva.search.common.enums.ParameterKind.FLAG;
import static no.unit.nva.search.common.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.HAS_PARTS;
import static no.unit.nva.search.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search.common.enums.ParameterKind.PART_OF;
import static no.unit.nva.search.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search.resource.Constants.ASSOCIATED_ARTIFACTS_LICENSE;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_AFFILIATION_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_FIELDS;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_IDENTITY_ID;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_IDENTITY_NAME_KEYWORD;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.CONTRIBUTOR_COUNT_NO_KEYWORD;
import static no.unit.nva.search.resource.Constants.COURSE_CODE_KEYWORD;
import static no.unit.nva.search.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_LANGUAGE;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_PUBLICATION_PAGES;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_REFERENCE_CONTEXT_REFERENCE;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_REFERENCE_JOURNAL;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_REFERENCE_SERIES;
import static no.unit.nva.search.resource.Constants.ENTITY_TAGS;
import static no.unit.nva.search.resource.Constants.FILES_STATUS_KEYWORD;
import static no.unit.nva.search.resource.Constants.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER;
import static no.unit.nva.search.resource.Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS;
import static no.unit.nva.search.resource.Constants.FUNDING_IDENTIFIER_KEYWORD;
import static no.unit.nva.search.resource.Constants.HANDLE_KEYWORD;
import static no.unit.nva.search.resource.Constants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search.resource.Constants.PARENT_PUBLICATION_ID;
import static no.unit.nva.search.resource.Constants.PUBLICATION_CONTEXT_ISBN_LIST;
import static no.unit.nva.search.resource.Constants.PUBLICATION_CONTEXT_PUBLISHER;
import static no.unit.nva.search.resource.Constants.PUBLICATION_CONTEXT_TYPE_KEYWORD;
import static no.unit.nva.search.resource.Constants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search.resource.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.REFERENCE_DOI_KEYWORD;
import static no.unit.nva.search.resource.Constants.REFERENCE_PUBLICATION_CONTEXT_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD;
import static no.unit.nva.search.resource.Constants.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_INDEX_STATUS_KEYWORD;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_INDEX_YEAR;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_LEVEL_SEARCH_FIELD;
import static no.unit.nva.search.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.resource.Constants.SUBJECTS;
import static no.unit.nva.search.resource.Constants.TOP_LEVEL_ORG_ID;

import static java.util.Objects.nonNull;

import no.unit.nva.search.common.enums.FieldOperator;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.ParameterKind;
import no.unit.nva.search.common.enums.ValueEncoding;

import nva.commons.core.JacocoGenerated;

import org.apache.commons.text.CaseUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to
 * implement these parameters <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin
 * API</a>
 *
 * @author Stig Norland
 * @author Kir Truhacev
 * @author Joachim Jorgensen
 */
@SuppressWarnings({"PMD.ExcessivePublicCount"})
public enum ResourceParameter implements ParameterKey<ResourceParameter> {
    INVALID(ParameterKind.INVALID),
    // Parameters used for filtering
    ABSTRACT(TEXT, ALL_OF, ENTITY_ABSTRACT),
    ABSTRACT_OF_CHILD(HAS_PARTS, ALL_OF, ABSTRACT),
    ABSTRACT_NOT(TEXT, NOT_ALL_OF, ENTITY_ABSTRACT),
    ABSTRACT_SHOULD(TEXT, ANY_OF, ENTITY_ABSTRACT),
    CONTEXT_TYPE(KEYWORD, ALL_OF, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_OF_NO_CHILD(HAS_PARTS, NOT_ANY_OF, null, null, null, null, CONTEXT_TYPE),
    CONTEXT_TYPE_NOT(KEYWORD, NOT_ALL_OF, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_SHOULD(KEYWORD, ANY_OF, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTRIBUTORS(ACROSS_FIELDS, ALL_OF, CONTRIBUTORS_FIELDS),
    CONTRIBUTORS_OF_CHILD(HAS_PARTS, ALL_OF, CONTRIBUTORS),
    CONTRIBUTOR(KEYWORD, ALL_OF, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI),
    CONTRIBUTOR_COUNT(NUMBER, BETWEEN, CONTRIBUTOR_COUNT_NO_KEYWORD),
    CONTRIBUTOR_NOT(KEYWORD, NOT_ALL_OF, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI),
    CONTRIBUTOR_SHOULD(KEYWORD, ANY_OF, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI),
    CONTRIBUTOR_NAME(FUZZY_KEYWORD, ALL_OF, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    CONTRIBUTOR_NAME_NOT(FUZZY_KEYWORD, NOT_ALL_OF, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    CONTRIBUTOR_NAME_SHOULD(TEXT, ANY_OF, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    COURSE(TEXT, ALL_OF, COURSE_CODE_KEYWORD),
    COURSE_NOT(TEXT, NOT_ALL_OF, COURSE_CODE_KEYWORD),
    COURSE_SHOULD(TEXT, ANY_OF, COURSE_CODE_KEYWORD),
    CREATED_BEFORE(DATE, LESS_THAN, CREATED_DATE),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, CREATED_DATE),
    CRISTIN_IDENTIFIER(CUSTOM),
    DOI(FUZZY_KEYWORD, REFERENCE_DOI_KEYWORD),
    DOI_NOT(FUZZY_KEYWORD, NOT_ALL_OF, REFERENCE_DOI_KEYWORD),
    DOI_SHOULD(TEXT, ANY_OF, REFERENCE_DOI_KEYWORD),
    /** excludeSubUnits holds path to hierarchical search, used by several keys. */
    EXCLUDE_SUBUNITS(FLAG, Constants.CONTRIBUTOR_ORG_KEYWORD),
    FUNDING(
            CUSTOM,
            ALL_OF,
            FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER,
            null,
            PATTERN_IS_FUNDING),
    FUNDING_IDENTIFIER(KEYWORD, ALL_OF, FUNDING_IDENTIFIER_KEYWORD, PATTERN_IS_FUNDING_IDENTIFIER),
    FUNDING_IDENTIFIER_NOT(
            KEYWORD, NOT_ALL_OF, FUNDING_IDENTIFIER_KEYWORD, PATTERN_IS_FUNDING_IDENTIFIER_NOT),
    FUNDING_IDENTIFIER_SHOULD(
            FUZZY_KEYWORD,
            ANY_OF,
            FUNDING_IDENTIFIER_KEYWORD,
            PATTERN_IS_FUNDING_IDENTIFIER_SHOULD),
    FUNDING_SOURCE(TEXT, ALL_OF, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(TEXT, NOT_ALL_OF, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(TEXT, ANY_OF, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    HANDLE(FUZZY_KEYWORD, ANY_OF, HANDLE_KEYWORD, PHI),
    HANDLE_OF_PARENT(PART_OF, ALL_OF, HANDLE),
    HANDLE_NOT(FUZZY_KEYWORD, NOT_ANY_OF, HANDLE_KEYWORD, PHI),
    FILES(KEYWORD, ALL_OF, FILES_STATUS_KEYWORD),
    ID(KEYWORD, ANY_OF, IDENTIFIER_KEYWORD),
    ID_NOT(KEYWORD, NOT_ANY_OF, IDENTIFIER_KEYWORD),
    ID_SHOULD(TEXT, ANY_OF, IDENTIFIER_KEYWORD),
    INSTANCE_TYPE(KEYWORD, ANY_OF, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_KEYS),
    INSTANCE_TYPE_NOT(KEYWORD, NOT_ANY_OF, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_NOT_KEYS),
    INSTANCE_TYPE_OF_PARENT(PART_OF, ALL_OF, INSTANCE_TYPE),
    INSTANCE_TYPE_OF_CHILD(HAS_PARTS, ALL_OF, INSTANCE_TYPE),
    INSTITUTION(TEXT, ALL_OF, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_NOT(TEXT, NOT_ALL_OF, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_SHOULD(TEXT, ANY_OF, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    ISBN(KEYWORD, ANY_OF, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_NOT(KEYWORD, NOT_ANY_OF, PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(FUZZY_KEYWORD, ANY_OF, PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(KEYWORD, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_NOT(KEYWORD, NOT_ANY_OF, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_SHOULD(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    JOURNAL(FUZZY_KEYWORD, ALL_OF, ENTITY_DESCRIPTION_REFERENCE_JOURNAL),
    JOURNAL_NOT(FUZZY_KEYWORD, NOT_ALL_OF, ENTITY_DESCRIPTION_REFERENCE_JOURNAL),
    JOURNAL_SHOULD(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_JOURNAL),
    LICENSE(FUZZY_KEYWORD, ALL_OF, ASSOCIATED_ARTIFACTS_LICENSE),
    LICENSE_NOT(FUZZY_KEYWORD, NOT_ALL_OF, ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED_BEFORE(DATE, LESS_THAN, MODIFIED_DATE),
    MODIFIED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, MODIFIED_DATE),
    ORCID(KEYWORD, ALL_OF, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_NOT(KEYWORD, NOT_ALL_OF, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    ORCID_SHOULD(TEXT, ANY_OF, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    PARENT_PUBLICATION(KEYWORD, ANY_OF, PARENT_PUBLICATION_ID),
    PARENT_PUBLICATION_EXIST(EXISTS, ANY_OF, PARENT_PUBLICATION_ID),
    PROJECT(KEYWORD, ANY_OF, PROJECTS_ID),
    PROJECT_NOT(KEYWORD, NOT_ANY_OF, PROJECTS_ID),
    PROJECT_SHOULD(FUZZY_KEYWORD, ANY_OF, PROJECTS_ID, PHI),
    PUBLICATION_LANGUAGE(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_LANGUAGE),
    PUBLICATION_LANGUAGE_NOT(FUZZY_KEYWORD, NOT_ANY_OF, ENTITY_DESCRIPTION_LANGUAGE),
    PUBLICATION_LANGUAGE_SHOULD(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_LANGUAGE),
    PUBLICATION_PAGES(NUMBER, BETWEEN, ENTITY_DESCRIPTION_PUBLICATION_PAGES),
    PUBLICATION_PAGES_EXISTS(EXISTS, ANY_OF, ENTITY_DESCRIPTION_PUBLICATION_PAGES),
    PUBLICATION_YEAR(NUMBER, BETWEEN, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLICATION_YEAR_BEFORE(NUMBER, LESS_THAN, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLICATION_YEAR_SINCE(
            NUMBER, GREATER_THAN_OR_EQUAL_TO, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLISHED_BEFORE(DATE, LESS_THAN, PUBLISHED_DATE),
    PUBLISHED_BETWEEN(DATE, BETWEEN, PUBLISHED_DATE),
    PUBLISHED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, PUBLISHED_DATE),
    PUBLISHER(FUZZY_KEYWORD, ALL_OF, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_NOT(FUZZY_KEYWORD, NOT_ALL_OF, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_SHOULD(FUZZY_KEYWORD, ANY_OF, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_ID(FUZZY_KEYWORD, ANY_OF, PUBLISHER_ID_KEYWORD),
    PUBLISHER_ID_NOT(FUZZY_KEYWORD, NOT_ANY_OF, PUBLISHER_ID_KEYWORD),
    PUBLISHER_ID_SHOULD(TEXT, ANY_OF, PUBLISHER_ID_KEYWORD),
    //    TODO commented away, need to deploy soon due to bug
    //    REFERENCED(ACROSS_FIELDS, ANY_OF, REFERENCE_PUBLICATION),
    REFERENCE_CONTEXT_REFERENCE_EXISTS(
            EXISTS, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_CONTEXT_REFERENCE),
    REFERENCE_EXISTS(CUSTOM, ALL_OF, ENTITY_DESCRIPTION_REFERENCE_CONTEXT_REFERENCE),
    REFERENCED_ID(FUZZY_KEYWORD, ANY_OF, REFERENCE_PUBLICATION_CONTEXT_ID_KEYWORD),
    SCIENTIFIC_VALUE(KEYWORD, ANY_OF, SCIENTIFIC_LEVEL_SEARCH_FIELD),
    SCIENTIFIC_VALUE_OF_CHILD(HAS_PARTS, ANY_OF, SCIENTIFIC_VALUE),
    SCIENTIFIC_INDEX_STATUS(KEYWORD, ANY_OF, SCIENTIFIC_INDEX_STATUS_KEYWORD),
    SCIENTIFIC_INDEX_STATUS_NOT(KEYWORD, NOT_ANY_OF, SCIENTIFIC_INDEX_STATUS_KEYWORD),
    SCIENTIFIC_REPORT_PERIOD(NUMBER, BETWEEN, SCIENTIFIC_INDEX_YEAR),
    SCIENTIFIC_REPORT_PERIOD_SINCE(NUMBER, GREATER_THAN_OR_EQUAL_TO, SCIENTIFIC_INDEX_YEAR),
    SCIENTIFIC_REPORT_PERIOD_BEFORE(NUMBER, LESS_THAN, SCIENTIFIC_INDEX_YEAR),
    SCOPUS_IDENTIFIER(CUSTOM),
    SERIES(FUZZY_KEYWORD, ALL_OF, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    SERIES_NOT(FUZZY_KEYWORD, NOT_ALL_OF, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    SERIES_SHOULD(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    STATISTICS(FLAG),
    STATUS(KEYWORD, ANY_OF, STATUS_KEYWORD),
    STATUS_NOT(KEYWORD, NOT_ANY_OF, STATUS_KEYWORD),
    TAGS(TEXT, ALL_OF, ENTITY_TAGS),
    TAGS_NOT(TEXT, NOT_ALL_OF, ENTITY_TAGS),
    TAGS_SHOULD(TEXT, ANY_OF, ENTITY_TAGS),
    TAGS_EXISTS(EXISTS, ANY_OF, ENTITY_TAGS),
    TITLE(TEXT, ALL_OF, ENTITY_DESCRIPTION_MAIN_TITLE, PI),
    TITLE_NOT(TEXT, NOT_ALL_OF, ENTITY_DESCRIPTION_MAIN_TITLE),
    TITLE_SHOULD(TEXT, ANY_OF, ENTITY_DESCRIPTION_MAIN_TITLE),
    TOP_LEVEL_ORGANIZATION(CUSTOM, ANY_OF, TOP_LEVEL_ORG_ID),
    UNIDENTIFIED_CONTRIBUTOR_INSTITUTION(CUSTOM, ALL_OF, CONTRIBUTORS_FIELDS),
    UNIDENTIFIED_NORWEGIAN(CUSTOM, ALL_OF, CONTRIBUTORS_FIELDS),
    UNIT(CUSTOM, ALL_OF, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_NOT(CUSTOM, NOT_ALL_OF, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    UNIT_SHOULD(TEXT, ANY_OF, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    USER(KEYWORD, ANY_OF, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_NOT(KEYWORD, NOT_ANY_OF, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_SHOULD(TEXT, ANY_OF, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_AFFILIATION(KEYWORD, ANY_OF, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_NOT(KEYWORD, NOT_ANY_OF, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_SHOULD(TEXT, ANY_OF, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    VOCABULARY(FUZZY_KEYWORD, ALL_OF, SUBJECTS),
    VOCABULARY_EXISTS(EXISTS, ANY_OF, SUBJECTS),
    SEARCH_ALL(CUSTOM, ANY_OF, Q, PATTERN_IS_SEARCH_ALL_KEY),
    GET_ALL(FREE_TEXT),
    HAS_CHILDREN(HAS_PARTS, ALL_OF, GET_ALL),
    HAS_NO_CHILDREN(HAS_PARTS, NOT_ALL_OF, GET_ALL),
    HAS_PARENT(PART_OF, ALL_OF, GET_ALL),
    HAS_NO_PARENT(PART_OF, NOT_ALL_OF, GET_ALL),
    // Query parameters passed to SWS/Opensearch
    AGGREGATION(FLAG),
    NODES_SEARCHED(FLAG, null, null, PATTERN_IS_FIELDS_SEARCHED),
    NODES_INCLUDED(FLAG),
    NODES_EXCLUDED(FLAG),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY),
    SEARCH_AFTER(FLAG),
    SORT(ParameterKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY),
    SORT_ORDER(FLAG, ALL_OF, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameter> RESOURCE_PARAMETER_SET =
            Arrays.stream(values())
                    .filter(ResourceParameter::isSearchField)
                    .sorted(ParameterKey::compareAscending)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
    private final ValueEncoding encoding;
    private final String keyPattern;
    private final String validValuePattern;
    private final String[] fieldsToSearch;
    private final FieldOperator fieldOperator;
    private final String errorMsg;
    private final ParameterKind paramkind;
    private final Float boost;
    private final ResourceParameter subQueryReference;

    ResourceParameter(ParameterKind kind) {
        this(kind, ALL_OF, null, null, null, null, null);
    }

    ResourceParameter(
            ParameterKind kind,
            FieldOperator operator,
            String fieldsToSearch,
            String keyPattern,
            String valuePattern,
            Float boost,
            ResourceParameter subquery) {

        this.fieldOperator = nonNull(operator) ? operator : NA;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch =
                nonNull(fieldsToSearch) ? fieldsToSearch.split("\\|") : new String[] {name()};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern =
                nonNull(keyPattern)
                        ? keyPattern
                        : PATTERN_IS_IGNORE_CASE
                                + name().replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.paramkind = kind;
        this.subQueryReference = subquery;
    }

    ResourceParameter(ParameterKind kind, String fieldsToSearch) {
        this(kind, ALL_OF, fieldsToSearch, null, null, null, null);
    }

    ResourceParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null, null);
    }

    ResourceParameter(ParameterKind kind, FieldOperator operator, ResourceParameter keys) {
        this(kind, operator, null, null, null, null, keys);
    }

    ResourceParameter(
            ParameterKind kind, FieldOperator operator, String fieldsToSearch, Float boost) {
        this(kind, operator, fieldsToSearch, null, null, boost, null);
    }

    ResourceParameter(
            ParameterKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern) {
        this(kind, operator, fieldsToSearch, keyPattern, null, null, null);
    }

    ResourceParameter(
            ParameterKind kind,
            FieldOperator operator,
            String fieldsToSearch,
            String keyPattern,
            String validValuePattern) {
        this(kind, operator, fieldsToSearch, keyPattern, validValuePattern, null, null);
    }

    public static ResourceParameter keyFromString(String paramName) {
        var result =
                Arrays.stream(values())
                        .filter(ResourceParameter::ignoreInvalidKey)
                        .filter(ParameterKey.equalTo(paramName))
                        .collect(Collectors.toSet());
        return result.size() == 1 ? result.stream().findFirst().get() : INVALID;
    }

    private static boolean ignoreInvalidKey(ResourceParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(ResourceParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX
                && enumParameter.ordinal() < AGGREGATION.ordinal();
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return new StringJoiner(COLON, "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(asCamelCase())
                .toString();
    }

    @Override
    public String asCamelCase() {
        return CaseUtils.toCamelCase(this.name(), false, CHAR_UNDERSCORE);
    }

    @Override
    public String asLowerCase() {
        return this.name().toLowerCase(Locale.getDefault());
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
    public Stream<String> searchFields(boolean... isKeyWord) {
        return Arrays.stream(fieldsToSearch).map(ParameterKey.trimKeyword(fieldType(), isKeyWord));
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
    public ResourceParameter subQuery() {
        return subQueryReference;
    }
}
