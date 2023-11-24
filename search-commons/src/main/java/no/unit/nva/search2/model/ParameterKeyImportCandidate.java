package no.unit.nva.search2.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.UNDERSCORE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.GREATER_THAN_OR_EQUAL_TO;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.LESS_THAN;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.MUST;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.MUST_NOT;
import static no.unit.nva.search2.model.ParameterKey.FieldOperator.SHOULD;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.CUSTOM;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.KEYWORD;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.NUMBER;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.SORT_KEY;
import static no.unit.nva.search2.model.ParameterKey.ParamKind.TEXT;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to implement these
 * parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */

public enum ParameterKeyImportCandidate implements ParameterKey {
    INVALID(TEXT),
    // Parameters converted to Lucene query
    CATEGORY(KEYWORD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CATEGORY_NOT(KEYWORD, MUST_NOT, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    CATEGORY_SHOULD(TEXT, SHOULD, Constants.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
    COLLABORATION_TYPE(KEYWORD, MUST, ""),
    DOI(KEYWORD, Constants.DOI),
    DOI_NOT(TEXT, MUST_NOT, Constants.DOI),
    DOI_SHOULD(TEXT, SHOULD, Constants.DOI),
    ID(KEYWORD, Constants.IDENTIFIER),
    ID_NOT(KEYWORD, MUST_NOT, Constants.IDENTIFIER),
    ID_SHOULD(TEXT, SHOULD, Constants.IDENTIFIER),
    OWNER(KEYWORD, MUST, Constants.RESOURCE_OWNER),
    OWNER_NOT(KEYWORD, MUST_NOT, Constants.RESOURCE_OWNER),
    OWNER_SHOULD(TEXT, SHOULD, Constants.RESOURCE_OWNER),
    PUBLISHED_BEFORE(NUMBER, LESS_THAN, Constants.PUBLISHED_DATE),
    PUBLISHED_SINCE(NUMBER, GREATER_THAN_OR_EQUAL_TO, Constants.PUBLISHED_DATE),
    PUBLISHER(KEYWORD, MUST, Constants.PUBLISHER),
    PUBLISHER_NOT(KEYWORD, MUST_NOT, Constants.PUBLISHER),
    PUBLISHER_SHOULD(TEXT, SHOULD, Constants.PUBLISHER),
    TITLE(TEXT, Constants.MAIN_TITLE, 2F),
    TITLE_NOT(TEXT, MUST_NOT, Constants.MAIN_TITLE),
    TITLE_SHOULD(TEXT, SHOULD, Constants.MAIN_TITLE),
    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(TEXT, MUST, "q", "(?i)search.?all|query", null, null),
    FIELDS(CUSTOM),
    // Pagination parameters
    PAGE(NUMBER),
    FROM(NUMBER, null, null, "(?i)offset|from", null, null),
    SIZE(NUMBER, null, null, "(?i)per.?page|results|limit|size", null, null),
    SORT(SORT_KEY, null, null, "(?i)order.?by|sort", null, null),
    SORT_ORDER(CUSTOM, MUST, null, "(?i)sort.?order|order", "(?i)asc|desc", null),
    SEARCH_AFTER(CUSTOM);
    
    public static final int IGNORE_PARAMETER_INDEX = 0;
    
    public static final Set<ParameterKeyImportCandidate> VALID_LUCENE_PARAMETER_KEYS =
        Arrays.stream(ParameterKeyImportCandidate.values())
            .filter(ParameterKeyImportCandidate::isSearchField)
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
    
    ParameterKeyImportCandidate(ParamKind kind) {
        this(kind, MUST, null, null, null, null);
    }
    
    ParameterKeyImportCandidate(ParamKind kind, String fieldsToSearch) {
        this(kind, MUST, fieldsToSearch, null, null, null);
    }
    
    ParameterKeyImportCandidate(ParamKind kind, String fieldsToSearch, Float boost) {
        this(kind, MUST, fieldsToSearch, null, null, boost);
    }
    
    ParameterKeyImportCandidate(ParamKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }
    
    ParameterKeyImportCandidate(
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
    
    public static ParameterKeyImportCandidate keyFromString(String paramName) {
        var result = Arrays.stream(ParameterKeyImportCandidate.values())
            .filter(ParameterKeyImportCandidate::ignoreInvalidKey)
            .filter(ParameterKey.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }
    
    private static boolean ignoreInvalidKey(ParameterKeyImportCandidate f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX;
    }
    
    private static boolean isSearchField(ParameterKeyImportCandidate f) {
        return f.ordinal() > IGNORE_PARAMETER_INDEX && f.ordinal() < SEARCH_ALL.ordinal();
    }
    
    private static class Constants {
        
        public static final String DOI = "doi";
        public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE =
            "entityDescription.reference.publicationInstance.type.keyword";
        public static final String IDENTIFIER = "identifier.keyword";
        public static final String MAIN_TITLE = "title.keyword";
        public static final String PUBLISHED_DATE = "publishedDate.year";
        public static final String PUBLISHER = "publisher.id.keyword";
        public static final String RESOURCE_OWNER = "owner.keyword";
    }
}