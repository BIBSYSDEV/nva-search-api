package no.unit.nva.search2.ticket;

import static no.unit.nva.search2.common.enums.SortKey.getIgnoreCaseAndUnderscoreKeyExpression;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Stig Norland
 */
public enum TicketType {
    NONE("None"),
    DOI_REQUEST("DoiRequest"),
    GENERAL_SUPPORT_CASE("GeneralSupportCase"),
    PUBLISHING_REQUEST("PublishingRequest");

    private final String type;
    private final String keyValidationRegEx;

    TicketType(String typeName) {
        this.type = typeName;
        this.keyValidationRegEx = getIgnoreCaseAndUnderscoreKeyExpression(this.name());
    }

    public static TicketType fromString(String name) {
        var result = Arrays.stream(TicketType.values())
            .filter(equalTo(name))
            .collect(Collectors.toSet());
        return result.size() == 1
            ? result.stream().findFirst().get()
            : NONE;
    }

    private static Predicate<TicketType> equalTo(String name) {
        return type -> name.matches(type.keyValidationRegEx);
    }

    @Override
    public String toString() {
        return this.type;
    }
}
