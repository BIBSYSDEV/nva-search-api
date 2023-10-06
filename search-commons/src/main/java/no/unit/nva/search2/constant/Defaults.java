package no.unit.nva.search2.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search2.model.SortKeys;

import java.net.URI;

public final class Defaults {
    public static final ObjectMapper jsonMapperWithNonAbsent = JsonUtils.dtoObjectMapper;
    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_VALUE_PER_PAGE = "15";
    public static final String DEFAULT_VALUE_SORT = SortKeys.PUBLISHED_DATE.name();
    public static final String DEFAULT_VALUE_SORT_ORDER = "desc";
    public static final URI PAGINATED_SEARCH_RESULT_CONTEXT
         = URI.create("https://bibsysdev.github.io/src/search/paginated-search-result.json");

}
