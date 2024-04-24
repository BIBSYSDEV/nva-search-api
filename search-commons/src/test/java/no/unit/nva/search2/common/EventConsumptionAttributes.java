package no.unit.nva.search2.common;

import no.unit.nva.identifiers.SortableIdentifier;

public record EventConsumptionAttributes(
    String index,
    SortableIdentifier documentIdentifier
) {

}
