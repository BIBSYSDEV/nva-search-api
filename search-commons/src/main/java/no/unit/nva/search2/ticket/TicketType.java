package no.unit.nva.search2.ticket;

public enum TicketType {
    NONE("None"),
    DOI_REQUEST("DoiRequest"),
    GENERAL_SUPPORT_CASE("GeneralSupportCase"),
    PUBLISHING_REQUEST("PublishingRequest");

    private final String type;

    TicketType(String typeName) {
        this.type = typeName;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
