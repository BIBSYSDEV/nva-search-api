package no.unit.nva.search2.ticket;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search2.common.constant.Words.CHAR_UNDERSCORE;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.PHI;
import static no.unit.nva.search2.common.constant.Words.Q;
import static no.unit.nva.search2.common.constant.Words.UNDERSCORE;
import static no.unit.nva.search2.common.enums.FieldOperator.ALL_ITEMS;
import static no.unit.nva.search2.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search2.common.enums.FieldOperator.NOT_ONE_ITEM;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static no.unit.nva.search2.common.enums.FieldOperator.ONE_OR_MORE_ITEM;
import static no.unit.nva.search2.common.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search2.common.enums.ParameterKind.DATE;
import static no.unit.nva.search2.common.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search2.common.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.FUZZY_TEXT;
import static no.unit.nva.search2.common.enums.ParameterKind.IGNORED;
import static no.unit.nva.search2.common.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search2.common.enums.ParameterKind.NUMBER;
import static no.unit.nva.search2.common.enums.ParameterKind.TEXT;
import static no.unit.nva.search2.ticket.Constants.ASSIGNEE_FIELDS;
import static no.unit.nva.search2.ticket.Constants.CUSTOMER_ID_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.FINALIZED_BY_FIELDS;
import static no.unit.nva.search2.ticket.Constants.ID_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.MESSAGE_FIELDS;
import static no.unit.nva.search2.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.OWNER_FIELDS;
import static no.unit.nva.search2.ticket.Constants.PUBLICATION_ID_OR_IDENTIFIER_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.PUBLICATION_MAIN_TITLE_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.PUBLICATION_OWNER_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.PUBLICATION_STATUS_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.VIEWED_BY_FIELDS;
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
import org.apache.commons.text.CaseUtils;

/**
 * Enum for all the parameters that can be used to query the search index. This enum needs to implement these
 * parameters
 * <a href="https://api.cristin.no/v2/doc/index.html#GETresults">cristin API</a>
 */

public enum TicketParameter implements ParameterKey {
    INVALID(ParameterKind.INVALID),
    // Parameters used for filtering
    ASSIGNEE(CUSTOM, ALL_ITEMS, ASSIGNEE_FIELDS),
    ASSIGNEE_NOT(TEXT, NO_ITEMS, ASSIGNEE_FIELDS),
    BY_USER_PENDING(IGNORED),
    CREATED_DATE(DATE, BETWEEN, Words.CREATED_DATE),
    CUSTOMER_ID(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, CUSTOMER_ID_KEYWORD),
    CUSTOMER_ID_NOT(FUZZY_KEYWORD, NOT_ONE_ITEM, CUSTOMER_ID_KEYWORD),
    ID(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, ID_KEYWORD),
    ID_NOT(FUZZY_KEYWORD, NOT_ONE_ITEM, ID_KEYWORD),
    FINALIZED_BY(TEXT, ALL_ITEMS, FINALIZED_BY_FIELDS),
    FINALIZED_BY_NOT(TEXT, NO_ITEMS, FINALIZED_BY_FIELDS),
    MESSAGES(FUZZY_TEXT, ALL_ITEMS, MESSAGE_FIELDS),
    MESSAGES_NOT(FUZZY_TEXT, NO_ITEMS, MESSAGE_FIELDS),
    MODIFIED_DATE(DATE, BETWEEN, Words.MODIFIED_DATE),
    ORGANIZATION_ID(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, ORGANIZATION_ID_KEYWORD),
    ORGANIZATION_ID_NOT(FUZZY_KEYWORD, NOT_ONE_ITEM, ORGANIZATION_ID_KEYWORD),
    OWNER(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, OWNER_FIELDS),
    OWNER_NOT(FUZZY_KEYWORD, NOT_ONE_ITEM, OWNER_FIELDS),
    PUBLICATION_ID(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, PUBLICATION_ID_OR_IDENTIFIER_KEYWORD),
    PUBLICATION_ID_NOT(FUZZY_KEYWORD, NOT_ONE_ITEM, PUBLICATION_ID_OR_IDENTIFIER_KEYWORD),
    PUBLICATION_MODIFIED_DATE(DATE, BETWEEN, Constants.PUBLICATION_MODIFIED_DATE),
    PUBLICATION_OWNER(FUZZY_KEYWORD, ONE_OR_MORE_ITEM, PUBLICATION_OWNER_KEYWORD),
    PUBLICATION_OWNER_NOT(FUZZY_KEYWORD, NOT_ONE_ITEM, PUBLICATION_OWNER_KEYWORD),
    PUBLICATION_STATUS(KEYWORD, ONE_OR_MORE_ITEM, PUBLICATION_STATUS_KEYWORD),
    PUBLICATION_STATUS_NOT(KEYWORD, NOT_ONE_ITEM, PUBLICATION_STATUS_KEYWORD),
    PUBLICATION_TITLE(FUZZY_TEXT, ALL_ITEMS, PUBLICATION_MAIN_TITLE_KEYWORD, PHI),
    STATUS(KEYWORD, ONE_OR_MORE_ITEM, STATUS_KEYWORD),
    STATUS_NOT(KEYWORD, NOT_ONE_ITEM, STATUS_KEYWORD),
    TYPE(KEYWORD, ONE_OR_MORE_ITEM, TYPE_KEYWORD),
    TYPE_NOT(KEYWORD, NOT_ONE_ITEM, TYPE_KEYWORD),
    VIEWED_BY(TEXT, ALL_ITEMS, VIEWED_BY_FIELDS),
    VIEWED_BY_NOT(TEXT, NO_ITEMS, VIEWED_BY_FIELDS),

    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(FREE_TEXT, ALL_ITEMS, Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    FIELDS(IGNORED),
    // Pagination parameters
    AGGREGATION(IGNORED),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SORT(ParameterKind.SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(IGNORED, ALL_ITEMS, null, PATTERN_IS_SORT_ORDER_KEY, PATTERN_IS_ASC_DESC_VALUE, null),
    SEARCH_AFTER(IGNORED);

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
        this(kind, ALL_ITEMS, null, null, null, null);
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

        this.fieldOperator = operator;
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
}