package no.unit.nva.search.service.ticket;

import static no.unit.nva.search.model.constant.ErrorMessages.NOT_IMPLEMENTED_FOR;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_ASC_DESC_VALUE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_FIELDS_SEARCHED;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_FROM_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_NONE_OR_ONE;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_ORGANIZATION;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SEARCH_ALL_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SIZE_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SORT_KEY;
import static no.unit.nva.search.model.constant.Patterns.PATTERN_IS_SORT_ORDER_KEY;
import static no.unit.nva.search.model.enums.ParameterKind.ACROSS_FIELDS;
import static no.unit.nva.search.model.enums.ParameterKind.CUSTOM;
import static no.unit.nva.search.model.enums.ParameterKind.DATE;
import static no.unit.nva.search.model.enums.ParameterKind.FLAG;
import static no.unit.nva.search.model.enums.ParameterKind.FREE_TEXT;
import static no.unit.nva.search.model.enums.ParameterKind.FUZZY_KEYWORD;
import static no.unit.nva.search.model.enums.ParameterKind.KEYWORD;
import static no.unit.nva.search.model.enums.ParameterKind.NUMBER;
import static no.unit.nva.search.model.enums.ParameterKind.SORT_KEY;
import static no.unit.nva.search.model.enums.ParameterKind.TEXT;
import static no.unit.nva.search.service.ticket.Constants.ASSIGNEE_FIELDS;
import static no.unit.nva.search.service.ticket.Constants.CUSTOMER_ID_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.FINALIZED_BY_FIELDS;
import static no.unit.nva.search.service.ticket.Constants.ID_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.MESSAGE_FIELDS;
import static no.unit.nva.search.service.ticket.Constants.ORGANIZATION_IDENTIFIER_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.ORGANIZATION_PATHS;
import static no.unit.nva.search.service.ticket.Constants.OWNER_FIELDS;
import static no.unit.nva.search.service.ticket.Constants.PUBLICATION_ID_OR_IDENTIFIER_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.PUBLICATION_INSTANCE_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.PUBLICATION_MAIN_TITLE_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.PUBLICATION_OWNER_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.PUBLICATION_STATUS_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.service.ticket.Constants.VIEWED_BY_FIELDS;

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
public enum TicketParameter implements ParameterKey<TicketParameter> {
    INVALID(ParameterKind.INVALID),
    // Parameters used for filtering
    ASSIGNEE(CUSTOM, FieldOperator.ANY_OF, ASSIGNEE_FIELDS),
    ASSIGNEE_NOT(ACROSS_FIELDS, FieldOperator.NOT_ANY_OF, ASSIGNEE_FIELDS),
    BY_USER_PENDING(FLAG),
    CREATED_DATE(DATE, FieldOperator.BETWEEN, Words.CREATED_DATE),
    CUSTOMER_ID(FUZZY_KEYWORD, FieldOperator.ANY_OF, CUSTOMER_ID_KEYWORD),
    CUSTOMER_ID_NOT(FUZZY_KEYWORD, FieldOperator.NOT_ANY_OF, CUSTOMER_ID_KEYWORD),
    EXCLUDE_SUBUNITS(
            FLAG,
            FieldOperator.ANY_OF,
            ORGANIZATION_ID_KEYWORD + Words.PIPE + ORGANIZATION_IDENTIFIER_KEYWORD),
    FINALIZED_BY(ACROSS_FIELDS, FieldOperator.ALL_OF, FINALIZED_BY_FIELDS),
    FINALIZED_BY_NOT(ACROSS_FIELDS, FieldOperator.NOT_ALL_OF, FINALIZED_BY_FIELDS),
    ID(FUZZY_KEYWORD, FieldOperator.ANY_OF, ID_KEYWORD),
    ID_NOT(FUZZY_KEYWORD, FieldOperator.NOT_ANY_OF, ID_KEYWORD),
    MESSAGES(TEXT, FieldOperator.ALL_OF, MESSAGE_FIELDS),
    MESSAGES_NOT(TEXT, FieldOperator.NOT_ALL_OF, MESSAGE_FIELDS),
    MODIFIED_DATE(DATE, FieldOperator.BETWEEN, Words.MODIFIED_DATE),
    ORGANIZATION_ID(
            CUSTOM, FieldOperator.ANY_OF, ORGANIZATION_PATHS, PATTERN_IS_ORGANIZATION, null, null),
    ORGANIZATION_ID_NOT(CUSTOM, FieldOperator.NOT_ANY_OF, ORGANIZATION_PATHS),
    OWNER(ACROSS_FIELDS, FieldOperator.ANY_OF, OWNER_FIELDS),
    OWNER_NOT(ACROSS_FIELDS, FieldOperator.NOT_ANY_OF, OWNER_FIELDS),
    PUBLICATION_ID(FUZZY_KEYWORD, FieldOperator.ANY_OF, PUBLICATION_ID_OR_IDENTIFIER_KEYWORD),
    PUBLICATION_ID_NOT(
            FUZZY_KEYWORD, FieldOperator.NOT_ANY_OF, PUBLICATION_ID_OR_IDENTIFIER_KEYWORD),
    PUBLICATION_MODIFIED_DATE(DATE, FieldOperator.BETWEEN, Constants.PUBLICATION_MODIFIED_DATE),
    PUBLICATION_OWNER(FUZZY_KEYWORD, FieldOperator.ANY_OF, PUBLICATION_OWNER_KEYWORD),
    PUBLICATION_OWNER_NOT(FUZZY_KEYWORD, FieldOperator.NOT_ANY_OF, PUBLICATION_OWNER_KEYWORD),
    PUBLICATION_STATUS(KEYWORD, FieldOperator.ANY_OF, PUBLICATION_STATUS_KEYWORD),
    PUBLICATION_STATUS_NOT(KEYWORD, FieldOperator.NOT_ANY_OF, PUBLICATION_STATUS_KEYWORD),
    PUBLICATION_TITLE(TEXT, FieldOperator.ALL_OF, PUBLICATION_MAIN_TITLE_KEYWORD, Words.PHI),
    PUBLICATION_TYPE(FUZZY_KEYWORD, FieldOperator.ANY_OF, PUBLICATION_INSTANCE_KEYWORD),
    PUBLICATION_TYPE_NOT(FUZZY_KEYWORD, FieldOperator.NOT_ANY_OF, PUBLICATION_INSTANCE_KEYWORD),
    STATISTICS(FLAG),
    STATUS(CUSTOM, FieldOperator.ANY_OF, STATUS_KEYWORD),
    STATUS_NOT(CUSTOM, FieldOperator.NOT_ANY_OF, STATUS_KEYWORD),
    TYPE(KEYWORD, FieldOperator.ANY_OF, TYPE_KEYWORD),
    TYPE_NOT(KEYWORD, FieldOperator.NOT_ANY_OF, TYPE_KEYWORD),
    VIEWED_BY(ACROSS_FIELDS, FieldOperator.ALL_OF, VIEWED_BY_FIELDS),
    VIEWED_BY_NOT(ACROSS_FIELDS, FieldOperator.NOT_ALL_OF, VIEWED_BY_FIELDS),

