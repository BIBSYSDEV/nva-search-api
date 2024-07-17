package no.unit.nva.indexingclient.models;

import no.unit.nva.identifiers.SortableIdentifier;

public record EventConsumptionAttributes(
    String index,
    SortableIdentifier documentIdentifier
) {
}
