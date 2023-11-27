package no.unit.nva.search2.enums;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.UNDERSCORE;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import no.unit.nva.search2.constant.ImportCandidateFields;
import nva.commons.core.JacocoGenerated;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to implement these
 * parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */

public enum ImportCandidateParameter implements ParameterKey<ImportCandidateParameter> {
    INVALID(ParamKind.TEXT),
    // Parameters converted to Lucene query
    ADDITIONAL_IDENTIFIERS(ParamKind.KEYWORD, FieldOperator.MUST, ImportCandidateFields.ADDITIONAL_IDENTIFIERS_VALUE),
    ADDITIONAL_IDENTIFIERS_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT,
                               ImportCandidateFields.ADDITIONAL_IDENTIFIERS_VALUE),
    ADDITIONAL_IDENTIFIERS_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD,
                                  ImportCandidateFields.ADDITIONAL_IDENTIFIERS_VALUE),
    CATEGORY(ParamKind.KEYWORD, ImportCandidateFields.PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT,
                 ImportCandidateFields.PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD,
                    ImportCandidateFields.PUBLICATION_INSTANCE_TYPE),
    CREATED_DATE(ParamKind.DATE, ImportCandidateFields.CREATED_DATE),
    CONTRIBUTOR(ParamKind.KEYWORD, ImportCandidateFields.CONTRIBUTOR),
    CONTRIBUTOR_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ImportCandidateFields.CONTRIBUTOR),
    CONTRIBUTOR_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.CONTRIBUTOR),
    COLLABORATION_TYPE(ParamKind.KEYWORD, FieldOperator.MUST, ImportCandidateFields.COLLABORATION_TYPE),
    COLLABORATION_TYPE_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ImportCandidateFields.COLLABORATION_TYPE),
    COLLABORATION_TYPE_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.COLLABORATION_TYPE),
    DOI(ParamKind.KEYWORD, ImportCandidateFields.DOI),
    DOI_NOT(ParamKind.TEXT, FieldOperator.MUST_NOT, ImportCandidateFields.DOI),
    DOI_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.DOI),
    ID(ParamKind.KEYWORD, ImportCandidateFields.IDENTIFIER),
    ID_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ImportCandidateFields.IDENTIFIER),
    ID_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.IDENTIFIER),
    IMPORT_STATUS(ParamKind.KEYWORD, ImportCandidateFields.IMPORT_STATUS),
    IMPORT_STATUS_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ImportCandidateFields.IMPORT_STATUS),
    IMPORT_STATUS_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.IMPORT_STATUS),
    INSTANCE_TYPE(ParamKind.KEYWORD, FieldOperator.MUST, ImportCandidateFields.INSTANCE_TYPE),
    INSTANCE_TYPE_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ImportCandidateFields.INSTANCE_TYPE),
    INSTANCE_TYPE_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.INSTANCE_TYPE),
    PUBLISHED_BEFORE(ParamKind.NUMBER, FieldOperator.LESS_THAN, ImportCandidateFields.PUBLICATION_YEAR),
    PUBLISHED_SINCE(ParamKind.NUMBER, FieldOperator.GREATER_THAN_OR_EQUAL_TO, ImportCandidateFields.PUBLICATION_YEAR),
    PUBLISHER(ParamKind.KEYWORD, FieldOperator.MUST, ImportCandidateFields.PUBLISHER),
    PUBLISHER_NOT(ParamKind.KEYWORD, FieldOperator.MUST_NOT, ImportCandidateFields.PUBLISHER),
    PUBLISHER_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.PUBLISHER),
    TITLE(ParamKind.TEXT, ImportCandidateFields.MAIN_TITLE, 2F),
    TITLE_NOT(ParamKind.TEXT, FieldOperator.MUST_NOT, ImportCandidateFields.MAIN_TITLE),
    TITLE_SHOULD(ParamKind.TEXT, FieldOperator.SHOULD, ImportCandidateFields.MAIN_TITLE),
    TYPE(ParamKind.KEYWORD, ImportCandidateFields.TYPE),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(ParamKind.TEXT, FieldOperator.MUST, "q", "(?i)search.?all|query", null, null),
    FIELDS(ParamKind.CUSTOM),
    // Pagination parameters
    PAGE(ParamKind.NUMBER),
    FROM(ParamKind.NUMBER, null, null, "(?i)offset|from", null, null),
    SIZE(ParamKind.NUMBER, null, null, "(?i)per.?page|results|limit|size", null, null),
    SORT(ParamKind.SORT_KEY, null, null, "(?i)order.?by|sort", null, null),
    SORT_ORDER(ParamKind.CUSTOM, FieldOperator.MUST, null, "(?i)sort.?order|order", "(?i)asc|desc", null),
    SEARCH_AFTER(ParamKind.CUSTOM);

    //    COLLABORATION_TYPE("collaborationType.keyword"),
    //    CREATED_DATE("createdDate"),
    //    INSTANCE_TYPE("publicationInstance.keyword"),
    //    PUBLICATION_YEAR("publicationYear"),
    //    TITLE("mainTitle.keyword"),
    //    TYPE("type.keyword");
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

    ImportCandidateParameter(ParamKind kind) {
        this(kind, FieldOperator.MUST, null, null, null, null);
    }

    ImportCandidateParameter(ParamKind kind, String fieldsToSearch) {
        this(kind, FieldOperator.MUST, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, FieldOperator.MUST, fieldsToSearch, null, null, boost);
    }

    ImportCandidateParameter(ParamKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    ImportCandidateParameter(
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