package no.unit.nva.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;

import no.unit.nva.commons.json.JsonUtils;

import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.List;

/**
 * Default values for the search service.
 *
 * @author Stig Norland
 */
public final class Defaults {

  public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
  public static final ObjectMapper objectMapperNoEmpty = JsonUtils.dynamoObjectMapper;
  public static final Environment ENVIRONMENT = new Environment();

  public static final String DEFAULT_OFFSET = "0";
  public static final String DEFAULT_VALUE_PER_PAGE = "15";

  public static final String DEFAULT_SORT_ORDER = "desc";
  public static final int DEFAULT_AGGREGATION_SIZE = 100;
  public static final URI PAGINATED_SEARCH_RESULT_CONTEXT =
      URI.create("https://bibsysdev.github.io/src/search/paginated-search-result.json");

  public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES =
      List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD, MediaType.CSV_UTF_8);

  public static final int ZERO_RESULTS_AGGREGATION_ONLY = 0;
  public static final String DEFAULT_SHARD_ID = "0";

  @JacocoGenerated
  public Defaults() {}
}
