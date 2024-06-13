package no.unit.nva.search.importcandidate;

import static java.util.Objects.nonNull;
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
import static no.unit.nva.search.common.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search.common.constant.Words.COLON;
import static no.unit.nva.search.common.constant.Words.Q;
import static no.unit.nva.search.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search.common.enums.FieldOperator.ALL_ITEMS;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.NA;
import static no.unit.nva.search.common.enums.FieldOperator.NO_ITEMS;
import static no.unit.nva.search.common.enums.FieldOperator.ONE_OR_MORE_ITEM;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.DATE;
import static no.unit.nva.search.common.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.IGNORED;
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
import no.unit.nva.search.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;
import org.apache.commons.text.CaseUtils;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to implement these
 * parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 * @author Stig Norland
 */
public enum ImportCandidateParameter implements ParameterKey {
    INVALID(ParameterKind.INVALID),
    ADDITIONAL_IDENTIFIERS(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_NOT(KEYWORD, NO_ITEMS, ADDITIONAL_IDENTIFIERS_KEYWORD),
    CATEGORY(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, NO_ITEMS, PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(DATE, BETWEEN, Words.CREATED_DATE),
    CONTRIBUTOR(FUZZY_KEYWORD, ALL_ITEMS, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NOT(FUZZY_KEYWORD, NO_ITEMS, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    COLLABORATION_TYPE(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_NOT(KEYWORD, NO_ITEMS, COLLABORATION_TYPE_KEYWORD),
    CRISTIN_IDENTIFIER(CUSTOM),
    DOI(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, Constants.DOI_KEYWORD),
    DOI_NOT(TEXT, NO_ITEMS, Constants.DOI_KEYWORD),
    FILES(KEYWORD, ALL_ITEMS, FILES_STATUS_PATH),
    ID(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, ID_KEYWORD),
    ID_NOT(FUZZY_KEYWORD, NO_ITEMS, ID_KEYWORD),
    IMPORT_STATUS(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, IMPORT_STATUS_PATH),
    IMPORT_STATUS_NOT(FUZZY_KEYWORD, NO_ITEMS, IMPORT_STATUS_PATH),
    LICENSE(CUSTOM, ALL_ITEMS, ASSOCIATED_ARTIFACTS_LICENSE),
    MODIFIED_DATE(DATE, BETWEEN, MODIFIED_DATE_PATH),
    LICENSE_NOT(CUSTOM, NO_ITEMS, ASSOCIATED_ARTIFACTS_LICENSE),
    PUBLICATION_YEAR(NUMBER, BETWEEN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_BEFORE(NUMBER, FieldOperator.LESS_THAN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_SINCE(NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLICATION_YEAR_KEYWORD),
    PUBLISHER(KEYWORD, ALL_ITEMS, PUBLISHER_ID_KEYWORD),
    PUBLISHER_NOT(KEYWORD, NO_ITEMS, PUBLISHER_ID_KEYWORD),
    SCOPUS_IDENTIFIER(CUSTOM),
    TOP_LEVEL_ORGANIZATION(KEYWORD, ONE_OR_MORE_ITEM, ORGANIZATIONS_PATH),
    TOP_LEVEL_ORGANIZATION_NOT(KEYWORD, NO_ITEMS, ORGANIZATIONS_PATH),
    TITLE(TEXT,ONE_OR_MORE_ITEM, Constants.MAIN_TITLE_KEYWORD,null,null, 2F),
    TITLE_NOT(TEXT, NO_ITEMS, Constants.MAIN_TITLE_KEYWORD),
    TYPE(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, PUBLICATION_INSTANCE_TYPE),
    TYPE_NOT(KEYWORD, NO_ITEMS, PUBLICATION_INSTANCE_TYPE),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(FREE_TEXT, ALL_ITEMS, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    // Pagination parameters
    NODES_SEARCHED(IGNORED, null, null, PATTERN_IS_FIELDS_SEARCHED, null, null),
    NODES_INCLUDED(IGNORED),
    NODES_EXCLUDED(IGNORED),
    AGGREGATION(IGNORED),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SORT(SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(IGNORED, ALL_ITEMS, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    SEARCH_AFTER(IGNORED);

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
        this(kind, ALL_ITEMS, null, null, null, null);
    }

    ImportCandidateParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(
        ParameterKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern, String valuePattern,
        Float boost) {

        this.fieldOperator = nonNull(operator) ? operator : NA;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
            ? fieldsToSearch.split(PATTERN_IS_PIPE)
            : new String[]{name()};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern = nonNull(keyPattern)
            ? keyPattern
            : PATTERN_IS_IGNORE_CASE + name().replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
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

    public static ImportCandidateParameter keyFromString(String paramName) {
        var result = Arrays.stream(ImportCandidateParameter.values())
            .filter(ImportCandidateParameter::ignoreInvalidKey)
            .filter(ParameterKey.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    private static boolean ignoreInvalidKey(ImportCandidateParameter f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(ImportCandidateParameter f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX && f.ordinal() < SEARCH_ALL.ordinal();
    }
}