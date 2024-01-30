package no.unit.nva.search2.enums;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ImportCandidate.ADDITIONAL_IDENTIFIERS_KEYWORD;
import static no.unit.nva.search2.constant.ImportCandidate.COLLABORATION_TYPE_KEYWORD;
import static no.unit.nva.search2.constant.ImportCandidate.INSTANCE_TYPE_KEYWORD;
import static no.unit.nva.search2.constant.ImportCandidate.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.constant.ImportCandidate.PUBLICATION_YEAR_KEYWORD;
import static no.unit.nva.search2.constant.ImportCandidate.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search2.constant.ImportCandidate.STATUS_TYPE_KEYWORD;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.Q;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.enums.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.KEYWORD;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.SORT_KEY;
import static no.unit.nva.search2.enums.ParameterKey.ParamKind.TEXT;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.constant.ImportCandidate;
import no.unit.nva.search2.constant.Words;
import nva.commons.core.JacocoGenerated;

/**
 * Enum for all the parameters that can be used to query the search index for import candidates.
 * <p>
 *     Parameter values can be read as camelCase or snake_case (e.g. additionalIdentifiers or additional_identifiers).
 * </p>
 * <p>
 *     Values are: (INVALID,) ADDITIONAL_IDENTIFIERS, CATEGORY, CREATED_DATE, CONTRIBUTOR, CONTRIBUTOR_NAME,
 *     COLLABORATION_TYPE, DOI, ID, IMPORT_STATUS, INSTANCE_TYPE, PUBLICATION_YEAR, PUBLISHER, TITLE, TYPE,
 *     SEARCH_ALL, FIELDS, PAGE, FROM, SIZE, SORT, SORT_ORDER, SEARCH_AFTER
 * </p>
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */
public enum ImportCandidateParameter implements ParameterKey {
    INVALID(TEXT),
    // Parameters converted to Lucene query
    CRISTIN_IDENTIFIER(KEYWORD),
    SCOPUS_IDENTIFIER(KEYWORD),
    ADDITIONAL_IDENTIFIERS_NOT(KEYWORD, MUST_NOT, ADDITIONAL_IDENTIFIERS_KEYWORD),
    ADDITIONAL_IDENTIFIERS_SHOULD(TEXT, SHOULD, ADDITIONAL_IDENTIFIERS_KEYWORD),
    CATEGORY(KEYWORD, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, MUST_NOT, PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(TEXT, SHOULD, PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(ParamKind.DATE, Words.CREATED_DATE),
    CONTRIBUTOR(KEYWORD, ImportCandidate.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NOT(KEYWORD, MUST_NOT, ImportCandidate.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_SHOULD(TEXT, SHOULD, ImportCandidate.CONTRIBUTOR_IDENTITY_KEYWORDS),
    CONTRIBUTOR_NAME(KEYWORD, ImportCandidate.CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NAME_NOT(KEYWORD, MUST_NOT, ImportCandidate.CONTRIBUTORS_IDENTITY_NAME),
    CONTRIBUTOR_NAME_SHOULD(TEXT, SHOULD, ImportCandidate.CONTRIBUTORS_IDENTITY_NAME),
    COLLABORATION_TYPE(KEYWORD, MUST, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_NOT(KEYWORD, MUST_NOT, COLLABORATION_TYPE_KEYWORD),
    COLLABORATION_TYPE_SHOULD(TEXT, SHOULD, COLLABORATION_TYPE_KEYWORD),
    DOI(KEYWORD, ImportCandidate.DOI_KEYWORD),
    DOI_NOT(TEXT, MUST_NOT, ImportCandidate.DOI_KEYWORD),
    DOI_SHOULD(TEXT, SHOULD, ImportCandidate.DOI_KEYWORD),
    ID(KEYWORD, ImportCandidate.IDENTIFIER),
    ID_NOT(KEYWORD, MUST_NOT, ImportCandidate.IDENTIFIER),
    ID_SHOULD(TEXT, SHOULD, ImportCandidate.IDENTIFIER),
    IMPORT_STATUS(KEYWORD, STATUS_TYPE_KEYWORD),
    IMPORT_STATUS_NOT(KEYWORD, MUST_NOT, STATUS_TYPE_KEYWORD),
    IMPORT_STATUS_SHOULD(TEXT, SHOULD, STATUS_TYPE_KEYWORD),
    INSTANCE_TYPE(KEYWORD, MUST, INSTANCE_TYPE_KEYWORD),
    INSTANCE_TYPE_NOT(KEYWORD, MUST_NOT, INSTANCE_TYPE_KEYWORD),
    INSTANCE_TYPE_SHOULD(TEXT, SHOULD, INSTANCE_TYPE_KEYWORD),
    PUBLICATION_YEAR(KEYWORD, MUST, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_BEFORE(NUMBER, FieldOperator.LESS_THAN, PUBLICATION_YEAR_KEYWORD),
    PUBLICATION_YEAR_SINCE(NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, PUBLICATION_YEAR_KEYWORD),
    PUBLISHER(KEYWORD, MUST, PUBLISHER_ID_KEYWORD),
    PUBLISHER_NOT(KEYWORD, MUST_NOT, PUBLISHER_ID_KEYWORD),
    PUBLISHER_SHOULD(TEXT, SHOULD, PUBLISHER_ID_KEYWORD),
    TITLE(TEXT, ImportCandidate.MAIN_TITLE_KEYWORD, 2F),
    TITLE_NOT(TEXT, MUST_NOT, ImportCandidate.MAIN_TITLE_KEYWORD),
    TITLE_SHOULD(TEXT, SHOULD, ImportCandidate.MAIN_TITLE_KEYWORD),
    TYPE(KEYWORD, ImportCandidate.TYPE_KEYWORD),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, MUST, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null, null),
    FIELDS(ParamKind.CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null, null),
    SORT(SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null, null),
    SORT_ORDER(ParamKind.CUSTOM, MUST, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null, null),
    SEARCH_AFTER(ParamKind.CUSTOM);

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
    private final ParamKind paramkind;
    private final Float boost;
    private final Boolean isNested;

    ImportCandidateParameter(ParamKind kind) {
        this(kind, MUST, null, null, null, null, null);
    }

    ImportCandidateParameter(ParamKind kind, String fieldsToSearch) {
        this(kind, MUST, fieldsToSearch, null, null, null, null);
    }

    ImportCandidateParameter(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, MUST, fieldsToSearch, null, null, boost, null);
    }

    ImportCandidateParameter(ParamKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null, null);
    }

    ImportCandidateParameter(
        ParamKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern, String valuePattern,
        Float boost, Boolean isNested) {

        this.key = this.name().toLowerCase(Locale.getDefault());
        this.fieldOperator = operator;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
            ? fieldsToSearch.split("\\|")
            : new String[] {key};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern = nonNull(keyPattern)
            ? keyPattern
            : PATTERN_IS_IGNORE_CASE + key.replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.paramkind = kind;
        this.isNested = isNull(isNested) ? false : isNested;
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
    public Boolean isNested() {
        return isNested;
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