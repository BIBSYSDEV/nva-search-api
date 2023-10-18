package no.unit.nva.search;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.unit.nva.commons.json.JsonSerializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSerialize
public record KeyBatchMessage(String lastEvaluatedKey) implements JsonSerializable {

    @Override
    public String toString() {
        return this.toJsonString();
    }
}
