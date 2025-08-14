package no.sikt.nva.oai.pmh.handler;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.FROM;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.SET;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.UNTIL;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.VERB;
import static no.unit.nva.constants.Words.CRISTIN_AS_TYPE;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.SCOPUS_AS_TYPE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.transform.Source;
import no.sikt.nva.oai.pmh.handler.data.PublisherChannelBuilder;
import no.sikt.nva.oai.pmh.handler.data.ResourceDocumentFactory;
import no.sikt.nva.oai.pmh.handler.data.SerialChannelBuilder;
import no.sikt.nva.oai.pmh.handler.oaipmh.DefaultOaiPmhMethodRouter;
import no.sikt.nva.oai.pmh.handler.oaipmh.MetadataPrefix;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.ResumptionToken;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.ListRecordsRequest;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName;
import no.sikt.nva.oai.pmh.handler.repository.ResourceClientResourceRepository;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.Hit;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.TotalInfo;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.testing.common.ResourceSearchQueryMatcher;
import no.unit.nva.search.testing.common.ResourceSearchQueryMatcher.TermsQueryBuilderExpectation;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openarchives.oai.pmh.v2.DeletedRecordType;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.VerbType;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;
import org.xmlunit.xpath.XPathEngine;

public class OaiPmhHandlerTest {

  private static final String OAI_PMH_NAMESPACE_PREFIX = "oai";
  private static final String OAI_PMH_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";
  private static final String DC_NAMESPACE_PREFIX = "dc";
  private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";

  private static final String[] EXPECTED_SET_SPECS = {
    "resourceTypeGeneral",
    "resourceTypeGeneral:AcademicArticle",
    "resourceTypeGeneral:AcademicChapter"
  };
  private static final String EMPTY_STRING = "";
  private static final String PROTOCOL_VERSION_NODE_NAME = "protocolVersion";
  private static final String EARLIEST_DATESTAMP_NODE_NAME = "earliestDatestamp";
  private static final String DELETED_RECORD_NODE_NAME = "deletedRecord";
  private static final String GRANULARITY_NODE_NAME = "granularity";
  private static final String CONTACT_AT_SIKT_NO = "kontakt@sikt.no";
  private static final String CODE_ATTRIBUTE_NAME = "code";
  private static final String GET_METHOD = "get";
  private static final String POST_METHOD = "post";
  private static final String OAI_DC_NAMESPACE_PREFIX = "oai-dc";
  private static final String OAI_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
  private static final URI LANGUAGE_ENG = URI.create("http://lexvo.org/id/iso639-3/eng");
  private static final String HANDLE_IDENTIFIER = "https://hdl.handle.net/11250/2590299";
  private static final String CRISTIN_IDENTIFIER = "1674987";
  private static final String SCOPUS_IDENTIFIER = "2-s2.0-85062524387";
  private static final String ISBN_IDENTIFIER = "978-0-8194-5906-0";
  private static final URI NVA_DOI = URI.create("https://doi.org/10.1234/nva");
  private static final URI REFERENCE_DOI = URI.create("https://doi.org/10.1234/reference");
  private static final String SERIES_PRINT_ISSN = "2387-2669";
  private static final String SERIES_ONLINE_ISSN = "2387-2660";
  private static final String PUBLISHER_NAME = "My publisher";
  private static final String JOURNAL_NAME = "My journal";
  private static final String SERIES_NAME = "My series";
  private static final String RESOURCE_IDENTIFIER = SortableIdentifier.next().toString();
  private static final URI RESOURCE_ID =
      URI.create("https://localhost/publication/" + RESOURCE_IDENTIFIER);
  private static final String RESOURCE_TITLE = "My title";
  private static final String PUBLICATION_YEAR = "2025";
  private static final String PUBLICATION_MONTH = "01";
  private static final String PUBLICATION_DAY = "01";
  private static final String RESOURCE_ABSTRACT = "My abstract";

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private Environment environment;
  private ResourceClient resourceClient;

  @BeforeEach
  public void setUp() {
    this.environment = mock(Environment.class);
    when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");
    when(environment.readEnv("SEARCH_INFRASTRUCTURE_API_URI"))
        .thenReturn("https://example.com/search");
    when(environment.readEnv("API_HOST")).thenReturn("localhost");
    when(environment.readEnv("OAI_BASE_PATH")).thenReturn("publication-oai-pmh");
    when(environment.readEnv("COGNITO_AUTHORIZER_URLS")).thenReturn("http://localhost:3000");
    this.resourceClient = mock(ResourceClient.class);
  }

  @ParameterizedTest
  @MethodSource("allSupportedRequestsPerMethodProvider")
  void shouldReturnCorrectContentTypeForAllResponses(InputStream request)
      throws JAXBException, IOException {
    mockRepositoryWithEmptyResponsesForAllQueries();
    invokeHandlerAndAssertContentType(request);
  }

  @Test
  void shouldReturnInternalServerErrorWithProperLoggingWhenXmlMarshallingFails()
      throws IOException {
    final var appender = LogUtils.getTestingAppenderForRootLogger();

    var inputStream = emptyRequest();

    var marshaller = new FakeMarshaller();

    var gatewayResponse =
        invokeHandler(environment, new JaxbXmlSerializer(marshaller), inputStream);

    assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
    assertThat(appender.getMessages(), containsString("Ha ha!"));
  }

  @Test
  void shouldReturnErrorResponseWhenNoVerbIsSupplied() throws IOException, JAXBException {
    var inputStream = emptyRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertXmlResponseWithError(
        response, OAIPMHerrorcodeType.BAD_VERB, "Parameter 'verb' is missing.");
  }

