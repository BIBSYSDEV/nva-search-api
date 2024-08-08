package no.unit.nva.search.resource;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_CATEGORY_KEYS;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FUNDING_IDENTIFIER;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search.common.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search.common.constant.Words.COLON;
import static no.unit.nva.search.common.constant.Words.CONTRIBUTOR_ORGANIZATIONS;
import static no.unit.nva.search.common.constant.Words.CREATED_DATE;
import static no.unit.nva.search.common.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search.common.constant.Words.PHI;
import static no.unit.nva.search.common.constant.Words.PI;
import static no.unit.nva.search.common.constant.Words.PROJECTS_ID;
import static no.unit.nva.search.common.constant.Words.PUBLISHED_DATE;
import static no.unit.nva.search.common.constant.Words.Q;
import static no.unit.nva.search.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search.common.enums.FieldOperator.ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search.common.enums.FieldOperator.LESS_THAN;
import static no.unit.nva.search.common.enums.FieldOperator.NA;
import static no.unit.nva.search.common.enums.FieldOperator.ANY_OF;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ANY_OF;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.DATE;
import static no.unit.nva.search.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.IGNORED;
import static no.unit.nva.search.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search.common.enums.ParameterSubKind.EXIST;
import static no.unit.nva.search.common.enums.ParameterSubKind.MUST_NOT;
import static no.unit.nva.search.common.enums.ParameterSubKind.SHOULD;
import static no.unit.nva.search.common.enums.ParameterSubKind.SUB_KIND_PATTERNS;
import static no.unit.nva.search.resource.Constants.ASSOCIATED_ARTIFACTS_LICENSE;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_AFFILIATION_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_IDENTITY_ID;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_IDENTITY_NAME_KEYWORD;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.COURSE_CODE_KEYWORD;
import static no.unit.nva.search.resource.Constants.ENTITY_ABSTRACT;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_LANGUAGE;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_MAIN_TITLE;
import static no.unit.nva.search.resource.Constants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
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
import static no.unit.nva.search.resource.Constants.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD;
import static no.unit.nva.search.resource.Constants.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_INDEX_STATUS_KEYWORD;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_INDEX_YEAR;
import static no.unit.nva.search.resource.Constants.SCIENTIFIC_LEVEL_SEARCH_FIELD;
import static no.unit.nva.search.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.resource.Constants.TOP_LEVEL_ORG_ID;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.unit.nva.search.common.constant.Words;
import no.unit.nva.search.common.enums.FieldOperator;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.ParameterKind;
import no.unit.nva.search.common.enums.ParameterSubKind;
import no.unit.nva.search.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;
import org.apache.commons.text.CaseUtils;

/**
 * Enum for all the parameters that can be used to query the search index.
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 * @author Stig Norland
 * @author Kir Truhacev
 * @author Joachim Jorgensen
 */
