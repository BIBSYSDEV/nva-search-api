package no.unit.nva.search2.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search2.model.ParameterKey;
import no.unit.nva.search2.model.ResourceParameterKey;
import no.unit.nva.search2.model.SortKeys;

public final class Defaults {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_VALUE_PER_PAGE = "15";
    public static final String DEFAULT_VALUE_SORT = SortKeys.PUBLISHED_YEAR.name();
    public static final String DEFAULT_VALUE_SORT_ORDER = "desc";
    public static final URI PAGINATED_SEARCH_RESULT_CONTEXT
         = URI.create("https://bibsysdev.github.io/src/search/paginated-search-result.json");

}
