package no.unit.nva.search2.dto;

import no.unit.nva.commons.json.JsonSerializable;

public record ResponseInfo(
    int totalHits,
    int opensearchStatusCode,
    long opensearchResponseTime,
    long responseTime
)  implements JsonSerializable {

}
