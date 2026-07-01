package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SchemaOrgItem(
    @JsonProperty("@type") String type,
    @JsonProperty("@id") String id,
    String url,
    String name,
    List<SchemaOrgPerson> author,
    List<SchemaOrgPerson> editor,
    List<SchemaOrgPerson> translator,
    List<SchemaOrgPerson> illustrator,
    List<SchemaOrgPerson> producer,
    List<SchemaOrgPerson> director,
    List<SchemaOrgPerson> actor,
    List<SchemaOrgPerson> composer,
    List<SchemaOrgPerson> contributor,
    String datePublished,
    @JsonProperty("abstract") String abstractText,
    String keywords,
    String identifier,
    SchemaOrgContainer isPartOf,
    String isbn,
    String numberOfPages,
    SchemaOrgOrganization publisher,
    String pageStart,
    String pageEnd) {}
