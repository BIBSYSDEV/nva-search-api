package no.unit.nva.search.service.ticket;

import static no.unit.nva.search.model.enums.SortKey.getIgnoreCaseAndUnderscoreKeyExpression;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * TicketStatus is an enum for sorting tickets.
 *
 * @author Stig Norland
 */
public enum TicketStatus {
    NONE(EMPTY_STRING),
    NEW("New"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    NOT_APPLICABLE("NotApplicable"),
    CLOSED("Closed");

    private final String code;
    private final String keyValidationRegEx;

    TicketStatus(String name) {
        this.code = name;
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
    }

    public static TicketStatus fromString(String name) {
        var result = Arrays.stream(values()).filter(equalTo(name)).collect(Collectors.toSet());
        return result.size() == 1 ? result.stream().findFirst().get() : NONE;
    }

    private static Predicate<TicketStatus> equalTo(String name) {
        return status -> name.matches(status.keyValidationRegEx);
    }

    @Override
    public String toString() {
        return this.code;
    }
}
