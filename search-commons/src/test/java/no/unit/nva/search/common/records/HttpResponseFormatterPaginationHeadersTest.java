package no.unit.nva.search.common.records;

import static no.unit.nva.constants.Defaults.BIBTEX_UTF_8;
import static nva.commons.apigateway.MediaType.CSV_UTF_8;
import static nva.commons.apigateway.MediaType.JSON_UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo;
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

  @Test
  void shouldReturnEmptyHeadersWhenMediaTypeIsJson() {
    var headers = formatterFor(JSON_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers, anEmptyMap());
  }

  @Test
  void shouldReturnEmptyHeadersWhenMediaTypeIsJsonLd() {
    var headers = formatterFor(MediaTypes.APPLICATION_JSON_LD, 0, 10, 100).paginationHeaders();

    assertThat(headers, anEmptyMap());
  }

  @Test
  void shouldEmitExposeHeadersForCsv() {
    var headers = formatterFor(CSV_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitExposeHeadersForBibtex() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitExposeHeadersEvenWhenSinglePage() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 100, 25).paginationHeaders();

    assertThat(headers, hasEntry(ACCESS_CONTROL_EXPOSE_HEADERS, EXPECTED_EXPOSED_HEADERS));
  }

  @Test
  void shouldEmitTotalCountHeaderForCsv() {
    var headers = formatterFor(CSV_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "100"));
  }

  @Test
  void shouldEmitTotalCountHeaderForBibtex() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "100"));
  }

  @Test
  void shouldEmitFirstNextAndLastButNotPrevOnFirstPage() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();

    var link = headers.get(LINK);
    assertThat(
        link,
        allOf(
            containsString("rel=\"first\""),
            containsString("rel=\"next\""),
            containsString("rel=\"last\""),
            not(containsString("rel=\"prev\""))));
  }

  @Test
  void shouldEmitAllRelsOnMiddlePage() {
    var headers = formatterFor(BIBTEX_UTF_8, 20, 10, 100).paginationHeaders();

    var link = headers.get(LINK);
    assertThat(
        link,
        allOf(
            containsString("rel=\"first\""),
            containsString("rel=\"prev\""),
            containsString("rel=\"next\""),
            containsString("rel=\"last\"")));
  }

  @Test
  void shouldEmitFirstPrevAndLastButNotNextOnLastPage() {
    var headers = formatterFor(BIBTEX_UTF_8, 90, 10, 100).paginationHeaders();

    var link = headers.get(LINK);
    assertThat(
        link,
        allOf(
            containsString("rel=\"first\""),
            containsString("rel=\"prev\""),
            containsString("rel=\"last\""),
            not(containsString("rel=\"next\""))));
  }

  @Test
  void shouldPointLastAtFinalPageOffset() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 95).paginationHeaders();

    assertThat(headers.get(LINK), containsString("from=90"));
  }

  @Test
  void shouldPointPrevAtPreviousPageOffset() {
    var headers = formatterFor(BIBTEX_UTF_8, 30, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), containsString("from=20"));
  }

  @Test
  void shouldPointNextAtNextPageOffset() {
    var headers = formatterFor(BIBTEX_UTF_8, 30, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), containsString("from=40"));
  }

  @Test
  void shouldClampPrevAtZeroWhenOffsetSmallerThanSize() {
    var headers = formatterFor(BIBTEX_UTF_8, 5, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), containsString("from=0"));
  }

  @Test
  void shouldOmitLinkHeaderWhenSinglePage() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 100, 25).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "25"));
    assertThat(headers, not(hasKey(LINK)));
  }

  @Test
  void shouldOmitLinkHeaderWhenTotalIsZero() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 0).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "0"));
    assertThat(headers, not(hasKey(LINK)));
  }

  @Test
  void shouldOmitLinkHeaderWhenSizeIsZero() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 0, 100).paginationHeaders();

    assertThat(headers, hasEntry(X_TOTAL_COUNT, "100"));
    assertThat(headers, not(hasKey(LINK)));
  }

  @Test
  void shouldUseSourceUriAsBaseForLinks() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), containsString(SOURCE.toString()));
  }

  @Test
  void shouldIncludeSizeInLinkUrls() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();

    assertThat(headers.get(LINK), containsString("size=10"));
  }

  @Test
  void shouldFormatLinkValueWithRfc8288Syntax() {
    var headers = formatterFor(BIBTEX_UTF_8, 0, 10, 100).paginationHeaders();
    var link = headers.get(LINK);

    assertThat(link, containsString("<"));
    assertThat(link, containsString(">; rel=\""));
  }

  private static HttpResponseFormatter<?> formatterFor(
      MediaType mediaType, int offset, int size, int totalHits) {
    var hitsInfo = new HitsInfo(new TotalInfo(totalHits, "eq"), 0.0, List.of());
    var swsResponse = new SwsResponse(0, false, null, hitsInfo, null, null);
    return new HttpResponseFormatter<>(
        swsResponse, mediaType, SOURCE, offset, size, Map.of(), null);
  }
}
