package no.unit.nva.search2.importcandidate;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.Q;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.common.enums.FieldOperator.ALL_ITEMS;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static no.unit.nva.search2.common.enums.FieldOperator.ONE_OR_MORE_ITEM;
import static no.unit.nva.search2.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search2.common.enums.ParameterKind.DATE;
import static no.unit.nva.search2.common.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search2.common.enums.ParameterKind.IGNORED;
import static no.unit.nva.search2.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search2.common.enums.ParameterKind.SORT_KEY;
import static no.unit.nva.search2.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search2.importcandidate.Constants.ADDITIONAL_IDENTIFIERS_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.COLLABORATION_TYPE_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.INSTANCE_TYPE_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.importcandidate.Constants.PUBLICATION_YEAR_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.STATUS_TYPE_KEYWORD;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.FieldOperator;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ParameterKind;
import no.unit.nva.search2.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to implement these
 * parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */

public enum ImportCandidateParameter implements ParameterKey {
    INVALID(ParameterKind.INVALID),
    // Parameters converted to Lucene query
    CRISTIN_IDENTIFIER(CUSTOM),
    SCOPUS_IDENTIFIER(CUSTOM),
    ADDITIONAL_IDENTIFIERS_NOT(KEYWORD, NO_ITEMS, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_SHOULD(TEXT, ONE_OR_MORE_ITEM, ADDITIONAL_IDENTIFIERS_KEYWORD),
    CATEGORY(KEYWORD, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, NO_ITEMS, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(TEXT, ONE_OR_MORE_ITEM, PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(DATE, Words.CREATED_DATE),
    CONTRIBUTOR(KEYWORD, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NOT(KEYWORD, NO_ITEMS, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_SHOULD(TEXT, ONE_OR_MORE_ITEM, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NAME(KEYWORD, Constants.CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NAME_NOT(KEYWORD, NO_ITEMS, Constants.CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NAME_SHOULD(TEXT, ONE_OR_MORE_ITEM, Constants.CONTRIBUTORS_IDENTITY_NAME),
    COLLABORATION_TYPE(KEYWORD, ALL_ITEMS, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_NOT(KEYWORD, NO_ITEMS, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_SHOULD(TEXT, ONE_OR_MORE_ITEM, COLLABORATION_TYPE_KEYWORD),
    DOI(KEYWORD, Constants.DOI_KEYWORD),
    DOI_NOT(TEXT, NO_ITEMS, Constants.DOI_KEYWORD),
    DOI_SHOULD(TEXT, ONE_OR_MORE_ITEM, Constants.DOI_KEYWORD),
    ID(KEYWORD, Constants.IDENTIFIER),
    ID_NOT(KEYWORD, NO_ITEMS, Constants.IDENTIFIER),
    ID_SHOULD(TEXT, ONE_OR_MORE_ITEM, Constants.IDENTIFIER),
    IMPORT_STATUS(KEYWORD, STATUS_TYPE_KEYWORD),
    IMPORT_STATUS_NOT(KEYWORD, NO_ITEMS, STATUS_TYPE_KEYWORD),
    IMPORT_STATUS_SHOULD(TEXT, ONE_OR_MORE_ITEM, STATUS_TYPE_KEYWORD),
    INSTANCE_TYPE(KEYWORD, ALL_ITEMS, INSTANCE_TYPE_KEYWORD),
    INSTANCE_TYPE_NOT(KEYWORD, NO_ITEMS, INSTANCE_TYPE_KEYWORD),
    INSTANCE_TYPE_SHOULD(TEXT, ONE_OR_MORE_ITEM, INSTANCE_TYPE_KEYWORD),
    PUBLICATION_YEAR(KEYWORD, ALL_ITEMS, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_BEFORE(NUMBER, FieldOperator.LESS_THAN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_SINCE(NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLICATION_YEAR_KEYWORD),
    PUBLISHER(KEYWORD, ALL_ITEMS, PUBLISHER_ID_KEYWORD),
    PUBLISHER_NOT(KEYWORD, NO_ITEMS, PUBLISHER_ID_KEYWORD),
    PUBLISHER_SHOULD(TEXT, ONE_OR_MORE_ITEM, PUBLISHER_ID_KEYWORD),
    TITLE(TEXT, Constants.MAIN_TITLE_KEYWORD, 2F),
    TITLE_NOT(TEXT, NO_ITEMS, Constants.MAIN_TITLE_KEYWORD),
    TITLE_SHOULD(TEXT, ONE_OR_MORE_ITEM, Constants.MAIN_TITLE_KEYWORD),
    TYPE(KEYWORD, Constants.TYPE_KEYWORD),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(FREE_TEXT, ALL_ITEMS, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    FIELDS(ParameterKind.CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SORT(SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(IGNORED, ALL_ITEMS, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    SEARCH_AFTER(IGNORED);

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<ImportCandidateParameter> VALID_LUCENE_PARAMETER_KEYS =
        Arrays.stream(ImportCandidateParameter.values())
            .filter(ImportCandidateParameter::isSearchField)
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

    ImportCandidateParameter(ParameterKind kind) {
        this(kind, ALL_ITEMS, null, null, null, null);
    }

    ImportCandidateParameter(ParameterKind kind, String fieldsToSearch) {
        this(kind, ALL_ITEMS, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(ParameterKind kind, String fieldsToSearch, Float boost) {
        this(kind, ALL_ITEMS, fieldsToSearch, null, null, boost);
    }

    ImportCandidateParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(
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
    public Stream<String> searchFields() {
        return Arrays.stream(fieldsToSearch)
            .map(String::trim)
            .map(trimmed -> isNotKeyword()
                ? trimmed.replace(DOT + Words.KEYWORD, EMPTY_STRING)
                : trimmed);
    }

    private boolean isNotKeyword() {
        return !fieldType().equals(KEYWORD);
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