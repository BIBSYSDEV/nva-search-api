package no.unit.nva.search2.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search2.enums.ImportCandidateSort;
import no.unit.nva.search2.enums.ResourceSort;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class Defaults {
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final String DEFAULT_OFFSET = "0";
    public static final String DEFAULT_VALUE_PER_PAGE = "15";
    public static final String DEFAULT_RESOURCE_SORT = ResourceSort.PUBLISHED_DATE
        .name().toLowerCase(Locale.getDefault());
    public static final String DEFAULT_IMPORT_CANDIDATE_SORT = ImportCandidateSort.CREATED_DATE
        .name().toLowerCase(Locale.getDefault());
    public static final String DEFAULT_SORT_ORDER = "desc";
    public static final int DEFAULT_AGGREGATION_SIZE = 100;
    public static final URI PAGINATED_SEARCH_RESULT_CONTEXT
        = URI.create("https://bibsysdev.github.io/src/search/paginated-search-result.json");

    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES =
        List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD,
            MediaType.CSV_UTF_8);
}
