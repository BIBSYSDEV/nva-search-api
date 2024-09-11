package no.unit.nva.search.importcandidate;

import static no.unit.nva.constants.ErrorMessages.NOT_IMPLEMENTED_FOR;
import static no.unit.nva.constants.Words.CHAR_UNDERSCORE;
import static no.unit.nva.constants.Words.COLON;
import static no.unit.nva.constants.Words.Q;
import static no.unit.nva.constants.Words.UNDERSCORE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.common.enums.FieldOperator.ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.ANY_OF;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.NA;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ALL_OF;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.DATE;
import static no.unit.nva.search.common.enums.ParameterKind.FLAG;
import static no.unit.nva.search.common.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search.common.enums.ParameterKind.SORT_KEY;
import static no.unit.nva.search.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search.importcandidate.Constants.ADDITIONAL_IDENTIFIERS_KEYWORD;
import static no.unit.nva.search.importcandidate.Constants.COLLABORATION_TYPE_KEYWORD;
import static no.unit.nva.search.importcandidate.Constants.FILES_STATUS_PATH;
import static no.unit.nva.search.importcandidate.Constants.ID_KEYWORD;
import static no.unit.nva.search.importcandidate.Constants.IMPORT_STATUS_PATH;
import static no.unit.nva.search.importcandidate.Constants.MODIFIED_DATE_PATH;
import static no.unit.nva.search.importcandidate.Constants.ORGANIZATIONS_PATH;
import static no.unit.nva.search.importcandidate.Constants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search.importcandidate.Constants.PUBLICATION_YEAR_KEYWORD;
import static no.unit.nva.search.importcandidate.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.ASSOCIATED_ARTIFACTS_LICENSE;

import static java.util.Objects.nonNull;

import no.unit.nva.constants.Words;
import no.unit.nva.search.common.enums.FieldOperator;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.ParameterKind;
import no.unit.nva.search.common.enums.ValueEncoding;

import nva.commons.core.JacocoGenerated;

import org.apache.commons.lang3.NotImplementedException;
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
 */
public enum ImportCandidateParameter implements ParameterKey<ImportCandidateParameter> {
    INVALID(ParameterKind.INVALID),
    ADDITIONAL_IDENTIFIERS(FUZZY_KEYWORD, ANY_OF, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_NOT(KEYWORD, NOT_ALL_OF, ADDITIONAL_IDENTIFIERS_KEYWORD),
    CATEGORY(FUZZY_KEYWORD, ANY_OF, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, NOT_ALL_OF, PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(DATE, BETWEEN, Words.CREATED_DATE),
    CONTRIBUTOR(FUZZY_KEYWORD, ALL_OF, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NOT(FUZZY_KEYWORD, NOT_ALL_OF, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    COLLABORATION_TYPE(FUZZY_KEYWORD, ANY_OF, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_NOT(KEYWORD, NOT_ALL_OF, COLLABORATION_TYPE_KEYWORD),
    CRISTIN_IDENTIFIER(CUSTOM),
    DOI(FUZZY_KEYWORD, ANY_OF, Constants.DOI_KEYWORD),
    DOI_NOT(TEXT, NOT_ALL_OF, Constants.DOI_KEYWORD),
    FILES(KEYWORD, ALL_OF, FILES_STATUS_PATH),
    ID(FUZZY_KEYWORD, ANY_OF, ID_KEYWORD),
    ID_NOT(FUZZY_KEYWORD, NOT_ALL_OF, ID_KEYWORD),
    IMPORT_STATUS(FUZZY_KEYWORD, ANY_OF, IMPORT_STATUS_PATH),
    IMPORT_STATUS_NOT(FUZZY_KEYWORD, NOT_ALL_OF, IMPORT_STATUS_PATH),
    LICENSE(CUSTOM, ALL_OF, ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED_DATE(DATE, BETWEEN, MODIFIED_DATE_PATH),
    LICENSE_NOT(CUSTOM, NOT_ALL_OF, ASSOCIATED_ARTIFACTS_LICENSE),
    PUBLICATION_YEAR(NUMBER, BETWEEN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_BEFORE(NUMBER, FieldOperator.LESS_THAN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_SINCE(
            NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLICATION_YEAR_KEYWORD),
    PUBLISHER(KEYWORD, ALL_OF, PUBLISHER_ID_KEYWORD),
    PUBLISHER_NOT(KEYWORD, NOT_ALL_OF, PUBLISHER_ID_KEYWORD),
    SCOPUS_IDENTIFIER(CUSTOM),
    TOP_LEVEL_ORGANIZATION(KEYWORD, ANY_OF, ORGANIZATIONS_PATH),
    TOP_LEVEL_ORGANIZATION_NOT(KEYWORD, NOT_ALL_OF, ORGANIZATIONS_PATH),
    TITLE(TEXT, ANY_OF, Constants.MAIN_TITLE_KEYWORD, null, null, 2F),
    TITLE_NOT(TEXT, NOT_ALL_OF, Constants.MAIN_TITLE_KEYWORD),
    TYPE(FUZZY_KEYWORD, ANY_OF, PUBLICATION_INSTANCE_TYPE),
    TYPE_NOT(KEYWORD, NOT_ALL_OF, PUBLICATION_INSTANCE_TYPE),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(FREE_TEXT, ALL_OF, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    // Pagination parameters
    NODES_SEARCHED(FLAG, null, null, PATTERN_IS_FIELDS_SEARCHED, null, null),
    NODES_INCLUDED(FLAG),
    NODES_EXCLUDED(FLAG),
    AGGREGATION(FLAG),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SEARCH_AFTER(FLAG),
    SORT(SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(FLAG, ALL_OF, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    ;

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ImportCandidateParameter> IMPORT_CANDIDATE_PARAMETER_SET =
            Arrays.stream(ImportCandidateParameter.values())
                    .filter(ImportCandidateParameter::isSearchField)
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

    ImportCandidateParameter(ParameterKind kind) {
        this(kind, ALL_OF, null, null, null, null);
    }

    ImportCandidateParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(
            ParameterKind kind,
            FieldOperator operator,
            String fieldsToSearch,
            String keyPattern,
            String valuePattern,
            Float boost) {

        this.fieldOperator = nonNull(operator) ? operator : NA;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch =
                nonNull(fieldsToSearch)
                        ? fieldsToSearch.split(PATTERN_IS_PIPE)
                        : new String[] {name()};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern =
                nonNull(keyPattern)
                        ? keyPattern
                        : PATTERN_IS_IGNORE_CASE
                                + name().replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.paramkind = kind;
    }

    public static ImportCandidateParameter keyFromString(String paramName) {
        var result =
                Arrays.stream(ImportCandidateParameter.values())
                        .filter(ImportCandidateParameter::ignoreInvalidKey)
                        .filter(ParameterKey.equalTo(paramName))
                        .collect(Collectors.toSet());
        return result.size() == 1 ? result.stream().findFirst().get() : INVALID;
    }

    private static boolean ignoreInvalidKey(ImportCandidateParameter f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(ImportCandidateParameter f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX && f.ordinal() < SEARCH_ALL.ordinal();
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
    @JacocoGenerated
    public ImportCandidateParameter subQuery() {
        throw new NotImplementedException(NOT_IMPLEMENTED_FOR + this.getClass().getName());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return new StringJoiner(COLON, "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(asCamelCase())
                .toString();
    }
}
