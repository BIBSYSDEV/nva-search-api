package no.unit.nva.search2.common.records;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;

public record LogRequestException(URI requestUri, Exception exception) implements JsonSerializable {

}
