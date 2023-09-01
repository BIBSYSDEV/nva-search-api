package no.unit.nva.search2.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class ApplicationConstants {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
}