  @ParameterizedTest
  @MethodSource("verbsAndMethodCombinations")
  void shouldReturnErrorResponseWhenVerbIsKnownButNotSupported(
      final VerbType verb, final String method) throws IOException, JAXBException {
    var inputStream = request(verb.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertXmlResponseWithError(
        response,
        OAIPMHerrorcodeType.BAD_VERB,
        "Parameter 'verb' has a value that is not supported.");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnErrorResponseWhenVerbIsUnknown(String method) throws IOException, JAXBException {
    var inputStream = request("Unknown", method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertXmlResponseWithError(
        response,
        OAIPMHerrorcodeType.BAD_VERB,
        "Parameter 'verb' has a value that is not supported.");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnExpectedSetsWhenAskingForListSets(String method)
      throws IOException, JAXBException {
    var matcher =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "0")
            .withSearchParameter(ResourceParameter.AGGREGATION, "all")
            .withNamedFilterQuery(
                "status",
                new TermsQueryBuilderExpectation(
                    "status.keyword", "PUBLISHED", "PUBLISHED_METADATA"))
            .build();
    when(resourceClient.doSearch(argThat(matcher), eq(RESOURCES))).thenReturn(swsResponse());

    var inputStream = listSetsRequest(method, null);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.LIST_SETS.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var listSetSpecNodes =
        xpathEngine.selectNodes("/oai:OAI-PMH/oai:ListSets/oai:set/oai:setSpec", response);
    var actualSetSpecs =
        StreamSupport.stream(listSetSpecNodes.spliterator(), false)
            .map(Node::getFirstChild)
            .map(Node::getNodeValue)
            .collect(Collectors.toSet());
    assertThat(actualSetSpecs, containsInAnyOrder(EXPECTED_SET_SPECS));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnOaiPmhErrorWhenUsingResumptionTokenWithListSets(String method)
      throws IOException, JAXBException {
    var inputStream = listSetsRequest(method, "someResumptionToken");

    var response = invokeHandlerAndAssertHttpStatus(inputStream, HTTP_OK);

    assertXmlResponseWithError(
        response,
        OAIPMHerrorcodeType.BAD_ARGUMENT,
        "Resumption token not supported for method 'ListSets'");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnExpectedErrorAndLogWhenSearchFailsForListSets(String method)
      throws IOException, JAXBException {
    final var appender = LogUtils.getTestingAppenderForRootLogger();

    doThrow(new RuntimeException(EMPTY_STRING)).when(resourceClient).doSearch(any(), eq(RESOURCES));

    var inputStream = listSetsRequest(method, null);

    invokeHandlerAndAssertHttpStatus(inputStream, HTTP_INTERNAL_ERROR);

    assertThat(
        appender.getMessages(),
        containsString("Failed to execute search for resources aggregations."));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithRepositoryName(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var repositoryName = getIdentifyChildNodeText(xpathEngine, response, "repositoryName");
    assertThat(repositoryName, is(equalTo("NVA-OAI-PMH")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithBaseURL(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var baseURL = getIdentifyChildNodeText(xpathEngine, response, "baseURL");

    assertThat(baseURL, is(equalTo("https://localhost/publication-oai-pmh")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithProtocolVersion(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var protocolVersion =
        getIdentifyChildNodeText(xpathEngine, response, PROTOCOL_VERSION_NODE_NAME);

    assertThat(protocolVersion, is(equalTo("2.0")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithEarliestDatestamp(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var earliestDatestamp =
        getIdentifyChildNodeText(xpathEngine, response, EARLIEST_DATESTAMP_NODE_NAME);

    assertThat(earliestDatestamp, is(equalTo("2023-01-01T01:02:03Z")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithEarliestDatestampCloseToNowIfRepositoryIsEmpty(String method)
      throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(true);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var earliestDatestamp =
        getIdentifyChildNodeText(xpathEngine, response, EARLIEST_DATESTAMP_NODE_NAME);

    var tolerance = Duration.ofSeconds(5);
    assertThat(
        Duration.between(Instant.now(), Instant.parse(earliestDatestamp)).abs(),
        lessThan(tolerance));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithDeletedRecord(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var deletedRecord = getIdentifyChildNodeText(xpathEngine, response, DELETED_RECORD_NODE_NAME);

    assertThat(deletedRecord, is(equalTo(DeletedRecordType.NO.value())));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithGranularity(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var granularity = getIdentifyChildNodeText(xpathEngine, response, GRANULARITY_NODE_NAME);

    assertThat(granularity, is(equalTo("YYYY-MM-DDThh:mm:ssZ")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithAdminEmail(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    mockEarliestDatestampQuery(false);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.IDENTIFY.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var adminEmailNodes =
        xpathEngine.selectNodes("/oai:OAI-PMH/oai:Identify/oai:adminEmail", response);
    assertThat(adminEmailNodes, iterableWithSize(1));

    var adminEmail = adminEmailNodes.iterator().next().getFirstChild().getNodeValue();
    assertThat(adminEmail, is(equalTo(CONTACT_AT_SIKT_NO)));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListMetadataFormatsOnlyOaiDc(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.LIST_METADATA_FORMATS.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.LIST_METADATA_FORMATS.value());
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var metadataFormatNodes =
        xpathEngine.selectNodes(
            "/oai:OAI-PMH/oai:ListMetadataFormats/oai:metadataFormat", response);
    assertThat(metadataFormatNodes, iterableWithSize(1));

    var metadataFormatNode = metadataFormatNodes.iterator().next();
    var metadataPrefix = getTextValueOfNamedChild(metadataFormatNode, "metadataPrefix");
    var schema = getTextValueOfNamedChild(metadataFormatNode, "schema");
    var metadataNamespace = getTextValueOfNamedChild(metadataFormatNode, "metadataNamespace");

    assertThat(metadataPrefix.orElseThrow(), is(equalTo(MetadataPrefix.OAI_DC.getPrefix())));
    assertThat(schema.orElseThrow(), is(equalTo("http://www.openarchives.org/OAI/2.0/oai_dc.xsd")));
    assertThat(
        metadataNamespace.orElseThrow(),
        is(equalTo("http://www.openarchives.org/OAI/2.0/oai_dc/")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReportErrorOnListRecordsWithoutMetadataPrefix(String method) throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";

    var response =
        performListRecordsOperation(
            method, fromDate, untilDate, null, "DUMMY" + ":AcademicArticle", null);
    assertXmlResponseWithError(
        response, OAIPMHerrorcodeType.BAD_ARGUMENT, "metadataPrefix is required");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReportErrorOnListRecordsWithNotSupportedMetadataPrefix(String method)
      throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";

    var response =
        performListRecordsOperation(
            method,
            fromDate,
            untilDate,
            "NOT_SUPPORTED",
            "resourceTypeGeneral:AcademicArticle",
            null);
    assertXmlResponseWithError(
        response,
        OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT,
        "metadataPrefix is not supported.");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReportErrorOnListRecordsWithNotSupportedSet(String method) throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";

    var response =
        performListRecordsOperation(
            method, fromDate, untilDate, MetadataPrefix.OAI_DC.getPrefix(), "NA:NA", null);
    assertXmlResponseWithError(
        response, OAIPMHerrorcodeType.BAD_ARGUMENT, "Illegal set spec. Unknown root.");
  }

  @ParameterizedTest
  @MethodSource("datestampIssueListRecordsParameterProvider")
  void shouldReportErrorOnInitialListRecordsWithNanoSecondDataStamp(
      String method, String from, String until) throws Exception {

    var response =
        performListRecordsOperation(
            method, from, until, MetadataPrefix.OAI_DC.getPrefix(), null, null);
    assertXmlResponseWithError(
        response,
        OAIPMHerrorcodeType.BAD_ARGUMENT,
        "datestamp fields with time does not support nanoseconds.");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsOnInitialQueryWithOnlyRequiredParameters(String method) throws Exception {
    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea64",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f1",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0c");

    runListRecordsTest(method, null, null, null, null, null, null, 3, expectedIdentifiers, true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsOnInitialQuery(String method) throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";
    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea64",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f1",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0c");

    runListRecordsTest(
        method, null, fromDate, untilDate, null, null, null, 3, expectedIdentifiers, true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsOnInitialQueryHarvestingSpecificSet(String method) throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";
    var set = "resourceTypeGeneral:AcademicArticle";
    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea64",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f1",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0c");

    runListRecordsTest(
        method,
        null,
        fromDate,
        untilDate,
        "resourceTypeGeneral",
        "AcademicArticle",
        null,
        3,
        expectedIdentifiers,
        true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsWithResumptionToken(String method) throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";
    var metadataPrefix = MetadataPrefix.OAI_DC;
    var cursor = "2016-01-04T05:48:31.123456789Z";
    var listRecordsRequest =
        new ListRecordsRequest(
            OaiPmhDateTime.from(fromDate),
            OaiPmhDateTime.from(untilDate),
            SetSpec.EMPTY_INSTANCE,
            metadataPrefix);
    var resumptionToken = new ResumptionToken(listRecordsRequest, cursor, 8).getValue();

    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea65",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f2",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0d");

    runListRecordsTest(
        method,
        cursor,
        fromDate,
        untilDate,
        null,
        null,
        resumptionToken,
        3,
        expectedIdentifiers,
        true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldNotReturnResumptionTokenOnLastPage(String method) throws Exception {
    var fromDate = "2016-01-01";
    var untilDate = "2017-01-01";
    var cursor = "2016-01-07T05:48:31.123456789Z";
    var metadataPrefix = MetadataPrefix.OAI_DC;
    var listRecordsRequest =
        new ListRecordsRequest(
            OaiPmhDateTime.from(fromDate),
            OaiPmhDateTime.from(untilDate),
            SetSpec.EMPTY_INSTANCE,
            metadataPrefix);

    var resumptionToken = new ResumptionToken(listRecordsRequest, cursor, 8).getValue();

    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea66",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f3");

    runListRecordsTest(
        method,
        cursor,
        fromDate,
        untilDate,
        null,
        null,
        resumptionToken,
        2,
        expectedIdentifiers,
        false);
  }

  @Test
  void shouldUseIdAsRecordHeaderIdentifier() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var identifier =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:header/oai:identifier",
            response);

    assertThat(identifier, is(equalTo(RESOURCE_ID.toString())));
  }

  @Test
  void shouldUseCorrectDateFormatOnHeaderDatestamp() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var datestamp =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:header/oai:datestamp",
            response);

    assertThat(datestamp, is(equalTo("2023-01-01T01:02:03Z")));
  }

  @Test
  void shouldPopulateRecordHeaderSetSpec() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var setSpec =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:header/oai:setSpec",
            response);

    assertThat(setSpec, is(equalTo("resourceTypeGeneral:AcademicArticle")));
  }

  @Test
  void shouldUseTitleAsRecordMetadataDcTitle() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var title =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:title",
            response);
    assertThat(title, is(equalTo("My title")));
  }

  @Test
  void shouldMapAbstractToRecordMetadataDcDescription() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertHasDescription(response, RESOURCE_ABSTRACT);
  }

  @Test
  void shouldPopulateReferenceDoiAsDcIdentifier() throws IOException, JAXBException {
    var inputStream = bookAnthologyRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertHasIdentifier(response, REFERENCE_DOI.toString());
  }

  @Test
  void shouldNotPopulateNvaDoiAsDcIdentifierIfReferenceDoiIsPresent()
      throws IOException, JAXBException {
    var inputStream = bookAnthologyRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertDoesNotHaveIdentifier(response, NVA_DOI.toString());
  }

  @Test
  void shouldFallBackToNvaDoiAsDcIdentifierIfReferenceDoiNotPresent()
      throws IOException, JAXBException {
    var inputStream = bookAnthologyRequestWithNvaDoiOnly();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertHasIdentifier(response, NVA_DOI.toString());
  }

  @ParameterizedTest
  @ValueSource(strings = {SERIES_PRINT_ISSN, SERIES_ONLINE_ISSN})
  void shouldPopulateIssnAsDcIdentifier(String issn) throws IOException, JAXBException {
    var inputStream = bookAnthologyRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertHasIdentifier(response, "ISSN:" + issn);
  }

  @Test
  void shouldPopulateIsbnAsDcIdentifier() throws IOException, JAXBException {
    var inputStream = bookAnthologyRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertHasIdentifier(response, "ISBN:" + ISBN_IDENTIFIER);
  }

  @Test
  void shouldPopulateHandleAsDcIdentifier() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertHasIdentifier(response, HANDLE_IDENTIFIER);
  }

  @Test
  void shouldPopulateCristinIdentifierAsDcIdentifier() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertHasIdentifier(response, "CRISTIN:" + CRISTIN_IDENTIFIER);
  }

  @Test
  void shouldPopulateScopusIdentifierAsDcIdentifier() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertHasIdentifier(response, "SCOPUS:" + SCOPUS_IDENTIFIER);
  }

  @Test
  void shouldPopulateMetadataDcLanguage() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var language =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:language",
            response);
    assertThat(language, is(equalTo("eng")));
  }

  @Test
  void shouldUsePublicationDateAsRecordMetadataDcDate() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var date =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:date",
            response);
    assertThat(
        date,
        is(equalTo("%s-%s-%s".formatted(PUBLICATION_YEAR, PUBLICATION_MONTH, PUBLICATION_DAY))));
  }

  @Test
  void shouldUsePublicationInstanceTypeAsRecordMetadataDcType() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var type =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:type",
            response);
    assertThat(type, is(equalTo("AcademicArticle")));
  }

  @Test
  void shouldUseIdAsRecordMetadataDcIdentifier() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var id =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:identifier",
            response);
    assertThat(id, is(equalTo(RESOURCE_ID.toString())));
  }

  @Test
  void
      shouldUseNameOfPublicationContextIfTypeIsJournalAsRecordMetadataDcPublisherIfPublisherAndSeriesAreNotPresent()
          throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var publisher =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:publisher",
            response);
    assertThat(publisher, is(equalTo("My journal")));
  }

  @Test
  void shouldUseNameOfPublicationContextPublisherIfTypeNotJournalAsRecordMetadataDcPublisher()
      throws IOException, JAXBException {
    var inputStream = requestWithReportBasicHit();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var publisher =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:publisher",
            response);
    assertThat(publisher, is(equalTo(PUBLISHER_NAME)));
  }

  @Test
  void shouldOmitPublisherNameAsPublisherIfDataIsIncomplete() throws IOException, JAXBException {
    var inputStream = requestWithReportBasicHitWithIncompletePublisherInformation();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertNotPresent(
        xpathEngine,
        "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:publisher",
        response);
  }

  @Test
  void shouldOmitJournalNameAsPublisherIfDataIsIncomplete() throws IOException, JAXBException {
    var inputStream = academicArticleWithIncompleteJournalInformation();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertNotPresent(
        xpathEngine,
        "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:publisher",
        response);
  }

  @Test
  void shouldOmitDcDateIfPublicationIsMissingPublicationDate() throws IOException, JAXBException {
    var inputStream = requestWithReportBasicHitMissingPublicationDate();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertNotPresent(
        xpathEngine,
        "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:date",
        response);
  }

  @Test
  void shouldOmitDcDateIfPublicationHasEmptyPublicationDate() throws IOException, JAXBException {
    var inputStream = requestWithReportBasicHitEmptyPublicationDate();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertNotPresent(
        xpathEngine,
        "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:date",
        response);
  }

  @Test
  void shouldUseSetIfSuppliedInListRecords() throws IOException, JAXBException {
    var inputStream =
        defaultHitAndRequest(new SetSpec(SetRoot.RESOURCE_TYPE_GENERAL, "AcademicArticle"));

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertThat(
        xpathEngine.selectNodes(
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:date", response),
        is(IsIterableWithSize.iterableWithSize(1)));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnErrorInGetRecordWhenIdentifierIsMissing(String method)
      throws IOException, JAXBException {
    mockRepositoryQueryForOneRecord();
    try (var request = createGetRecordRequest(method, null, MetadataPrefix.OAI_DC.getPrefix())) {
      var gatewayResponse = invokeHandler(request);

      assertThat(gatewayResponse.getStatusCode(), is(equalTo(200)));
      var source = Input.fromString(gatewayResponse.getBody()).build();
      assertXmlResponseWithError(
          source, OAIPMHerrorcodeType.BAD_ARGUMENT, "identifier is required");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnErrorInGetRecordWhenMetadataPrefixIsMissing(String method)
      throws IOException, JAXBException {
    mockRepositoryQueryForOneRecord();
    try (var request = createGetRecordRequest(method, RESOURCE_ID.toString(), null)) {
      var gatewayResponse = invokeHandler(request);

      assertThat(gatewayResponse.getStatusCode(), is(equalTo(200)));
      var source = Input.fromString(gatewayResponse.getBody()).build();
      assertXmlResponseWithError(
          source, OAIPMHerrorcodeType.BAD_ARGUMENT, "metadataPrefix is required");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnRecordInGetRecordWhenExists(String method) throws IOException, JAXBException {
    mockRepositoryQueryForOneRecord();
    try (var request =
        createGetRecordRequest(method, RESOURCE_ID.toString(), MetadataPrefix.OAI_DC.getPrefix())) {
      var gatewayResponse = invokeHandler(request);

      assertThat(gatewayResponse.getStatusCode(), is(equalTo(200)));
      var source = Input.fromString(gatewayResponse.getBody()).build();
      var xpathEngine = getXpathEngine();
      var firstNode =
          xpathEngine
              .selectNodes(
                  "/oai:OAI-PMH/oai:GetRecord/oai:record/oai:header/oai:identifier", source)
              .iterator()
              .next();

      assertThat(firstNode.getFirstChild().getNodeValue(), is(equalTo(RESOURCE_ID.toString())));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnErrorInGetRecordWhenInvalidLocalIdentifierUri(String method)
      throws IOException, JAXBException {
    mockRepositoryQueryForOneRecord();
    var illegalResourceId = URI.create("https://some.illegal.domain/" + RESOURCE_IDENTIFIER);
    try (var request =
        createGetRecordRequest(
            method, illegalResourceId.toString(), MetadataPrefix.OAI_DC.getPrefix())) {
      var gatewayResponse = invokeHandler(request);

      assertThat(gatewayResponse.getStatusCode(), is(equalTo(200)));

      var source = Input.fromString(gatewayResponse.getBody()).build();
      assertXmlResponseWithError(
          source, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "identifier does not exist");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnErrorInGetRecordWhenNotExist(String method) throws IOException, JAXBException {
    mockRepositoryQueryForNoRecord();
    try (var request =
        createGetRecordRequest(method, RESOURCE_ID.toString(), MetadataPrefix.OAI_DC.getPrefix())) {
      var gatewayResponse = invokeHandler(request);

      assertThat(gatewayResponse.getStatusCode(), is(equalTo(200)));

      var source = Input.fromString(gatewayResponse.getBody()).build();
      assertXmlResponseWithError(
          source, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "identifier does not exist");
    }
  }

  private void assertHasDescription(Source source, String expectedDescription) {
    var descriptions = extractValuesFromDcElements("description", source);
    assertThat(descriptions, hasItem(expectedDescription));
  }

  private void assertHasIdentifier(Source source, String expectedIdentifier) {
    var identifiers = extractValuesFromDcElements("identifier", source);
    assertThat(identifiers, hasItem(expectedIdentifier));
  }

  void assertDoesNotHaveIdentifier(Source source, String identifier) {
    var identifiers = extractValuesFromDcElements("identifier", source);
    assertThat(identifiers, not(hasItem(identifier)));
  }

  private static List<String> extractValuesFromDcElements(String element, Source source) {
    var xpathEngine = getXpathEngine();
    var nodes =
        xpathEngine.selectNodes(
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:" + element, source);
    var nodeIterator = nodes.iterator();

    var values = new ArrayList<String>();
    while (nodeIterator.hasNext()) {
      values.add(nodeIterator.next().getFirstChild().getNodeValue());
    }
    return values;
  }

  private void mockRepositoryQueryForOneRecord() {
    var queryMatcher =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "1")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE)
            .withSearchParameter(ResourceParameter.ID, RESOURCE_IDENTIFIER)
            .withNamedFilterQuery(
                "status",
                new TermsQueryBuilderExpectation(
                    "status.keyword", "PUBLISHED", "PUBLISHED_METADATA"))
            .build();
    when(resourceClient.doSearch(argThat(queryMatcher), eq(RESOURCES)))
        .thenReturn(swsResponseWithIdentifiedResource());
  }

  private void mockRepositoryQueryForNoRecord() {
    var queryMatcher =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "1")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE)
            .withSearchParameter(ResourceParameter.ID, RESOURCE_IDENTIFIER)
            .withNamedFilterQuery(
                "status",
                new TermsQueryBuilderExpectation(
                    "status.keyword", "PUBLISHED", "PUBLISHED_METADATA"))
            .build();
    when(resourceClient.doSearch(argThat(queryMatcher), eq(RESOURCES)))
        .thenReturn(emptySwsResponse());
  }

  private SwsResponse swsResponseWithIdentifiedResource() {
    var identifiedResourceNode = defaultAcademicArticle();

    var hitList = new ArrayList<Hit>();
    hitList.add(new Hit("", "", "", 1.0, identifiedResourceNode, null, List.of()));

    return new SwsResponse(
        0, false, null, new HitsInfo(new TotalInfo(hitList.size(), ""), 1.0, hitList), null, null);
  }

  private InputStream createGetRecordRequest(
      String method, String identifier, String metadataPrefix) throws JsonProcessingException {
    var requestBuilder = new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper);

    if (GET_METHOD.equals(method)) {
      GetRecordsRequestHelper.applyQueryParams(requestBuilder, identifier, metadataPrefix);
    } else {
      GetRecordsRequestHelper.applyBody(requestBuilder, identifier, metadataPrefix);
    }
    return requestBuilder.build();
  }

  private void assertNotPresent(JAXPXPathEngine xpathEngine, String expression, Source source) {
    assertThat(xpathEngine.selectNodes(expression, source), is(emptyIterable()));
  }

  private void runListRecordsTest(
      final String method,
      final String cursor,
      final String from,
      final String until,
      final String setRoot,
      final String setChild,
      final String resumptionToken,
      final int expectedRecordCount,
      final List<String> expectedIdentifiers,
      final boolean expectResumptionToken)
      throws Exception {

    var matcher = buildMatcher(cursor, from, until, setChild);
    var swsResponse = resolveMockResponse(cursor, expectedRecordCount);
    when(resourceClient.doSearch(argThat(matcher), any())).thenReturn(swsResponse);

    var metadataPrefix = MetadataPrefix.OAI_DC.getPrefix();
    var setSpec =
        nonNull(setRoot) && nonNull(setChild) ? String.join(":", setRoot, setChild) : null;

    var response =
        performListRecordsOperation(method, from, until, metadataPrefix, setSpec, resumptionToken);
    var xpathEngine = getXpathEngine();

    var expectedRequestParameters =
        new EnumMap<OaiPmhParameterName, String>(OaiPmhParameterName.class);
    expectedRequestParameters.put(VERB, VerbType.LIST_RECORDS.value());
    if (nonNull(from)) {
      expectedRequestParameters.put(FROM, from);
    }
    if (nonNull(until)) {
      expectedRequestParameters.put(UNTIL, until);
    }
    if (nonNull(setRoot) && nonNull(setChild)) {
      expectedRequestParameters.put(SET, setRoot + ":" + setChild);
    }
    assertResponseRequestContains(expectedRequestParameters, response, xpathEngine);

    var recordNodes = xpathEngine.selectNodes("/oai:OAI-PMH/oai:ListRecords/oai:record", response);
    assertThat(recordNodes, iterableWithSize(expectedRecordCount));

    var identifiers = extractRecordIdentifiers(recordNodes);
    assertThat(identifiers, containsInAnyOrder(expectedIdentifiers.toArray()));

    if (expectResumptionToken) {
      assertResumptionTokenHasCompleteRecordSize(xpathEngine, response);
      var resumptionTokenValue =
          xpathEngine
              .selectNodes("/oai:OAI-PMH/oai:ListRecords/oai:resumptionToken", response)
              .iterator()
              .next()
              .getFirstChild()
              .getNodeValue();
      var token = ResumptionToken.from(resumptionTokenValue).orElseThrow();
      assertThat(
          token.originalRequest().getMetadataPrefix().getPrefix(), is(equalTo(metadataPrefix)));
      var expectedCursorInToken =
          nonNull(cursor) ? "2016-01-06T08:55:42.820948673Z" : "2016-01-03T08:55:42.820948673Z";
      assertThat(
          "Expects cursor in token to be 1 ms ahead of the last record in page",
          token.cursor(),
          is(equalTo(expectedCursorInToken)));
    } else {
      assertNoResumptionToken(xpathEngine, response);
    }
  }

  private SwsResponse resolveMockResponse(String cursor, int expectedRecordCount) throws Exception {
    return switch (expectedRecordCount) {
      case 3 -> nonNull(cursor) ? secondPageSwsResponse() : firstPageSwsResponse();
      // on context
      case 2 -> lastPageSwsResponse();
      default ->
          throw new IllegalArgumentException("Unexpected record count: " + expectedRecordCount);
    };
  }

  private ResourceSearchQueryMatcher buildMatcher(
      String cursor, String from, String until, String setChild) {
    var builder =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "3")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE);
    if (nonNull(cursor)) {
      builder.withSearchParameter(ResourceParameter.MODIFIED_SINCE, cursor);
    } else if (nonNull(from)) {
      builder.withSearchParameter(ResourceParameter.MODIFIED_SINCE, from + "T00:00:00Z");
    }
    if (nonNull(until)) {
      builder.withSearchParameter(ResourceParameter.MODIFIED_BEFORE, until + "T00:00:00Z");
    }
    if (nonNull(setChild)) {
      builder.withSearchParameter(ResourceParameter.INSTANCE_TYPE, setChild);
    }

    return builder.build();
  }

  private void assertResumptionTokenHasCompleteRecordSize(
      XPathEngine xPathEngine, Source response) {
    var resumptionTokenNodes =
        xPathEngine.selectNodes("/oai:OAI-PMH/oai:ListRecords/oai:resumptionToken", response);

    assertThat(resumptionTokenNodes, iterableWithSize(1));
    var completeListSize =
        resumptionTokenNodes
            .iterator()
            .next()
            .getAttributes()
            .getNamedItem("completeListSize")
            .getNodeValue();
    assertThat(completeListSize, is(equalTo("8")));
  }

  private void assertNoResumptionToken(XPathEngine xPathEngine, Source response) {
    var resumptionTokenNodes =
        xPathEngine.selectNodes("/oai:OAI-PMH/oai:ListRecords/oai:resumptionToken", response);

    assertThat(resumptionTokenNodes, is(emptyIterable()));
  }

  private Source performListRecordsOperation(
      String method,
      String from,
      String until,
      String metadataPrefix,
      String setSpec,
      String resumptionToken)
      throws JAXBException, IOException {
    var inputStream =
        request(
            VerbType.LIST_RECORDS.value(),
            method,
            from,
            until,
            metadataPrefix,
            setSpec,
            resumptionToken);

    return invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
  }

  private static String extractTextNodeValueFromResponse(
      JAXPXPathEngine xpathEngine, String xpathExpression, Source response) {
    return xpathEngine
        .selectNodes(xpathExpression, response)
        .iterator()
        .next()
        .getFirstChild()
        .getNodeValue();
  }

  private InputStream defaultHitAndRequest() throws JsonProcessingException {
    return defaultHitAndRequest(SetSpec.EMPTY_INSTANCE);
  }

  private InputStream bookAnthologyRequest() throws JsonProcessingException {
    return hitAndRequest(wrapHits(defaultBookAnthology()));
  }

  private InputStream bookAnthologyRequestWithNvaDoiOnly() throws JsonProcessingException {
    return hitAndRequest(wrapHits(bookAnthologyWithNvaDoiOnly()));
  }

  private InputStream defaultHitAndRequest(SetSpec setSpec) throws JsonProcessingException {
    return hitAndRequest(wrapHits(defaultAcademicArticle()), setSpec);
  }

  private ArrayNode wrapHits(ObjectNode... hits) {
    return new ArrayNode(JsonNodeFactory.instance, List.of(hits));
  }

  private ObjectNode defaultAcademicArticle() {
    var journalBuilder = new SerialChannelBuilder("Journal", JOURNAL_NAME);
    return ResourceDocumentFactory.builder(
            RESOURCE_ID, RESOURCE_TITLE, PUBLICATION_YEAR, PUBLICATION_MONTH, PUBLICATION_DAY)
        .withAbstract(RESOURCE_ABSTRACT)
        .withAdditionalIdentifier(CRISTIN_AS_TYPE, CRISTIN_IDENTIFIER)
        .withAdditionalIdentifier(SCOPUS_AS_TYPE, SCOPUS_IDENTIFIER)
        .withAdditionalIdentifier("HandleIdentifier", HANDLE_IDENTIFIER)
        .withDoi(NVA_DOI)
        .withLanguage(LANGUAGE_ENG)
        .academicArticle(journalBuilder)
        .withReferenceDoi(REFERENCE_DOI)
        .apply()
        .build();
  }

  private ObjectNode defaultReportBasic() {
    var publisherBuilder = new PublisherChannelBuilder(PUBLISHER_NAME);
    var seriesBuilder = new SerialChannelBuilder("Series", SERIES_NAME);
    return ResourceDocumentFactory.builder(
            RESOURCE_ID, RESOURCE_TITLE, PUBLICATION_YEAR, PUBLICATION_MONTH, PUBLICATION_DAY)
        .withAdditionalIdentifier(CRISTIN_AS_TYPE, CRISTIN_IDENTIFIER)
        .withAdditionalIdentifier(SCOPUS_AS_TYPE, SCOPUS_IDENTIFIER)
        .withAdditionalIdentifier("HandleIdentifier", HANDLE_IDENTIFIER)
        .withDoi(NVA_DOI)
        .withLanguage(LANGUAGE_ENG)
        .reportBasic(publisherBuilder, seriesBuilder)
        .withReferenceDoi(REFERENCE_DOI)
        .apply()
        .build();
  }

  private ObjectNode defaultBookAnthology() {
    var publisherBuilder = new PublisherChannelBuilder(PUBLISHER_NAME);
    var seriesBuilder =
        new SerialChannelBuilder("Series", SERIES_NAME)
            .withOnlineIssn(SERIES_ONLINE_ISSN)
            .withPrintIssn(SERIES_PRINT_ISSN);

    return ResourceDocumentFactory.builder(
            RESOURCE_ID, RESOURCE_TITLE, PUBLICATION_YEAR, PUBLICATION_MONTH, PUBLICATION_DAY)
        .withDoi(NVA_DOI)
        .bookAnthology(publisherBuilder)
        .withSeries(seriesBuilder)
        .withIsbn(ISBN_IDENTIFIER)
        .withReferenceDoi(REFERENCE_DOI)
        .apply()
        .build();
  }

  private ObjectNode bookAnthologyWithNvaDoiOnly() {
    var bookAnthology = defaultBookAnthology();
    var referenceNode = (ObjectNode) bookAnthology.at("/entityDescription/reference");
    referenceNode.remove("doi");

    return bookAnthology;
  }

  private InputStream academicArticleWithIncompleteJournalInformation()
      throws JsonProcessingException {
    var academicArticle = defaultAcademicArticle();
    var journalNode =
        (ObjectNode) academicArticle.at("/entityDescription/reference/publicationContext");
    journalNode.remove("name");
    return hitAndRequest(wrapHits(academicArticle));
  }

  private InputStream hitAndRequest(ArrayNode hits) throws JsonProcessingException {
    return hitAndRequest(hits, SetSpec.EMPTY_INSTANCE);
  }

  private InputStream hitAndRequest(ArrayNode hits, SetSpec setSpec)
      throws JsonProcessingException {
    var queryBuilder =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "3")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE)
            .withSearchParameter(ResourceParameter.MODIFIED_BEFORE, "2016-01-02T00:00:00Z")
            .withSearchParameter(ResourceParameter.MODIFIED_SINCE, "2016-01-01T00:00:00Z");
    setSpec.ifPresent(
        ignored ->
            queryBuilder.withSearchParameter(
                ResourceParameter.INSTANCE_TYPE, setSpec.children()[0]));
    var resourceQueryMatcher =
        queryBuilder
            .withNamedFilterQuery(
                "status",
                new TermsQueryBuilderExpectation(
                    "status.keyword", "PUBLISHED", "PUBLISHED_METADATA"))
            .build();

    when(resourceClient.doSearch(argThat(resourceQueryMatcher), any()))
        .thenReturn(initialSwsResponse(hits));

    var from = "2016-01-01";
    var until = "2016-01-02";
    var metadataPrefix = MetadataPrefix.OAI_DC.getPrefix();
    return request(
        VerbType.LIST_RECORDS.value(),
        "GET",
        from,
        until,
        metadataPrefix,
        setSpec.orElse(null),
        null);
  }

  private void mockEarliestDatestampQuery(boolean emptyRepository) {
    var hits =
        emptyRepository
            ? new ArrayNode(JsonNodeFactory.instance)
            : new ArrayNode(JsonNodeFactory.instance, List.of(defaultAcademicArticle()));
    var queryMatcher =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "1")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE)
            .withNamedFilterQuery(
                "status",
                new TermsQueryBuilderExpectation(
                    "status.keyword", "PUBLISHED", "PUBLISHED_METADATA"))
            .build();

    when(resourceClient.doSearch(argThat(queryMatcher), any()))
        .thenReturn(initialSwsResponse(hits));
  }

  private InputStream requestWithReportBasicHit() throws JsonProcessingException {
    var hits = new ArrayNode(JsonNodeFactory.instance, List.of(defaultReportBasic()));
    return hitAndRequest(hits);
  }

  private JsonNode reportBasicWithMissingPublisherName() {
    var reportBasic = defaultReportBasic();
    var publisherNode =
        (ObjectNode) reportBasic.at("/entityDescription/reference/publicationContext/publisher");
    publisherNode.remove("name");
    return reportBasic;
  }

  private JsonNode reportBasicWithMissingPublicationDate() {
    var reportBasic = defaultReportBasic();
    var entityDescriptionNode = (ObjectNode) reportBasic.at("/entityDescription");
    entityDescriptionNode.remove("publicationDate");
    return reportBasic;
  }

  private JsonNode reportBasicWithEmptyPublicationDate() {
    var reportBasic = defaultReportBasic();
    var entityDescriptionNode = (ObjectNode) reportBasic.at("/entityDescription");
    entityDescriptionNode.remove("publicationDate");
    entityDescriptionNode.set("publicationDate", JsonNodeFactory.instance.objectNode());
    return reportBasic;
  }

  private InputStream requestWithReportBasicHitWithIncompletePublisherInformation()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(JsonNodeFactory.instance, List.of(reportBasicWithMissingPublisherName()));
    return hitAndRequest(hits);
  }

  private InputStream requestWithReportBasicHitMissingPublicationDate()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(JsonNodeFactory.instance, List.of(reportBasicWithMissingPublicationDate()));
    return hitAndRequest(hits);
  }

  private InputStream requestWithReportBasicHitEmptyPublicationDate()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(JsonNodeFactory.instance, List.of(reportBasicWithEmptyPublicationDate()));
    return hitAndRequest(hits);
  }

  private InputStream request(
      String verb,
      String method,
      String from,
      String until,
      String metadataPrefix,
      String set,
      String resumptionToken)
      throws JsonProcessingException {
    var handlerRequestBuilder =
        new HandlerRequestBuilder<String>(new ObjectMapper()).withHttpMethod(method);

    if ("get".equalsIgnoreCase(method)) {
      addAsQueryParams(
          verb, from, until, metadataPrefix, set, resumptionToken, handlerRequestBuilder);
    } else if ("post".equalsIgnoreCase(method)) {
      addAsBody(verb, from, until, metadataPrefix, set, resumptionToken, handlerRequestBuilder);
    }
    return handlerRequestBuilder.build();
  }

  private static void addAsBody(
      String verb,
      String from,
      String until,
      String metadataPrefix,
      String set,
      String resumptionToken,
      HandlerRequestBuilder<String> handlerRequestBuilder)
      throws JsonProcessingException {
    var bodyBuilder = new StringBuilder();
    bodyBuilder.append("verb=").append(verb);
    if (nonNull(metadataPrefix)) {
      bodyBuilder.append("&metadataPrefix=").append(metadataPrefix);
    }
    if (nonNull(from)) {
      bodyBuilder.append("&from=").append(from);
    }
    if (nonNull(until)) {
      bodyBuilder.append("&until=").append(until);
    }
    if (nonNull(set)) {
      bodyBuilder.append("&set=").append(set);
    }
    if (nonNull(resumptionToken)) {
      bodyBuilder.append("&resumptionToken=").append(resumptionToken);
    }

    handlerRequestBuilder.withBody(bodyBuilder.toString());
  }

  private static class GetRecordsRequestHelper {
    public static void applyQueryParams(
        HandlerRequestBuilder<String> handlerRequestBuilder,
        String identifier,
        String metadataPrefix) {
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("verb", VerbType.GET_RECORD.value());
      if (nonNull(identifier)) {
        queryParams.put("identifier", identifier);
      }
      if (nonNull(metadataPrefix)) {
        queryParams.put("metadataPrefix", metadataPrefix);
      }
      handlerRequestBuilder.withQueryParameters(queryParams);
    }

    public static void applyBody(
        HandlerRequestBuilder<String> handlerRequestBuilder,
        String identifier,
        String metadataPrefix)
        throws JsonProcessingException {
      var bodyBuilder = new StringBuilder();
      bodyBuilder.append("verb=").append(VerbType.GET_RECORD.value());
      if (nonNull(identifier)) {
        bodyBuilder.append("&identifier=").append(identifier);
      }
      if (nonNull(metadataPrefix)) {
        bodyBuilder.append("&metadataPrefix=").append(metadataPrefix);
      }

      handlerRequestBuilder.withBody(bodyBuilder.toString());
    }
  }

  private static void addAsQueryParams(
      String verb,
      String from,
      String until,
      String metadataPrefix,
      String set,
      String resumptionToken,
      HandlerRequestBuilder<String> handlerRequestBuilder) {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("verb", verb);
    if (nonNull(metadataPrefix)) {
      queryParams.put("metadataPrefix", metadataPrefix);
    }
    if (nonNull(from)) {
      queryParams.put("from", from);
    }
    if (nonNull(until)) {
      queryParams.put("until", until);
    }
    if (nonNull(set)) {
      queryParams.put("set", set);
    }
    if (nonNull(resumptionToken)) {
      queryParams.put("resumptionToken", resumptionToken);
    }
    handlerRequestBuilder.withQueryParameters(queryParams);
  }

  private java.util.Set<String> extractRecordIdentifiers(Iterable<Node> recordNodes) {
    var identifiers = new HashSet<String>();
    recordNodes.forEach(
        recordNode -> {
          var headerNode = getNamedChildNode(recordNode, "header").orElseThrow();
          var identifierNode = getNamedChildNode(headerNode, "identifier").orElseThrow();
          identifiers.add(identifierNode.getFirstChild().getNodeValue());
        });
    return identifiers;
  }

  private Optional<Node> getNamedChildNode(Node node, String name) {
    var children = node.getChildNodes();
    for (var index = 0; index < children.getLength(); index++) {
      var child = children.item(index);
      if (child.getNodeName().equals(name)) {
        return Optional.of(child);
      }
    }
    return Optional.empty();
  }

  private Optional<String> getTextValueOfNamedChild(Node node, String childName) {
    var childNode = getNamedChildNode(node, childName).orElseThrow();
    return Optional.ofNullable(childNode.getFirstChild().getNodeValue());
  }

  private static String getIdentifyChildNodeText(
      XPathEngine xpathEngine, Source source, String childNodeName) {
    return xpathEngine
        .selectNodes("/oai:OAI-PMH/oai:Identify/oai:" + childNodeName, source)
        .iterator()
        .next()
        .getFirstChild()
        .getNodeValue();
  }

  private static void assertXmlResponseWithError(
      Source response, OAIPMHerrorcodeType errorCode, String message) {
    var xpathEngine = getXpathEngine();
    var errorNodes = xpathEngine.selectNodes("/oai:OAI-PMH/oai:error", response);

    assertThat(errorNodes, iterableWithSize(1));

    var errorNode = errorNodes.iterator().next();
    assertThat(
        errorNode.getAttributes().getNamedItem(CODE_ATTRIBUTE_NAME).getNodeValue(),
        is(equalTo(errorCode.value())));
    assertThat(errorNode.getFirstChild().getNodeValue(), is(equalTo(message)));
  }

  private SwsResponse swsResponse() throws JsonProcessingException {
    return new SwsResponse(
        0,
        false,
        null,
        new HitsInfo(new TotalInfo(0, ""), 1.0, Collections.emptyList()),
        aggregations(),
        null);
  }

  private SwsResponse initialSwsResponse(JsonNode hits) {
    var hitList = new ArrayList<Hit>();
    var iterator = hits.elements();

    while (iterator.hasNext()) {
      var element = iterator.next();
      hitList.add(new Hit("", "", "", 1.0, element, null, List.of()));
    }
    return new SwsResponse(
        0, false, null, new HitsInfo(new TotalInfo(hitList.size(), ""), 1.0, hitList), null, null);
  }

  private SwsResponse firstPageSwsResponse() throws JsonProcessingException {
    return createSwsResponse("firstPageHits.json", new TotalInfo(8, ""));
  }

  private SwsResponse secondPageSwsResponse() throws JsonProcessingException {
    return createSwsResponse("secondPageHits.json", new TotalInfo(5, ""));
  }

  private SwsResponse lastPageSwsResponse() throws JsonProcessingException {
    return createSwsResponse("thirdAndLastPageHits.json", new TotalInfo(2, ""));
  }

  private SwsResponse createSwsResponse(String resourceFileName, TotalInfo totalInfo)
      throws JsonProcessingException {
    var hitsJson = readHits(resourceFileName);
    var hits = createHits(hitsJson);
    var hitsInfo = new HitsInfo(totalInfo, 1.0, hits);

    return new SwsResponse(0, false, null, hitsInfo, null, null);
  }

  private ArrayNode readHits(String fileName) throws JsonProcessingException {
    var json = IoUtils.stringFromResources(Path.of(fileName));
    return (ArrayNode) JsonUtils.dtoObjectMapper.readTree(json);
  }

  private List<Hit> createHits(ArrayNode hitsJson) {
    List<Hit> hits = new ArrayList<>();
    hitsJson.forEach(element -> hits.add(new Hit("", "", "", 1.0, element, null, List.of())));
    return hits;
  }

  private JsonNode aggregations() throws JsonProcessingException {
    var aggregationsJson = IoUtils.stringFromResources(Path.of("aggregations.json"));
    return JsonUtils.dtoObjectMapper.readTree(aggregationsJson);
  }

  private Source invokeHandlerAndAssertHttpStatusCodeOk(InputStream inputStream)
      throws JAXBException, IOException {
    return invokeHandlerAndAssertHttpStatus(inputStream, HttpURLConnection.HTTP_OK);
  }

  private void invokeHandlerAndAssertContentType(InputStream request)
      throws JAXBException, IOException {
    var gatewayResponse = invokeHandler(request);
    assertThat(
        gatewayResponse.getHeaders().get("Content-Type"), is(equalTo("text/xml; charset=utf-8")));
  }

  private GatewayResponse<String> invokeHandler(InputStream request)
      throws JAXBException, IOException {
    var context = JAXBContext.newInstance(OAIPMHtype.class);
    var marshaller = context.createMarshaller();
    JaxbUtils.configureMarshaller(marshaller);
    return invokeHandler(environment, new JaxbXmlSerializer(marshaller), request);
  }

  private Source invokeHandlerAndAssertHttpStatus(InputStream inputStream, int statusCode)
      throws JAXBException, IOException {
    var gatewayResponse = invokeHandler(inputStream);
    assertThat(gatewayResponse.getStatusCode(), is(equalTo(statusCode)));
    return Input.fromString(gatewayResponse.getBody()).build();
  }

  private void assertResponseRequestContains(
      EnumMap<OaiPmhParameterName, String> parameters, Source source, JAXPXPathEngine xpathEngine) {
    var requestNodes = xpathEngine.selectNodes("/oai:OAI-PMH/oai:request", source);
    assertThat(requestNodes, iterableWithSize(1));

    var attributes = requestNodes.iterator().next().getAttributes();

    parameters.forEach(
        (key, value) ->
            assertThat(value, is(equalTo(attributes.getNamedItem(key.getName()).getNodeValue()))));
  }

  private static JAXPXPathEngine getXpathEngine() {
    var xpathEngine = new JAXPXPathEngine();
    xpathEngine.setNamespaceContext(
        Map.of(
            OAI_PMH_NAMESPACE_PREFIX, OAI_PMH_NAMESPACE,
            OAI_DC_NAMESPACE_PREFIX, OAI_DC_NAMESPACE,
            DC_NAMESPACE_PREFIX, DC_NAMESPACE));
    return xpathEngine;
  }

  private static InputStream listSetsRequest(String method, String resumptionToken)
      throws JsonProcessingException {
    var handlerRequestBuilder =
        new HandlerRequestBuilder<String>(new ObjectMapper()).withHttpMethod(method);

    if ("get".equalsIgnoreCase(method)) {
      var queryParameters = new HashMap<String, String>();
      queryParameters.put("verb", VerbType.LIST_SETS.value());
      if (nonNull(resumptionToken)) {
        queryParameters.put("resumptionToken", resumptionToken);
      }
      handlerRequestBuilder.withQueryParameters(queryParameters);
    } else if ("post".equalsIgnoreCase(method)) {
      var body = new StringBuilder();
      body.append("verb=").append(VerbType.LIST_SETS.value());
      if (nonNull(resumptionToken)) {
        body.append("&resumptionToken=").append(resumptionToken);
      }
      handlerRequestBuilder.withBody(body.toString());
    }
    return handlerRequestBuilder.build();
  }

  private static InputStream request(String verb, String method) throws JsonProcessingException {
    var handlerRequestBuilder =
        new HandlerRequestBuilder<String>(new ObjectMapper()).withHttpMethod(method);

    if ("get".equalsIgnoreCase(method)) {
      handlerRequestBuilder.withQueryParameters(Map.of("verb", verb));
    } else if ("post".equalsIgnoreCase(method)) {
      handlerRequestBuilder.withBody("verb=" + verb);
    }
    return handlerRequestBuilder.build();
  }

  private GatewayResponse<String> invokeHandler(
      Environment environment, JaxbXmlSerializer marshaller, InputStream inputStream)
      throws IOException {
    var endpointUri = OaiPmhHandler.generateEndpointUri(environment);
    var identifierBaseUri = OaiPmhHandler.generateIdentifierBaseUri(environment);
    var dataProvider =
        new DefaultOaiPmhMethodRouter(
            new ResourceClientResourceRepository(resourceClient),
            3,
            endpointUri,
            identifierBaseUri);
    var handler = new OaiPmhHandler(environment, dataProvider, marshaller);
    handler.handleRequest(inputStream, outputStream, new FakeContext());

    return GatewayResponse.fromOutputStream(outputStream, String.class);
  }

  private static InputStream emptyRequest() throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(new ObjectMapper()).build();
  }

  private static Stream<Arguments> verbsAndMethodCombinations() {
    return Stream.of(
        Arguments.of(VerbType.LIST_IDENTIFIERS, GET_METHOD),
        Arguments.of(VerbType.LIST_IDENTIFIERS, POST_METHOD));
  }

  static Stream<Arguments> allSupportedRequestsPerMethodProvider() throws JsonProcessingException {
    return Stream.of(
        Arguments.argumentSet(
            "GET Identify should return correct content-type",
            generateSimpleRequest(GET_METHOD, VerbType.IDENTIFY)),
        Arguments.argumentSet(
            "POST Identify should return correct content-type",
            generateSimpleRequest(POST_METHOD, VerbType.IDENTIFY)),
        Arguments.argumentSet(
            "GET ListMetadataFormats should return correct content-type",
            generateSimpleRequest(GET_METHOD, VerbType.LIST_METADATA_FORMATS)),
        Arguments.argumentSet(
            "POST ListMetadataFormats should return correct content-type",
            generateSimpleRequest(POST_METHOD, VerbType.LIST_METADATA_FORMATS)),
        Arguments.argumentSet(
            "GET ListSets should return correct content-type",
            generateSimpleRequest(GET_METHOD, VerbType.LIST_SETS)),
        Arguments.argumentSet(
            "POST ListSets should return correct content-type",
            generateSimpleRequest(POST_METHOD, VerbType.LIST_SETS)),
        Arguments.argumentSet(
            "GET ListRecords should return correct content-type",
            generateSimpleRequest(GET_METHOD, VerbType.LIST_RECORDS)),
        Arguments.argumentSet(
            "GET ListRecords should return correct content-type",
            generateSimpleRequest(POST_METHOD, VerbType.LIST_RECORDS)),
        Arguments.argumentSet(
            "GET GetRecord should return correct content-type",
            generateSimpleRequest(GET_METHOD, VerbType.GET_RECORD)),
        Arguments.argumentSet(
            "POST GetRecord should return correct content-type",
            generateSimpleRequest(POST_METHOD, VerbType.GET_RECORD)));
  }

  private static InputStream generateSimpleRequest(String method, VerbType verbType)
      throws JsonProcessingException {
    var handlerRequestBuilder =
        new HandlerRequestBuilder<String>(new ObjectMapper()).withHttpMethod(method);

    if (GET_METHOD.equalsIgnoreCase(method)) {
      addAsQueryParams(verbType.value(), null, null, null, null, null, handlerRequestBuilder);
    } else if (POST_METHOD.equalsIgnoreCase(method)) {
      addAsBody(verbType.value(), null, null, null, null, null, handlerRequestBuilder);
    }
    return handlerRequestBuilder.build();
  }

  void mockRepositoryWithEmptyResponsesForAllQueries() {
    when(resourceClient.doSearch(any(), eq(RESOURCES))).thenReturn(emptySwsResponse());
  }

  private SwsResponse emptySwsResponse() {
    return new SwsResponse(
        0,
        false,
        null,
        new HitsInfo(new TotalInfo(0, null), 0.0, Collections.emptyList()),
        new ObjectNode(JsonNodeFactory.instance),
        null);
  }

  private static Stream<Arguments> datestampIssueListRecordsParameterProvider() {
    var secondsGranularityDate = "2020-01-01T00:00:00Z";
    var nanosGranularityDate = "2020-01-01T00:00:00.123456789Z";
    var nameTemplate =
        "%s request with nano resolution in %s parameter should report bad argument error.";
    return Stream.of(
        Arguments.argumentSet(
            nameTemplate.formatted(GET_METHOD, "until"),
            GET_METHOD,
            secondsGranularityDate,
            nanosGranularityDate),
        Arguments.argumentSet(
            nameTemplate.formatted(GET_METHOD, "from"),
            GET_METHOD,
            nanosGranularityDate,
            secondsGranularityDate),
        Arguments.argumentSet(
            nameTemplate.formatted(GET_METHOD, "until"),
            POST_METHOD,
            secondsGranularityDate,
            nanosGranularityDate),
        Arguments.argumentSet(
            nameTemplate.formatted(GET_METHOD, "from"),
            POST_METHOD,
            nanosGranularityDate,
            secondsGranularityDate));
  }
}
