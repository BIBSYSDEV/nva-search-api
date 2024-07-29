package no.unit.nva.search.ticket;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_ORGANIZATION;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.common.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search.common.constant.Words.COLON;
import static no.unit.nva.search.common.constant.Words.PHI;
import static no.unit.nva.search.common.constant.Words.PIPE;
import static no.unit.nva.search.common.constant.Words.Q;
import static no.unit.nva.search.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search.common.enums.FieldOperator.ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.ANY_OF;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.NA;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ANY_OF;
import static no.unit.nva.search.common.enums.ParameterKind.ACROSS_FIELDS;
import static no.unit.nva.search.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.common.enums.ParameterKind.DATE;
import static no.unit.nva.search.common.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.IGNORED;
import static no.unit.nva.search.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search.ticket.Constants.ASSIGNEE_FIELDS;
import static no.unit.nva.search.ticket.Constants.CUSTOMER_ID_KEYWORD;
import static no.unit.nva.search.ticket.Constants.FINALIZED_BY_FIELDS;
import static no.unit.nva.search.ticket.Constants.ID_KEYWORD;
import static no.unit.nva.search.ticket.Constants.MESSAGE_FIELDS;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_IDENTIFIER_KEYWORD;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_PATHS;
import static no.unit.nva.search.ticket.Constants.OWNER_FIELDS;
import static no.unit.nva.search.ticket.Constants.PUBLICATION_ID_OR_IDENTIFIER_KEYWORD;
import static no.unit.nva.search.ticket.Constants.PUBLICATION_INSTANCE_KEYWORD;
import static no.unit.nva.search.ticket.Constants.PUBLICATION_MAIN_TITLE_KEYWORD;
import static no.unit.nva.search.ticket.Constants.PUBLICATION_OWNER_KEYWORD;
import static no.unit.nva.search.ticket.Constants.PUBLICATION_STATUS_KEYWORD;
import static no.unit.nva.search.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.ticket.Constants.VIEWED_BY_FIELDS;

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
 *
 * @author Stig Norland
 */
