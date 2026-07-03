package no.unit.nva.search.common.records;

import static no.unit.nva.constants.Defaults.BIBTEX_UTF_8;
import static no.unit.nva.constants.Defaults.RESOURCE_RESPONSE_MEDIA_TYPES;
import static nva.commons.apigateway.MediaType.CSV_UTF_8;
import static nva.commons.apigateway.MediaType.JSON_UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.Hit;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.TotalInfo;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.MediaTypes;
import org.junit.jupiter.api.Test;

class HttpResponseFormatterPaginationHeadersTest {

  private static final URI SOURCE = URI.create("https://api.example.com/search/resources");
  private static final String LINK = "Link";
  private static final String X_TOTAL_COUNT = "X-Total-Count";
  private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  private static final String EXPECTED_EXPOSED_HEADERS = "Link, X-Total-Count";
  private static final List<String> LAST_HIT_SORT = List.of("2017", "abc-123");

  @Test
  void shouldReturnEmptyHeadersWhenMediaTypeIsJson() {
    var headers = formatterFor(JSON_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers, anEmptyMap());
  }

  @Test
  void defaultMediaTypeShouldBeJsonUtf8AndShouldReturnEmptyHeaders() {
    var defaultMediaType = RESOURCE_RESPONSE_MEDIA_TYPES.getFirst();

    assertThat(defaultMediaType, is(JSON_UTF_8));
    assertThat(formatterFor(defaultMediaType, 0, 10, 100).paginationHeaders(), anEmptyMap());
  }

  @Test
  void shouldEmitPaginationHeadersForJsonLd() {
    var headers = formatterFor(MediaTypes.APPLICATION_JSON_LD, 0, 10, 100).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "10"));
    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitProfileLinkForJsonLd() {
    var headers = formatterFor(MediaTypes.APPLICATION_JSON_LD, 0, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), containsString("<https://schema.org>; rel=\"profile\""));
  }

  @Test
  void shouldEmitProfileLinkEvenOnSinglePageJsonLdResponse() {
    var headers = formatterFor(MediaTypes.APPLICATION_JSON_LD, 0, 100, 25).paginationHeaders();

    assertThat(headers.get(LINK), containsString("<https://schema.org>; rel=\"profile\""));
  }

  @Test
  void shouldNotEmitProfileLinkForBibtex() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), not(containsString("rel=\"profile\"")));
  }

  @Test
  void shouldEmitExposeHeadersForCsv() {
    var headers = formatterFor(CSV_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitExposeHeadersForBibtex() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitExposeHeadersEvenWhenSinglePage() {
    var headers = formatterFor(BIBTEX_UTF_8, 100, 25, 25).paginationHeaders();

    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitTotalCountHeaderForCsv() {
    var headers = formatterFor(CSV_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "100"));
  }

  @Test
  void shouldEmitTotalCountHeaderForBibtex() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "100"));
  }

  @Test
  void shouldEmitFirstAndNextButNotPrevOrLastWhenMoreResultsExist() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    var link = headers.get(LINK);
    assertThat(
        link,
        allOf(
            containsString("rel=\"first\""),
            containsString("rel=\"next\""),
            not(containsString("rel=\"prev\"")),
            not(containsString("rel=\"last\""))));
  }

  @Test
  void shouldEmitFirstButNotNextOnLastPage() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 4).paginationHeaders();

    var link = headers.get(LINK);
    assertThat(link, allOf(containsString("rel=\"first\""), not(containsString("rel=\"next\""))));
  }

  @Test
  void shouldPointNextAtSearchAfterCursor() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers.get(LINK), containsString("search_after="));
  }

  @Test
  void shouldNotUseOffsetForLinks() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers.get(LINK), not(containsString("from=")));
  }

  @Test
  void shouldOmitNextWhenLastHitHasNoSortValues() {
    var headers = formatterWithoutSort(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    var link = headers.get(LINK);
    assertThat(link, allOf(containsString("rel=\"first\""), not(containsString("rel=\"next\""))));
  }

  @Test
  void shouldOmitLinkHeaderWhenSinglePage() {
    var headers = formatterFor(BIBTEX_UTF_8, 100, 25, 25).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "25"));
    assertThat(headers, not(hasKey(LINK)));
  }

  @Test
  void shouldOmitLinkHeaderWhenTotalIsZero() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 0, 0).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "0"));
    assertThat(headers, not(hasKey(LINK)));
  }

  @Test
  void shouldOmitLinkHeaderWhenSizeIsZero() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 100, 0).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "100"));
    assertThat(headers, not(hasKey(LINK)));
  }

  @Test
  void shouldUseSourceUriAsBaseForLinks() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers.get(LINK), containsString(SOURCE.toString()));
  }

  @Test
  void shouldIncludeSizeInLinkUrls() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();

    assertThat(headers.get(LINK), containsString("size=10"));
  }

  @Test
  void shouldFormatLinkValueWithRfc8288Syntax() {
    var headers = formatterFor(BIBTEX_UTF_8, 10, 100, 10).paginationHeaders();
    var link = headers.get(LINK);

    assertThat(link, containsString("<"));
    assertThat(link, containsString(">; rel=\""));
  }

  private static HttpResponseFormatter<?> formatterFor(
      MediaType mediaType, int size, int totalHits, int hitsOnPage) {
    return formatter(mediaType, size, totalHits, hitsOnPage, LAST_HIT_SORT);
  }

  private static HttpResponseFormatter<?> formatterWithoutSort(
      MediaType mediaType, int size, int totalHits, int hitsOnPage) {
    return formatter(mediaType, size, totalHits, hitsOnPage, List.of());
  }

  private static HttpResponseFormatter<?> formatter(
      MediaType mediaType, int size, int totalHits, int hitsOnPage, List<String> lastHitSort) {
    var hits = hitsWithCursorOnLastHit(hitsOnPage, lastHitSort);
    var hitsInfo = new HitsInfo(new TotalInfo(totalHits, "eq"), 0.0, hits);
    var swsResponse = new SwsResponse(0, false, null, hitsInfo, null, null);
    return new HttpResponseFormatter<>(swsResponse, mediaType, SOURCE, 0, size, Map.of(), null);
  }

  private static List<Hit> hitsWithCursorOnLastHit(int hitsOnPage, List<String> lastHitSort) {
    if (hitsOnPage == 0) {
      return List.of();
    }
    return Stream.concat(
            Stream.generate(HttpResponseFormatterPaginationHeadersTest::searchHitWithoutSortValues)
                .limit(hitsOnPage - 1L),
            Stream.of(searchHitCarryingSortValues(lastHitSort)))
        .toList();
  }

  private static Hit searchHitWithoutSortValues() {
    return searchHitCarryingSortValues(null);
  }

  private static Hit searchHitCarryingSortValues(List<String> sortValues) {
    return new Hit(null, null, null, 0.0, null, null, sortValues);
  }
}