public enum ResourceParameter implements ParameterKey {
    INVALID(ParameterKind.INVALID),
    // Parameters used for filtering
    ABSTRACT(TEXT, ALL_OF, ENTITY_ABSTRACT),
    CONTEXT_TYPE(KEYWORD, ALL_OF, PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTRIBUTOR(KEYWORD, ALL_OF, CONTRIBUTORS_IDENTITY_ID, null, PATTERN_IS_URI, null),
    CONTRIBUTOR_NAME(FUZZY_KEYWORD, ALL_OF, CONTRIBUTORS_IDENTITY_NAME_KEYWORD),
    COURSE(TEXT, ALL_OF, COURSE_CODE_KEYWORD),
    CREATED(DATE, LESS_THAN, CREATED_DATE),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, CREATED_DATE),
    CRISTIN_IDENTIFIER(CUSTOM),
    DOI(FUZZY_KEYWORD, REFERENCE_DOI_KEYWORD),
    /**
     * excludeSubUnits holds path to hierarchical search, used by several keys.
     */
    EXCLUDE_SUBUNITS(IGNORED, jsonPath(CONTRIBUTOR_ORGANIZATIONS, Words.KEYWORD)),
    FUNDING(CUSTOM, ALL_OF, FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER, null, PATTERN_IS_FUNDING, null),
    FUNDING_IDENTIFIER(FUZZY_KEYWORD, ALL_OF, FUNDING_IDENTIFIER_KEYWORD, PATTERN_IS_FUNDING_IDENTIFIER, null, null),
    FUNDING_SOURCE(TEXT, ALL_OF, FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    HANDLE(FUZZY_KEYWORD, ANY_OF, HANDLE_KEYWORD, PHI),
    FILES(KEYWORD, ALL_OF, FILES_STATUS_KEYWORD),
    ID(KEYWORD, ANY_OF, IDENTIFIER_KEYWORD),
    INSTANCE_TYPE(KEYWORD, ANY_OF, PUBLICATION_INSTANCE_TYPE, PATTERN_IS_CATEGORY_KEYS, null, null),
    INSTITUTION(TEXT, ALL_OF, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    ISBN(KEYWORD, ANY_OF, PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(KEYWORD, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    JOURNAL(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_REFERENCE_JOURNAL),
    LICENSE(FUZZY_KEYWORD, ALL_OF, ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED(DATE, BETWEEN, MODIFIED_DATE),
    ORCID(FUZZY_KEYWORD, ALL_OF, CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD),
    PARENT_PUBLICATION(KEYWORD, ANY_OF, PARENT_PUBLICATION_ID),
    PROJECT(FUZZY_KEYWORD, ANY_OF, PROJECTS_ID, PHI),
    PUBLICATION_LANGUAGE(FUZZY_KEYWORD, ANY_OF, ENTITY_DESCRIPTION_LANGUAGE),
    PUBLICATION_YEAR(NUMBER, BETWEEN, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR),
    PUBLISHED(DATE, BETWEEN, PUBLISHED_DATE),
    PUBLISHER(FUZZY_KEYWORD, ALL_OF, PUBLICATION_CONTEXT_PUBLISHER),
    PUBLISHER_ID(FUZZY_KEYWORD, ANY_OF, PUBLISHER_ID_KEYWORD),
    SCIENTIFIC_VALUE(KEYWORD, ANY_OF, SCIENTIFIC_LEVEL_SEARCH_FIELD),
    SCIENTIFIC_INDEX_STATUS(KEYWORD, ANY_OF, SCIENTIFIC_INDEX_STATUS_KEYWORD),
    SCIENTIFIC_REPORT_PERIOD(NUMBER, BETWEEN, SCIENTIFIC_INDEX_YEAR),
    SCOPUS_IDENTIFIER(CUSTOM),
    SERIES(FUZZY_KEYWORD, ALL_OF, ENTITY_DESCRIPTION_REFERENCE_SERIES),
    STATISTICS(IGNORED),
    STATUS(KEYWORD, ANY_OF, STATUS_KEYWORD),
    TAGS(TEXT, ALL_OF, ENTITY_TAGS),
    TITLE(TEXT, ENTITY_DESCRIPTION_MAIN_TITLE, PI),
    TOP_LEVEL_ORGANIZATION(CUSTOM, ANY_OF, TOP_LEVEL_ORG_ID),
    UNIT(CUSTOM, ALL_OF, CONTRIBUTORS_AFFILIATION_ID_KEYWORD),
    USER(FUZZY_KEYWORD, ANY_OF, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_AFFILIATION(KEYWORD, ANY_OF, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(CUSTOM, ANY_OF, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    NODES_SEARCHED(IGNORED, null, null, PATTERN_IS_FIELDS_SEARCHED, null, null),
    NODES_INCLUDED(IGNORED),
    NODES_EXCLUDED(IGNORED),
    // Pagination parameters
    AGGREGATION(IGNORED),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SEARCH_AFTER(IGNORED),
    SORT(ParameterKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(IGNORED, ALL_OF, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ResourceParameter> RESOURCE_PARAMETER_SET =
        Arrays.stream(ResourceParameter.values())
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
    private ParameterSubKind paramSubkind;

    ResourceParameter(ParameterKind kind) {
        this(kind, ALL_OF, null, null, null, null);
    }

    ResourceParameter(ParameterKind kind, String fieldsToSearch) {
        this(kind, ALL_OF, fieldsToSearch, null, null, null);
    }

    ResourceParameter(ParameterKind kind, String fieldsToSearch, Float boost) {
        this(kind, ALL_OF, fieldsToSearch, null, null, boost);
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

        this.fieldOperator = nonNull(operator) ? operator : NA;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
            ? fieldsToSearch.split("\\|")
            : new String[]{name()};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);

        this.keyPattern = nonNull(keyPattern)
            ? keyPattern
            : PATTERN_IS_IGNORE_CASE + name().replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE) + SUB_KIND_PATTERNS;
        this.paramkind = kind;
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
    public ParameterSubKind fieldSubKind() {
        return paramSubkind;
    }

    @Override
    public ResourceParameter withSubKind(ParameterSubKind subKind) {
        this.paramSubkind = subKind;
        return this;
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
        return Arrays.stream(fieldsToSearch)
            .map(ParameterKey.trimKeyword(fieldType(), isKeyWord));
    }


    @Override
    public FieldOperator searchOperator() {
        if (SHOULD.equals(fieldSubKind())) {
            return ANY_OF;
        } else if (EXIST.equals(fieldSubKind())) {
            return ANY_OF;
        } else if (MUST_NOT.equals(fieldSubKind())) {
            return fieldOperator== ALL_OF
                ? NOT_ALL_OF
                : NOT_ANY_OF;
        }
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
                .add(asCamelCase())
                .toString();
    }

    public static ResourceParameter keyFromString(String paramName) {
        var result = Arrays.stream(ResourceParameter.values())
            .filter(ResourceParameter::ignoreInvalidKey)
            .filter(ParameterKey.equalTo(paramName))
            .map(key -> assignSubKind(paramName, key))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    private static ResourceParameter assignSubKind(String paramName, ResourceParameter key) {
        return key.withSubKind(ParameterSubKind.keyFromString(paramName));
    }

    private static boolean ignoreInvalidKey(ResourceParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(ResourceParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX && enumParameter.ordinal() < SEARCH_ALL.ordinal();
    }
}