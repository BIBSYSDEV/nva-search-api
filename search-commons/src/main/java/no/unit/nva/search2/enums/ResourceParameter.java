package no.unit.nva.search2.enums;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_REFERENCE_DOI;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD;
import static no.unit.nva.search2.constant.ApplicationConstants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.constant.ApplicationConstants.FUNDINGS_SOURCE_LABELS;
import static no.unit.nva.search2.constant.ApplicationConstants.IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.constant.ApplicationConstants.MAIN_TITLE;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search2.constant.ApplicationConstants.jsonPath;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.PIPE;
import static no.unit.nva.search2.constant.Words.PROJECTS_ID;
import static no.unit.nva.search2.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search2.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search2.constant.Words.PUBLISHED_DATE;
import static no.unit.nva.search2.constant.Words.REFERENCE;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.constant.Words;
import nva.commons.core.JacocoGenerated;

/**
 * Enum for all the parameters that can be used to query the search index.
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 *
 */

public enum ResourceParameter implements ParameterKey<ResourceParameter> {
    INVALID(ParamKind.INVALID),
    // Parameters converted to Lucene query
    CONTEXT_TYPE(ParamKind.KEYWORD, FieldOperator.MUST, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT,
                     ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTEXT_TYPE_SHOULD(ParamKind.KEYWORD, FieldOperator.SHOULD,
                        ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD),
    CONTRIBUTOR_ID(ParamKind.KEYWORD, FieldOperator.MUST, ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID, null,
                   PATTERN_IS_URI, null),
    CONTRIBUTOR(ParamKind.KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY),
    CONTRIBUTOR_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY),
    CONTRIBUTOR_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY),
    CREATED_BEFORE(ParamKind.DATE, FieldOperator.LESS_THAN, Constants.CREATED_DATE),
    CREATED_SINCE(ParamKind.DATE, FieldOperator.GREATER_THAN_OR_EQUAL_TO, Constants.CREATED_DATE),
    DOI(ParamKind.KEYWORD, ENTITY_DESCRIPTION_REFERENCE_DOI),
    DOI_NOT(ParamKind.TEXT, FieldOperator.MUST_NOT, ENTITY_DESCRIPTION_REFERENCE_DOI),
    DOI_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ENTITY_DESCRIPTION_REFERENCE_DOI),
    FUNDING(ParamKind.KEYWORD, FieldOperator.MUST, Constants.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER, null,
            PATTERN_IS_FUNDING, null),
    FUNDING_SOURCE(ParamKind.KEYWORD, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT,
                       Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD,
                          Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    ID(ParamKind.KEYWORD, IDENTIFIER_KEYWORD),
    ID_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, IDENTIFIER_KEYWORD),
    ID_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, IDENTIFIER_KEYWORD),
    INSTANCE_TYPE(ParamKind.KEYWORD, FieldOperator.MUST, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE,
                  "(?i)instance.?type|category", null, null),
    INSTANCE_TYPE_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE,
                      "(?i)instance.?type.?not|category.?not", null, null),
    INSTANCE_TYPE_SHOULD(ParamKind.KEYWORD, FieldOperator.SHOULD,
                         ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE,
                         "(?i)instance.?type.?should|category.?should", null, null),
    INSTITUTION(ParamKind.KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    INSTITUTION_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION),
    ISBN(ParamKind.KEYWORD, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(ParamKind.KEYWORD, FieldOperator.SHOULD, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(ParamKind.KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT,
             Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ISSN_SHOULD(ParamKind.KEYWORD, FieldOperator.SHOULD,
                Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN),
    ORCID(ParamKind.KEYWORD, ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    ORCID_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    ORCID_SHOULD(ParamKind.KEYWORD, FieldOperator.SHOULD, ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    MODIFIED_BEFORE(ParamKind.DATE, FieldOperator.LESS_THAN, MODIFIED_DATE),
    MODIFIED_SINCE(ParamKind.DATE, FieldOperator.GREATER_THAN_OR_EQUAL_TO, MODIFIED_DATE),
    PARENT_PUBLICATION(ParamKind.KEYWORD, FieldOperator.MUST, Constants.PARENT_PUBLICATION_ID),
    PARENT_PUBLICATION_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, Constants.PARENT_PUBLICATION_ID),
    PROJECT(ParamKind.KEYWORD, PROJECTS_ID),
    PROJECT_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, PROJECTS_ID),
    PROJECT_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, PROJECTS_ID),
    PUBLISHED_BEFORE(ParamKind.DATE, FieldOperator.LESS_THAN, PUBLISHED_DATE),
    PUBLISHED_SINCE(ParamKind.DATE, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLISHED_DATE),
    TITLE(ParamKind.TEXT, MAIN_TITLE, 2F),
    TITLE_NOT(ParamKind.TEXT, FieldOperator.MUST_NOT, MAIN_TITLE),
    TITLE_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, MAIN_TITLE),
    TOP_LEVEL_ORGANIZATION(ParamKind.KEYWORD, FieldOperator.MUST, TOP_LEVEL_ORGANIZATIONS + ".id.keyword"),
    UNIT(ParamKind.KEYWORD, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    UNIT_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    UNIT_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    USER(ParamKind.KEYWORD, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, RESOURCE_OWNER_OWNER_KEYWORD),
    USER_AFFILIATION(ParamKind.KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_NOT(ParamKind.KEYWORD, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    USER_AFFILIATION_SHOULD(ParamKind.TEXT, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
    PUBLICATION_YEAR(ParamKind.NUMBER, FieldOperator.MUST, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                     "(?i)year.?reported|publication.?year", null, null),
    PUBLICATION_YEAR_SHOULD(ParamKind.NUMBER, FieldOperator.SHOULD, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                            "(?i)year.?reported.?should|publication.?year.?should", null, null),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(ParamKind.TEXT, FieldOperator.MUST, "q", "(?i)search.?all|query", null, null),
    FIELDS(ParamKind.CUSTOM),
    // Pagination parameters
    PAGE(ParamKind.NUMBER),
    FROM(ParamKind.NUMBER, null, null, "(?i)offset|from", null, null),
    SIZE(ParamKind.NUMBER, null, null, "(?i)per.?page|results|limit|size", null, null),
    SORT(ParamKind.SORT_KEY, null, null, "(?i)order.?by|sort", null, null),
    SORT_ORDER(ParamKind.CUSTOM, FieldOperator.MUST, null, "(?i)sort.?order|order", "(?i)asc|desc", null),
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
        this(kind, FieldOperator.MUST, null, null, null, null);
    }

    ResourceParameter(ParamKind kind, String fieldsToSearch) {
        this(kind, FieldOperator.MUST, fieldsToSearch, null, null, null);
    }

    ResourceParameter(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, FieldOperator.MUST, fieldsToSearch, null, null, boost);
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
            : PATTERN_IS_IGNORE_CASE + key.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE) + ASTERISK;
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

    private static boolean ignoreInvalidKey(ResourceParameter f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(ResourceParameter f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX && f.ordinal() < SEARCH_ALL.ordinal();
    }

    private static class Constants {

        public static final String CREATED_DATE = "createdDate";
        private static final String AC_KEYWORD = Words.KEYWORD;
        public static final String AC_ID = Words.ID;

        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD =
            jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, ENGLISH_CODE, AC_KEYWORD)
            + PIPE + jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, NYNORSK_CODE, AC_KEYWORD)
            + PIPE + jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, BOKMAAL_CODE, AC_KEYWORD)
            + PIPE + jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, SAMI_CODE, AC_KEYWORD);

        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION =
            ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID
            + PIPE + ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD;

        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY =
            ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
            + PIPE + ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME;

        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN =
            ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
            + PIPE + ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN;

        public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER =
            jsonPath(FUNDINGS, IDENTIFIER_KEYWORD + PIPE + FUNDINGS, SOURCE, IDENTIFIER, AC_KEYWORD);

        public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS =
            FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER
            + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, ENGLISH_CODE, AC_KEYWORD)
            + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, NYNORSK_CODE, AC_KEYWORD)
            + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, BOKMAAL_CODE, AC_KEYWORD)
            + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, SAMI_CODE, AC_KEYWORD);

        public static final String PARENT_PUBLICATION_ID =
            jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, "corrigendumFor", AC_KEYWORD)
            + PIPE
            + jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, "manifestations", AC_ID, AC_KEYWORD)
            + PIPE
            + jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, AC_ID, AC_KEYWORD);
    }
}