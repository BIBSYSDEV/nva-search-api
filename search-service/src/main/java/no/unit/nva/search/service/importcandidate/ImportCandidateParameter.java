package no.unit.nva.search.service.importcandidate;

import static no.unit.nva.search.model.constant.ErrorMessages.NOT_IMPLEMENTED_FOR;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_PIPE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.service.importcandidate.Constants.ADDITIONAL_IDENTIFIERS_KEYWORD;
import static no.unit.nva.search.service.importcandidate.Constants.COLLABORATION_TYPE_KEYWORD;
import static no.unit.nva.search.service.importcandidate.Constants.FILES_STATUS_PATH;
import static no.unit.nva.search.service.importcandidate.Constants.ID_KEYWORD;
import static no.unit.nva.search.service.importcandidate.Constants.IMPORT_STATUS_PATH;
import static no.unit.nva.search.service.importcandidate.Constants.MODIFIED_DATE_PATH;
import static no.unit.nva.search.service.importcandidate.Constants.ORGANIZATIONS_PATH;
import static no.unit.nva.search.service.importcandidate.Constants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search.service.importcandidate.Constants.PUBLICATION_YEAR_KEYWORD;
import static no.unit.nva.search.service.importcandidate.Constants.PUBLISHER_ID_KEYWORD;

import static java.util.Objects.nonNull;

import no.unit.nva.search.model.constant.Words;
import no.unit.nva.search.model.enums.FieldOperator;
import no.unit.nva.search.model.enums.ParameterKey;
import no.unit.nva.search.model.enums.ParameterKind;
import no.unit.nva.search.model.enums.ValueEncoding;

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
@SuppressWarnings({"PMD.ExcessivePublicCount"})
public enum ImportCandidateParameter implements ParameterKey<ImportCandidateParameter> {
    INVALID(ParameterKind.INVALID),
    ADDITIONAL_IDENTIFIERS(
            ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_NOT(
            ParameterKind.KEYWORD, FieldOperator.NOT_ALL_OF, ADDITIONAL_IDENTIFIERS_KEYWORD),
    CATEGORY(ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(ParameterKind.KEYWORD, FieldOperator.NOT_ALL_OF, PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(ParameterKind.DATE, FieldOperator.BETWEEN, Words.CREATED_DATE),
    CONTRIBUTOR(
            ParameterKind.FUZZY_KEYWORD,
            FieldOperator.ALL_OF,
            Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NOT(
            ParameterKind.FUZZY_KEYWORD,
            FieldOperator.NOT_ALL_OF,
            Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    COLLABORATION_TYPE(
            ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_NOT(
            ParameterKind.KEYWORD, FieldOperator.NOT_ALL_OF, COLLABORATION_TYPE_KEYWORD),
    CRISTIN_IDENTIFIER(ParameterKind.CUSTOM),
    DOI(ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, Constants.DOI_KEYWORD),
    DOI_NOT(ParameterKind.TEXT, FieldOperator.NOT_ALL_OF, Constants.DOI_KEYWORD),
    FILES(ParameterKind.KEYWORD, FieldOperator.ALL_OF, FILES_STATUS_PATH),
    ID(ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, ID_KEYWORD),
    ID_NOT(ParameterKind.FUZZY_KEYWORD, FieldOperator.NOT_ALL_OF, ID_KEYWORD),
    IMPORT_STATUS(ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, IMPORT_STATUS_PATH),
    IMPORT_STATUS_NOT(ParameterKind.FUZZY_KEYWORD, FieldOperator.NOT_ALL_OF, IMPORT_STATUS_PATH),
    LICENSE(
            ParameterKind.CUSTOM,
            FieldOperator.ALL_OF,
            no.unit.nva.search.service.resource.Constants.ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED_DATE(ParameterKind.DATE, FieldOperator.BETWEEN, MODIFIED_DATE_PATH),
    LICENSE_NOT(
            ParameterKind.CUSTOM,
            FieldOperator.NOT_ALL_OF,
            no.unit.nva.search.service.resource.Constants.ASSOCIATED_ARTIFACTS_LICENSE),
    PUBLICATION_YEAR(ParameterKind.NUMBER, FieldOperator.BETWEEN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_BEFORE(
            ParameterKind.NUMBER, FieldOperator.LESS_THAN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_SINCE(
            ParameterKind.NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLICATION_YEAR_KEYWORD),
    PUBLISHER(ParameterKind.KEYWORD, FieldOperator.ALL_OF, PUBLISHER_ID_KEYWORD),
    PUBLISHER_NOT(ParameterKind.KEYWORD, FieldOperator.NOT_ALL_OF, PUBLISHER_ID_KEYWORD),
    SCOPUS_IDENTIFIER(ParameterKind.CUSTOM),
    TOP_LEVEL_ORGANIZATION(ParameterKind.KEYWORD, FieldOperator.ANY_OF, ORGANIZATIONS_PATH),
    TOP_LEVEL_ORGANIZATION_NOT(ParameterKind.KEYWORD, FieldOperator.NOT_ALL_OF, ORGANIZATIONS_PATH),
    TITLE(ParameterKind.TEXT, FieldOperator.ANY_OF, Constants.MAIN_TITLE_KEYWORD, null, null, 2F),
    TITLE_NOT(ParameterKind.TEXT, FieldOperator.NOT_ALL_OF, Constants.MAIN_TITLE_KEYWORD),
    TYPE(ParameterKind.FUZZY_KEYWORD, FieldOperator.ANY_OF, PUBLICATION_INSTANCE_TYPE),
    TYPE_NOT(ParameterKind.KEYWORD, FieldOperator.NOT_ALL_OF, PUBLICATION_INSTANCE_TYPE),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(
            ParameterKind.FREE_TEXT,
            FieldOperator.ALL_OF,
            Words.Q,
            PATTERN_IS_SEARCH_ALL_KEY,
            null,
            null),
    // Pagination parameters
    NODES_SEARCHED(ParameterKind.FLAG, null, null, PATTERN_IS_FIELDS_SEARCHED, null, null),
    NODES_INCLUDED(ParameterKind.FLAG),
    NODES_EXCLUDED(ParameterKind.FLAG),
    AGGREGATION(ParameterKind.FLAG),
    PAGE(ParameterKind.NUMBER),
    FROM(ParameterKind.NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(ParameterKind.NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SEARCH_AFTER(ParameterKind.FLAG),
    SORT(ParameterKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(
            ParameterKind.FLAG,
            FieldOperator.ALL_OF,
            null,
            PATTERN_IS_SORT_ORDER_KEY,
            PATTERN_IS_ASC_DESC_VALUE,
            null),
    ;

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ImportCandidateParameter> IMPORT_CANDIDATE_PARAMETER_SET =
            Arrays.stream(values())
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
        this(kind, FieldOperator.ALL_OF, null, null, null, null);
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

        this.fieldOperator = nonNull(operator) ? operator : FieldOperator.NA;
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
                                + name().replace(Words.UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.paramkind = kind;
    }

    public static ImportCandidateParameter keyFromString(String paramName) {
        var result =
                Arrays.stream(values())
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
        return CaseUtils.toCamelCase(this.name(), false, Words.CHAR_UNDERSCORE);
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
        return new StringJoiner(Words.COLON, "Key[", "]")
                .add(String.valueOf(ordinal()))
                .add(asCamelCase())
                .toString();
    }
}
