<<<<<<<< HEAD:search-commons/src/main/java/no/unit/nva/search2/common/dto/UserSettings.java
package no.unit.nva.search2.common.dto;
========
package no.unit.nva.search2.common.records;
>>>>>>>> main:search-commons/src/main/java/no/unit/nva/search2/common/records/UserSettings.java

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;


@JsonInclude
public record UserSettings(
    List<String> promotedPublications
) {
}
