package no.unit.nva.search2.importcandidate;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.FieldOperator;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ParameterKind;
import no.unit.nva.search2.common.enums.ValueEncoding;
import nva.commons.core.JacocoGenerated;

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
import static no.unit.nva.search2.common.constant.Words.Q;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.common.enums.FieldOperator.ALL;
import static no.unit.nva.search2.common.enums.FieldOperator.ANY;
import static no.unit.nva.search2.common.enums.FieldOperator.NONE;
import static no.unit.nva.search2.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search2.common.enums.ParameterKind.SORT_KEY;
import static no.unit.nva.search2.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search2.importcandidate.Constants.ADDITIONAL_IDENTIFIERS_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.COLLABORATION_TYPE_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.INSTANCE_TYPE_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.importcandidate.Constants.PUBLICATION_YEAR_KEYWORD;
import static no.unit.nva.search2.importcandidate.Constants.STATUS_TYPE_KEYWORD;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to implement these
 * parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */

public enum ImportCandidateParameter implements ParameterKey {
    INVALID(TEXT),
    // Parameters converted to Lucene query
    ADDITIONAL_IDENTIFIERS(KEYWORD, ALL, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_NOT(KEYWORD, NONE, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_SHOULD(TEXT, ANY, ADDITIONAL_IDENTIFIERS_KEYWORD),
    CATEGORY(KEYWORD, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, NONE, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(KEYWORD, ANY, PUBLICATION_INSTANCE_TYPE),
    COLLABORATION_TYPE(KEYWORD, ALL, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_NOT(KEYWORD, NONE, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_SHOULD(TEXT, ANY, COLLABORATION_TYPE_KEYWORD),
    CONTRIBUTOR(FUZZY_KEYWORD, ALL, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS, 2F),
    CONTRIBUTOR_NOT(KEYWORD, NONE, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_SHOULD(TEXT, ANY, Constants.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NAME(KEYWORD, Constants.CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NAME_NOT(KEYWORD, NONE, Constants.CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NAME_SHOULD(TEXT, ANY, Constants.CONTRIBUTORS_IDENTITY_NAME),
    CREATED_DATE(ParameterKind.DATE, Words.CREATED_DATE),
    CRISTIN_IDENTIFIER(KEYWORD),
    DOI(KEYWORD, Constants.DOI_KEYWORD),
    DOI_NOT(TEXT, NONE, Constants.DOI_KEYWORD),
    DOI_SHOULD(TEXT, ANY, Constants.DOI_KEYWORD),
    ID(KEYWORD, Constants.IDENTIFIER),
    ID_NOT(KEYWORD, NONE, Constants.IDENTIFIER),
    ID_SHOULD(TEXT, ANY, Constants.IDENTIFIER),
    IMPORT_STATUS(KEYWORD, STATUS_TYPE_KEYWORD),
    IMPORT_STATUS_NOT(KEYWORD, NONE, STATUS_TYPE_KEYWORD),
    IMPORT_STATUS_SHOULD(TEXT, ANY, STATUS_TYPE_KEYWORD),
    INSTANCE_TYPE(KEYWORD, ALL, INSTANCE_TYPE_KEYWORD),
    INSTANCE_TYPE_NOT(KEYWORD, NONE, INSTANCE_TYPE_KEYWORD),
    INSTANCE_TYPE_SHOULD(KEYWORD, ANY, INSTANCE_TYPE_KEYWORD),
    PUBLICATION_YEAR(KEYWORD, ALL, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_BEFORE(NUMBER, FieldOperator.LESS_THAN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_SINCE(NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLICATION_YEAR_KEYWORD),
    SCOPUS_IDENTIFIER(KEYWORD),
    TITLE(TEXT, ALL, Constants.MAIN_TITLE_KEYWORD, 2F),
    TITLE_NOT(TEXT, NONE, Constants.MAIN_TITLE_KEYWORD),
    TITLE_SHOULD(TEXT, ANY, Constants.MAIN_TITLE_KEYWORD, 2F),
    TYPE(KEYWORD, Constants.TYPE_KEYWORD),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, ALL, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    FIELDS(ParameterKind.CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SORT(SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(ParameterKind.CUSTOM, ALL, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    SEARCH_AFTER(ParameterKind.CUSTOM);

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
        this(kind, ALL, null, null, null, null);
    }

    ImportCandidateParameter(ParameterKind kind, String fieldsToSearch) {
        this(kind, ALL, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch, Float boost) {
        this(kind, operator, fieldsToSearch, null, null, boost);
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