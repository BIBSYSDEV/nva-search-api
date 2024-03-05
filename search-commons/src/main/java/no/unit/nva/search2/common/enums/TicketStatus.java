package no.unit.nva.search2.common.enums;

public enum TicketStatus {
    NEW("New"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    NOT_APPLICABLE("Not Applicable"),
    CLOSED("Closed");

    private final String code;
    TicketStatus(String name) {
        this.code = name;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
