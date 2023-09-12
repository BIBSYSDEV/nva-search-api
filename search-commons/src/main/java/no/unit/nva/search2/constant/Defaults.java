package no.unit.nva.search2.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;

public final class Defaults {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final String DEFAULT_VALUE_PAGE = "0";
    public static final String DEFAULT_VALUE_PER_PAGE = "20";
    public static final String DEFAULT_VALUE_SORT = "publishedDate";
    public static final String DEFAULT_VALUE_SORT_ORDER = "desc";
    public static final String HTTPS_SCHEME = "https";
    public static final URI PAGINATED_SEARCH_RESULT_CONTEXT
         = URI.create("https://bibsysdev.github.io/src/search/paginated-search-result.json");

}
