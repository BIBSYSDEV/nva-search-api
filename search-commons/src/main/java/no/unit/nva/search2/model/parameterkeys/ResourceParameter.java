package no.unit.nva.search2.model.parameterkeys;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.PIPE;
import static no.unit.nva.search2.constant.ApplicationConstants.UNDERSCORE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FUNDING;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URI;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.ParamKind.DATE;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.ParamKind.KEYWORD;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.ParamKind.SORT_KEY;
import static no.unit.nva.search2.model.parameterkeys.ParameterKey.ParamKind.TEXT;
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
 * This enum needs to implement these parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 *
 */

public enum ResourceParameter implements ParameterKey<ResourceParameter> {
    INVALID(ParamKind.INVALID),
    // Parameters converted to Lucene query
    CATEGORY(KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CONTRIBUTOR_ID(KEYWORD, MUST, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID,null, PATTERN_IS_URI, null),
    CONTRIBUTOR(KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
                        + PIPE + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
                                      + PIPE + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
                                       + PIPE + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME),
    CREATED_BEFORE(DATE, LESS_THAN, Constants.CREATED_DATE),
    CREATED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, Constants.CREATED_DATE),
    DOI(KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_DOI),
    DOI_NOT(TEXT, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_DOI),
    DOI_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_DOI),
    FUNDING(KEYWORD, MUST, Constants.FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER,
            null, PATTERN_IS_FUNDING, null),
    FUNDING_SOURCE(KEYWORD, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_NOT(KEYWORD, MUST_NOT, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    FUNDING_SOURCE_SHOULD(TEXT, SHOULD, Constants.FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS),
    ID(KEYWORD, Constants.IDENTIFIER),
    ID_NOT(KEYWORD, MUST_NOT, Constants.IDENTIFIER),
    ID_SHOULD(TEXT, SHOULD, Constants.IDENTIFIER),
    INSTITUTION(KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID + PIPE
        + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME),
    INSTITUTION_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID + PIPE
                                    + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME),
    INSTITUTION_SHOULD(TEXT, SHOULD,  Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID + PIPE
        + Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME),
    ISBN(KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISBN_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST),
    ISSN(KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
                 + PIPE + Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN),
    ISSN_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
                               + PIPE + Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN),
    ISSN_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
                                + PIPE + Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN),
    ORCID(KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    ORCID_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    ORCID_SHOULD(KEYWORD, SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID),
    MODIFIED_BEFORE(DATE, LESS_THAN, Constants.MODIFIED_DATE),
    MODIFIED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, Constants.MODIFIED_DATE),
    PARENT_PUBLICATION(KEYWORD, MUST, Constants.PARENT_PUBLICATION_ID),
    PARENT_PUBLICATION_SHOULD(TEXT, SHOULD, Constants.PARENT_PUBLICATION_ID),
    PROJECT(KEYWORD, Constants.PROJECTS_ID),
    PROJECT_NOT(KEYWORD, MUST_NOT, Constants.PROJECTS_ID),
    PROJECT_SHOULD(TEXT, SHOULD, Constants.PROJECTS_ID),
    PUBLISHED_BEFORE(DATE, LESS_THAN, Constants.PUBLISHED_DATE),
    PUBLISHED_SINCE(DATE, GREATER_THAN_OR_EQUAL_TO, Constants.PUBLISHED_DATE),
    TITLE(TEXT, Constants.MAIN_TITLE, 2F),
    TITLE_NOT(TEXT, MUST_NOT, Constants.MAIN_TITLE),
    TITLE_SHOULD(TEXT, SHOULD, Constants.MAIN_TITLE),
    UNIT(KEYWORD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    UNIT_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    UNIT_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID),
    USER(KEYWORD, Constants.RESOURCE_OWNER),
    USER_NOT(KEYWORD, MUST_NOT, Constants.RESOURCE_OWNER),
    USER_SHOULD(TEXT, SHOULD, Constants.RESOURCE_OWNER),
    USER_AFFILIATION(KEYWORD, Constants.RESOURCE_OWNER_AFFILIATION),
    USER_AFFILIATION_NOT(KEYWORD, Constants.RESOURCE_OWNER_AFFILIATION),
    USER_AFFILIATION_SHOULD(TEXT, Constants.RESOURCE_OWNER_AFFILIATION),
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
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID =
            "entityDescription.contributors.affiliations.id.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_NAME =
            "entityDescription.contributors.affiliations.labels.nb.keyword"
                + "|entityDescription.contributors.affiliations.labels.nn.keyword"
            + "|entityDescription.contributors.affiliations.labels.en.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID =
            "entityDescription.contributors.identity.id.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME =
            "entityDescription.contributors.identity.name.keyword";
        public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID =
            "entityDescription.contributors.identity.orcId.keyword";
        public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
            "entityDescription.publicationDate.year";
        public static final String ENTITY_DESCRIPTION_REFERENCE_DOI =
            "entityDescription.reference.doi.keyword";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST =
            "entityDescription.reference.publicationContext.isbnList";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN =
            "entityDescription.reference.publicationContext.onlineIssn.keyword";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN =
            "entityDescription.reference.publicationContext.printIssn.keyword";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE =
            "entityDescription.reference.publicationInstance.type.keyword";
        public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER =
            "fundings.identifier.keyword|fundings.source.identifier.keyword";
        public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS =
            FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER + PIPE +
            "fundings.source.labels.nn.keyword|fundings.source.labels.nb.keyword"
            + "|fundings.source.labels.en.keyword|fundings.source.labels.sme.keyword";
        public static final String PARENT_PUBLICATION_ID =
            "entityDescription.reference.publicationInstance.corrigendumFor.keyword"
            + "|entityDescription.reference.publicationContext.id.keyword"
            + "|entityDescription.reference.publicationInstance.manifestations.id.keyword";
        public static final String IDENTIFIER = "identifier.keyword";
        public static final String MAIN_TITLE = "entityDescription.mainTitle";
        public static final String MODIFIED_DATE = "modifiedDate";
        public static final String PROJECTS_ID = "projects.id";
        public static final String PUBLISHED_DATE = "publishedDate";
        public static final String RESOURCE_OWNER = "resourceOwner.owner.keyword";
        public static final String RESOURCE_OWNER_AFFILIATION = "resourceOwner.ownerAffiliation.keyword";
    }
}