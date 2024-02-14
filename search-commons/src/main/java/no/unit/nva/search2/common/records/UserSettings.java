package no.unit.nva.search2.common.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;


@JsonInclude
public record UserSettings(
    List<String> promotedPublications
) {
}
