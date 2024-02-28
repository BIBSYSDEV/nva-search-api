package no.unit.nva.search2.common.enums;

public enum TicketStatus {
    NEW("New"),
    COMPLETED("Completed");

    private final String code;
    TicketStatus(String name) {
        this.code = name;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
