package no.unit.nva.search.resource;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.constants.Words.CONTENT_TYPE;
import static no.unit.nva.constants.Words.HTTPS;
import static no.unit.nva.search.common.constant.Functions.readApiHost;
import static no.unit.nva.search.resource.Constants.PERSON_PREFERENCES;
import static nva.commons.core.attempt.Try.attempt;

import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.util.List;
import no.unit.nva.search.common.records.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSettingsClient {

  private static final Logger logger = LoggerFactory.getLogger(UserSettingsClient.class);
  private final HttpClient client;

  public UserSettingsClient(HttpClient client) {
    this.client = client;
  }

  public List<String> fetchPromotedPublications(String contributorId) {
    return attempt(() -> createRequest(contributorId))
        .map(request -> client.send(request, BodyHandlers.ofString()))
        .map(response -> handleResponse(response, contributorId))
        .map(UserSettings::promotedPublications)
        .orElse(failure -> List.<String>of());
  }

  private UserSettings handleResponse(HttpResponse<String> response, String contributorId) {
    if (response.statusCode() == HTTP_OK) {
      return attempt(() -> singleLineObjectMapper.readValue(response.body(), UserSettings.class))
          .orElseThrow();
    } else {
      logger.error(
          "Failed to fetch user settings for contributor {}, got {} in response. "
              + "\n Not applying user settings in query",
          contributorId,
          response.statusCode());
      return new UserSettings(List.of());
    }
  }

  private HttpRequest createRequest(String contributorId) {
    var personId = URLEncoder.encode(contributorId, Charset.defaultCharset());
    var uri = URI.create(HTTPS + readApiHost() + PERSON_PREFERENCES + personId);
    logger.info("Fetching promoted publication for: {}", uri);
    return HttpRequest.newBuilder(uri)
        .headers(
            ACCEPT, MediaType.JSON_UTF_8.toString(), CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
        .GET()
        .build();
  }
}
