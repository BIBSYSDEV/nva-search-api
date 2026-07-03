package no.unit.nva.search.common.bibliography;

sealed interface SchemaOrgContainer
    permits SchemaOrgBook,
        SchemaOrgBookSeries,
        SchemaOrgPeriodical,
        SchemaOrgPublicationIssue,
        SchemaOrgPublicationVolume {}
