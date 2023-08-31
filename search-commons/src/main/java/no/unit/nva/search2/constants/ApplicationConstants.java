package no.unit.nva.search2.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class ApplicationConstants {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final String DEFAULT_VALUE_PAGE = "0";
    public static final String DEFAULT_VALUE_PER_PAGE = "20";
    public static final String DEFAULT_VALUE_SORT = "publishedDate";
    public static final String DEFAULT_VALUE_SORT_ORDER = "desc";


}