public enum TicketParameter implements ParameterKey {
    INVALID(ParameterKind.INVALID),
    // Parameters used for filtering
    ASSIGNEE(CUSTOM, ANY_OF, ASSIGNEE_FIELDS),
    ASSIGNEE_NOT(ACROSS_FIELDS, NOT_ANY_OF, ASSIGNEE_FIELDS),
    BY_USER_PENDING(IGNORED),
    CREATED_DATE(DATE, BETWEEN, Words.CREATED_DATE),
    CUSTOMER_ID(FUZZY_KEYWORD, ANY_OF, CUSTOMER_ID_KEYWORD),
    CUSTOMER_ID_NOT(FUZZY_KEYWORD, NOT_ANY_OF, CUSTOMER_ID_KEYWORD),
    ID(FUZZY_KEYWORD, ANY_OF, ID_KEYWORD),
    ID_NOT(FUZZY_KEYWORD, NOT_ANY_OF, ID_KEYWORD),
    EXCLUDE_SUBUNITS(IGNORED, ANY_OF, ORGANIZATION_ID_KEYWORD + PIPE + ORGANIZATION_IDENTIFIER_KEYWORD),
    FINALIZED_BY(ACROSS_FIELDS, ALL_OF, FINALIZED_BY_FIELDS),
    FINALIZED_BY_NOT(ACROSS_FIELDS, NOT_ALL_OF, FINALIZED_BY_FIELDS),
    MESSAGES(TEXT, ALL_OF, MESSAGE_FIELDS),
    MESSAGES_NOT(TEXT, NOT_ALL_OF, MESSAGE_FIELDS),
    MODIFIED_DATE(DATE, BETWEEN, Words.MODIFIED_DATE),
    ORGANIZATION_ID(CUSTOM, ANY_OF, ORGANIZATION_PATHS, PATTERN_IS_ORGANIZATION, null, null),
    ORGANIZATION_ID_NOT(CUSTOM, NOT_ANY_OF, ORGANIZATION_PATHS),
    OWNER(ACROSS_FIELDS, ANY_OF, OWNER_FIELDS),
    OWNER_NOT(ACROSS_FIELDS, NOT_ANY_OF, OWNER_FIELDS),
    PUBLICATION_ID(FUZZY_KEYWORD, ANY_OF, PUBLICATION_ID_OR_IDENTIFIER_KEYWORD),
    PUBLICATION_ID_NOT(FUZZY_KEYWORD, NOT_ANY_OF, PUBLICATION_ID_OR_IDENTIFIER_KEYWORD),
    PUBLICATION_TYPE(FUZZY_KEYWORD, ANY_OF, PUBLICATION_INSTANCE_KEYWORD),
    PUBLICATION_TYPE_NOT(FUZZY_KEYWORD, NOT_ANY_OF, PUBLICATION_INSTANCE_KEYWORD),
    PUBLICATION_MODIFIED_DATE(DATE, BETWEEN, Constants.PUBLICATION_MODIFIED_DATE),
    PUBLICATION_OWNER(FUZZY_KEYWORD, ANY_OF, PUBLICATION_OWNER_KEYWORD),
    PUBLICATION_OWNER_NOT(FUZZY_KEYWORD, NOT_ANY_OF, PUBLICATION_OWNER_KEYWORD),
    PUBLICATION_STATUS(KEYWORD, ANY_OF, PUBLICATION_STATUS_KEYWORD),
    PUBLICATION_STATUS_NOT(KEYWORD, NOT_ANY_OF, PUBLICATION_STATUS_KEYWORD),
    PUBLICATION_TITLE(TEXT, ALL_OF, PUBLICATION_MAIN_TITLE_KEYWORD, PHI),
    STATUS(CUSTOM, ANY_OF, STATUS_KEYWORD),
    STATUS_NOT(CUSTOM, NOT_ANY_OF, STATUS_KEYWORD),
    TYPE(KEYWORD, ANY_OF, TYPE_KEYWORD),
    TYPE_NOT(KEYWORD, NOT_ANY_OF, TYPE_KEYWORD),
    VIEWED_BY(ACROSS_FIELDS, ALL_OF, VIEWED_BY_FIELDS),
    VIEWED_BY_NOT(ACROSS_FIELDS, NOT_ALL_OF, VIEWED_BY_FIELDS),

    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(FREE_TEXT, ALL_OF, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    NODES_SEARCHED(IGNORED, null, null, PATTERN_IS_FIELDS_SEARCHED, null, null),
    NODES_INCLUDED(IGNORED),
    NODES_EXCLUDED(IGNORED),
    // Pagination parameters
    AGGREGATION(IGNORED),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SEARCH_AFTER(IGNORED),
    SORT(ParameterKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(IGNORED, ALL_OF, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    ;

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<TicketParameter> TICKET_PARAMETER_SET =
        Arrays.stream(TicketParameter.values())
            .filter(TicketParameter::isSearchField)
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

    TicketParameter(ParameterKind kind) {
        this(kind, ALL_OF, null, null, null, null);
    }

    TicketParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    TicketParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch, Float boost) {
        this(kind, operator, fieldsToSearch, null, null, boost);
    }

    TicketParameter(
        ParameterKind kind, FieldOperator operator, String fieldsToSearch, String keyPattern, String valuePattern,
        Float boost) {

        this.fieldOperator = nonNull(operator) ? operator : NA;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch = nonNull(fieldsToSearch)
            ? fieldsToSearch.split("\\|")
            : new String[]{name()};
        this.validValuePattern = ParameterKey.getValuePattern(kind, valuePattern);
        this.errorMsg = ParameterKey.getErrorMessage(kind);
        this.encoding = ParameterKey.getEncoding(kind);
        this.keyPattern = nonNull(keyPattern)
            ? keyPattern
            : PATTERN_IS_IGNORE_CASE + name().replace(UNDERSCORE, PATTERN_IS_NONE_OR_ONE);
        this.paramkind = kind;
    }

    public static TicketParameter keyFromString(String paramName) {
        var result = Arrays.stream(TicketParameter.values())
            .filter(TicketParameter::ignoreInvalidKey)
            .filter(ParameterKey.equalTo(paramName))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : INVALID;
    }

    private static boolean ignoreInvalidKey(TicketParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(TicketParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX && enumParameter.ordinal() < SEARCH_ALL.ordinal();
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
}