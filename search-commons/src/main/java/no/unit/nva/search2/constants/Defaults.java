package no.unit.nva.search2.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;

public final class Defaults {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final String DEFAULT_VALUE_PAGE = "0";
    public static final String DEFAULT_VALUE_PER_PAGE = "20";
    public static final String DEFAULT_VALUE_SORT = "publishedDate";
    public static final String DEFAULT_VALUE_SORT_ORDER = "desc";
    public static final URI DEFAULT_SEARCH_CONTEXT = URI.create("https://api.nva.unit.no/resources/search");
    public static final String HTTPS_SCHEME = "https";


}
