package no.unit.nva.search.models;

import no.unit.nva.identifiers.SortableIdentifier;

public record EventConsumptionAttributes(
    String index,
    SortableIdentifier documentIdentifier
) {
}
