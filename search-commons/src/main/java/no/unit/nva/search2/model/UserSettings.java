package no.unit.nva.search2.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;


@JsonInclude
public record UserSettings(
    List<String> promotedPublications
) {
}