    // Query parameters passed to SWS/Opensearch
    SEARCH_ALL(FREE_TEXT, FieldOperator.ALL_OF, Words.Q, PATTERN_IS_SEARCH_ALL_KEY, null, null),
    NODES_SEARCHED(FLAG, null, null, PATTERN_IS_FIELDS_SEARCHED, null, null),
    NODES_INCLUDED(FLAG),
    NODES_EXCLUDED(FLAG),
    // Pagination parameters
    AGGREGATION(FLAG),
    PAGE(NUMBER),
    FROM(NUMBER, null, null, PATTERN_IS_FROM_KEY, null, null),
    SIZE(NUMBER, null, null, PATTERN_IS_SIZE_KEY, null, null),
    SEARCH_AFTER(FLAG),
    SORT(SORT_KEY, null, null, PATTERN_IS_SORT_KEY, null, null),
    SORT_ORDER(
            FLAG,
            FieldOperator.ALL_OF,
            null,
            PATTERN_IS_SORT_ORDER_KEY,
            PATTERN_IS_ASC_DESC_VALUE,
            null),
    ;

    public static final int IGNORE_PARAMETER_INDEX = 0;

    public static final Set<TicketParameter> TICKET_PARAMETER_SET =
            Arrays.stream(values())
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
        this(kind, FieldOperator.ALL_OF, null, null, null, null);
    }

    TicketParameter(ParameterKind kind, FieldOperator operator, String fieldsToSearch) {
        this(kind, operator, fieldsToSearch, null, null, null);
    }

    TicketParameter(
            ParameterKind kind, FieldOperator operator, String fieldsToSearch, Float boost) {
        this(kind, operator, fieldsToSearch, null, null, boost);
    }

    TicketParameter(
            ParameterKind kind,
            FieldOperator operator,
            String fieldsToSearch,
            String keyPattern,
            String valuePattern,
            Float boost) {

        this.fieldOperator = nonNull(operator) ? operator : FieldOperator.NA;
        this.boost = nonNull(boost) ? boost : 1F;
        this.fieldsToSearch =
                nonNull(fieldsToSearch) ? fieldsToSearch.split("\\|") : new String[] {name()};
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

    public static TicketParameter keyFromString(String paramName) {
        var result =
                Arrays.stream(values())
                        .filter(TicketParameter::ignoreInvalidKey)
                        .filter(ParameterKey.equalTo(paramName))
                        .collect(Collectors.toSet());
        return result.size() == 1 ? result.stream().findFirst().get() : INVALID;
    }

    private static boolean ignoreInvalidKey(TicketParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX;
    }

    private static boolean isSearchField(TicketParameter enumParameter) {
        return enumParameter.ordinal() > IGNORE_PARAMETER_INDEX
                && enumParameter.ordinal() < SEARCH_ALL.ordinal();
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
    public TicketParameter subQuery() {
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
