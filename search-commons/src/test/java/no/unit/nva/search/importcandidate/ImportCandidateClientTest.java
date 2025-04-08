package no.unit.nva.search.importcandidate;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.EQUAL;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.AGGREGATION;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.COLLABORATION_TYPE;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.CONTRIBUTOR;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.CREATED_DATE;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.FILES;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.IMPORT_STATUS;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.PUBLICATION_YEAR;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SIZE;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SORT;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import no.unit.nva.constants.Words;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ImportCandidateClientTest {

  public static final String REQUEST_BASE_URL = "https://example.com/?";
  private static ImportCandidateClient importCandidateClient;

  @BeforeAll
  public static void setUp() {

    var cachedJwtProvider = setupMockedCachedJwtProvider();
    importCandidateClient =
        new ImportCandidateClient(HttpClient.newHttpClient(), cachedJwtProvider);
  }

  static Stream<URI> uriSortingProvider() {
    return Stream.of(
        URI.create(REQUEST_BASE_URL + "sort=title&sortOrder=asc&sort=created_date&order=desc"),
        URI.create(
            REQUEST_BASE_URL
                + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
        URI.create(
            REQUEST_BASE_URL
                + "category=AcademicArticle&sort=title&sortOrder=asc&sort=created_date"),
        URI.create(REQUEST_BASE_URL + "category=AcademicArticle&size=10&from=0&sort=created_date"),
        URI.create(REQUEST_BASE_URL + "orderBy=INSTANCE_TYPE:asc,PUBLICATION_YEAR:desc"),
        URI.create(
            REQUEST_BASE_URL + "orderBy=title:asc,CREATED_DATE:desc&searchAfter=1241234,23412"),
        URI.create(REQUEST_BASE_URL + "orderBy=relevance,title:asc,CREATED_DATE:desc"),
        URI.create(
            REQUEST_BASE_URL
                + "category=AcademicArticle&sort=relevance,TYPE+asc&sort=INSTANCE_TYPE+desc"));
  }

  static Stream<URI> uriProvider() {
    return Stream.of(
        URI.create(REQUEST_BASE_URL + "size=8"),
        URI.create(REQUEST_BASE_URL + "aggregation=ALL&size=8"),
        URI.create(REQUEST_BASE_URL + "aggregation=importStatus&size=8"),
        URI.create(REQUEST_BASE_URL + "category=AcademicArticle&size=5"),
        URI.create(REQUEST_BASE_URL + "CONTRIBUTOR=Andrew+Morrison&size=1"),
        URI.create(REQUEST_BASE_URL + "CONTRIBUTOR=Andrew+Morrison,Nina+Bjørnstad&size=1"),
        URI.create(REQUEST_BASE_URL + "CONTRIBUTOR_NOT=George+Rigos&size=7"),
        URI.create(REQUEST_BASE_URL + "files=hasPublicFiles&size=7"),
        URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=IMPORTED&size=4"),
        URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=1136326@20754.0.0.0&size=2"),
        URI.create(REQUEST_BASE_URL + "IMPORT_STATUS=20754.0.0.0&size=4"),
        URI.create(
            REQUEST_BASE_URL + "id=018bed744c78-f53e06f7-74da-4c91-969f-ec307a7e7816&size=1"),
        URI.create(REQUEST_BASE_URL + "license=CC-BY&size=7"),
        URI.create(REQUEST_BASE_URL + "MODIFIED_DATE=2023&size=8"),
        URI.create(REQUEST_BASE_URL + "MODIFIED_DATE=2023-10&size=2"),
        URI.create(REQUEST_BASE_URL + "MODIFIED_DATE=2023-05,&size=7"),
        URI.create(REQUEST_BASE_URL + "PUBLICATION_YEAR_BEFORE=2023&size=5"),
        URI.create(
            REQUEST_BASE_URL + "PublicationYearBefore=2024&publication_year_since=2023&size=3"),
        URI.create(REQUEST_BASE_URL + "publicationYear=2022,2022&size=1"),
        URI.create(REQUEST_BASE_URL + "publicationYear=2022&size=1"),
        URI.create(REQUEST_BASE_URL + "publicationYear=,2022&size=5"),
        URI.create(REQUEST_BASE_URL + "publicationYear=2022,&size=4"),
        URI.create(REQUEST_BASE_URL + "publicationYear=2023&size=3"),
        URI.create(REQUEST_BASE_URL + "publicationYear=2022,2023&size=4"),
        URI.create(REQUEST_BASE_URL + "title=In+reply:+Why+big+data&size=1"),
        URI.create(REQUEST_BASE_URL + "title=chronic+diseases&size=1"),
        URI.create(REQUEST_BASE_URL + "title=antibacterial,Fishing&size=2"),
        URI.create(REQUEST_BASE_URL + "query=antibacterial&fields=category,title&size=1"),
        URI.create(
            REQUEST_BASE_URL
                + "query=antibacterial&fields=category,title,werstfg&ID_NOT=123&size=1"),
        URI.create(REQUEST_BASE_URL + "query=European&fields=all&size=3"),
        URI.create(REQUEST_BASE_URL + "CRISTIN_IDENTIFIER=3212342&size=1"),
        URI.create(REQUEST_BASE_URL + "SCOPUS_IDENTIFIER=3212342&size=1"));
  }

  static Stream<URI> uriInvalidProvider() {
    return Stream.of(
        URI.create(REQUEST_BASE_URL + "size=7&sort="),
        URI.create(REQUEST_BASE_URL + "query=European&fields"),
        URI.create(REQUEST_BASE_URL + "feilName=epler"),
        URI.create(REQUEST_BASE_URL + "query=epler&fields=feilName"),
        URI.create(REQUEST_BASE_URL + "CREATED_DATE=epler"),
        URI.create(REQUEST_BASE_URL + "sort=CATEGORY:DEdd"),
        URI.create(REQUEST_BASE_URL + "sort=CATEGORdfgY:desc"),
        URI.create(REQUEST_BASE_URL + "sort=CATEGORY"),
        URI.create(REQUEST_BASE_URL + "sort=CATEGORY:asc:DEdd"),
        URI.create(REQUEST_BASE_URL + "size=8&sort=epler"),
        URI.create(REQUEST_BASE_URL + "size=8&sort=type:DEdd"),
        URI.create(REQUEST_BASE_URL + "categories=hello+world"),
        URI.create(REQUEST_BASE_URL + "tittles=hello+world&modified_before=2019-01"),
        URI.create(
            REQUEST_BASE_URL + "conttributors=hello+world&PUBLICATION_YEAR_BEFORE=2020-01-01"),
        URI.create(REQUEST_BASE_URL + "category=PhdThesis&sort=beunited+asc"),
        URI.create(REQUEST_BASE_URL + "funding=NFR,296896"),
        URI.create(REQUEST_BASE_URL + "useers=hello+world"));
  }

  @Test
  void shouldCheckFacets() throws BadRequestException {
    var hostAddress = URI.create(container.getHttpHostAddress());
    var uri1 = URI.create(REQUEST_BASE_URL + AGGREGATION.asCamelCase() + EQUAL + ALL);

    var response1 =
        ImportCandidateSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(uri1))
            .withDockerHostUri(hostAddress)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX);

    assertNotNull(response1);

    var aggregations = response1.toPagedResponse().aggregations();

    assertFalse(aggregations.isEmpty());
    assertThat(aggregations.get(IMPORT_STATUS.asCamelCase()).size(), is(2));
    assertThat(aggregations.get(CONTRIBUTOR.asCamelCase()).size(), is(5));
    assertThat(aggregations.get(COLLABORATION_TYPE.asCamelCase()).size(), is(2));
    assertThat(aggregations.get(FILES.asCamelCase()).getFirst().count(), is(7));
    assertThat(aggregations.get(PUBLICATION_YEAR.asCamelCase()).size(), is(4));
    assertThat(aggregations.get(TYPE.asCamelCase()).size(), is(4));
    assertThat(aggregations.get(TOP_LEVEL_ORGANIZATION.asCamelCase()).size(), is(9));
    assertThat(
        aggregations.get(TOP_LEVEL_ORGANIZATION.asCamelCase()).get(1).labels().get("nb"),
        is(equalTo("Universitetet i Bergen")));
  }

  @Test
  void openSearchFailedResponse() throws IOException, InterruptedException {
    HttpClient httpClient = mock(HttpClient.class);
    var response = mock(HttpResponse.class);
    when(httpClient.send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(500);
    when(response.body()).thenReturn("EXPECTED ERROR");
    var toMapEntries = queryToMapEntries(URI.create(REQUEST_BASE_URL + "size=2"));
    var importCandidateClient =
        new ImportCandidateClient(httpClient, setupMockedCachedJwtProvider());

    assertThrows(
        RuntimeException.class,
        () ->
            ImportCandidateSearchQuery.builder()
                .withRequiredParameters(SIZE, FROM)
                .fromTestQueryParameters(toMapEntries)
                .build()
                .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX));
  }

  @ParameterizedTest
  @MethodSource("uriProvider")
  void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
    var response =
        ImportCandidateSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE, SORT)
            .build()
            .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX);

    var pagedResponse = response.toPagedResponse();

    assertNotNull(response.toPagedResponse());
    assertThat(pagedResponse.hits().size(), is(equalTo(response.parameters().get(SIZE).as())));
    assertThat(pagedResponse.totalHits(), is(equalTo(response.parameters().get(SIZE).as())));
  }

  @ParameterizedTest
  @MethodSource("uriProvider")
  @Disabled(
      "Does not work. When test was written it returned an empty string even if there were"
          + " supposed to be hits. Now we throw an exception instead as the method is not"
          + " implemented.")
  void searchWithUriReturnsCsvResponse(URI uri) throws ApiGatewayException {
    var csvResult =
        ImportCandidateSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE, SORT)
            .withMediaType(Words.TEXT_CSV)
            .build()
            .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX);
    assertNotNull(csvResult);
  }

  @ParameterizedTest
  @MethodSource("uriSortingProvider")
  void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
    var response =
        ImportCandidateSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(uri))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX);

    var pagedResponse = response.toPagedResponse();
    assertNotNull(pagedResponse.id());
    assertNotNull(pagedResponse.context());
    assertTrue(pagedResponse.id().getScheme().contains("https"));
  }

  @ParameterizedTest
  @MethodSource("uriInvalidProvider")
  void failToSearchUri(URI uri) {
    assertThrows(
        BadRequestException.class,
        () ->
            ImportCandidateSearchQuery.builder()
                .fromTestQueryParameters(queryToMapEntries(uri))
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .build()
                .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX));
  }

  @ParameterizedTest
  @MethodSource("uriInvalidProvider")
  void failToSetRequired(URI uri) {
    assertThrows(
        BadRequestException.class,
        () ->
            ImportCandidateSearchQuery.builder()
                .fromTestQueryParameters(queryToMapEntries(uri))
                .withDockerHostUri(URI.create(container.getHttpHostAddress()))
                .withRequiredParameters(FROM, SIZE, CREATED_DATE)
                .build()
                .doSearch(importCandidateClient, Words.IMPORT_CANDIDATES_INDEX));
  }
}
