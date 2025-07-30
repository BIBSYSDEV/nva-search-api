package no.sikt.nva.oai.pmh.handler;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
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
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.transform.Source;
import no.sikt.nva.oai.pmh.handler.oaipmh.DefaultOaiPmhMethodRouter;
import no.sikt.nva.oai.pmh.handler.oaipmh.ListRecordsRequest;
import no.sikt.nva.oai.pmh.handler.oaipmh.MetadataPrefix;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.ResumptionToken;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
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

@WireMockTest
public class OaiPmhHandlerTest {

  private static final SetSpec NO_SET = null;
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
  private static final String VERB_ATTRIBUTE_NAME = "verb";
  private static final String GET_METHOD = "get";
  private static final String POST_METHOD = "post";
  private static final String OAI_DC_NAMESPACE_PREFIX = "oai-dc";
  private static final String OAI_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
  public static final String DEFAULT_PUBLICATION_IDENTIFIER = "1";

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private Environment environment;
  private ResourceClient resourceClient;
  private int port = 0;

  @BeforeEach
  public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
    this.environment = mock(Environment.class);
    when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");
    when(environment.readEnv("SEARCH_INFRASTRUCTURE_API_URI"))
        .thenReturn("https://example.com/search");
    when(environment.readEnv("API_HOST")).thenReturn("localhost");
    when(environment.readEnv("OAI_BASE_PATH")).thenReturn("publication-oai-pmh");
    when(environment.readEnv("COGNITO_AUTHORIZER_URLS")).thenReturn("http://localhost:3000");
    this.resourceClient = mock(ResourceClient.class);
    var context = IoUtils.stringFromResources(Path.of("publication.context"));
    stubFor(
        get("/publication/context")
            .willReturn(ok(context).withHeader("Content-Type", "application/json")));
    this.port = wireMockRuntimeInfo.getHttpPort();
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
            .build();
    when(resourceClient.doSearch(argThat(matcher), eq(RESOURCES))).thenReturn(swsResponse());

    var inputStream = listSetsRequest(method, null);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.LIST_SETS, response, xpathEngine);

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
        containsString(
            "Failed to search for publication instance types using 'type' aggregation."));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithRepositoryName(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

    var repositoryName = getIdentifyChildNodeText(xpathEngine, response, "repositoryName");
    assertThat(repositoryName, is(equalTo("NVA-OAI-PMH")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithBaseURL(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

    var baseURL = getIdentifyChildNodeText(xpathEngine, response, "baseURL");

    assertThat(baseURL, is(equalTo("https://localhost/publication-oai-pmh")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithProtocolVersion(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

    var protocolVersion =
        getIdentifyChildNodeText(xpathEngine, response, PROTOCOL_VERSION_NODE_NAME);

    assertThat(protocolVersion, is(equalTo("2.0")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithEarliestDatestamp(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

    var earliestDatestamp =
        getIdentifyChildNodeText(xpathEngine, response, EARLIEST_DATESTAMP_NODE_NAME);

    assertThat(earliestDatestamp, is(equalTo("2016-01-01")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithDeletedRecord(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

    var deletedRecord = getIdentifyChildNodeText(xpathEngine, response, DELETED_RECORD_NODE_NAME);

    assertThat(deletedRecord, is(equalTo(DeletedRecordType.NO.value())));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithGranularity(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

    var granularity = getIdentifyChildNodeText(xpathEngine, response, GRANULARITY_NODE_NAME);

    assertThat(granularity, is(equalTo("YYYY-MM-DDThh:mm:ssZ")));
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldIdentifyWithAdminEmail(String method) throws IOException, JAXBException {
    var inputStream = request(VerbType.IDENTIFY.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.IDENTIFY, response, xpathEngine);

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

    assertResponseRequestContains(VerbType.LIST_METADATA_FORMATS, response, xpathEngine);

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
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsOnInitialQueryWithOnlyRequiredParameters(String method) throws Exception {
    String currentPageFromDate = null;
    final OaiPmhDateTime fromDate = null;
    final OaiPmhDateTime untilDate = null;
    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea64",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f1",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0c");

    runListRecordsTest(
        method, currentPageFromDate, fromDate, untilDate, null, 3, expectedIdentifiers, true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsOnInitialQuery(String method) throws Exception {
    String currentPageFromDate = null;
    var fromDate = OaiPmhDateTime.from("2016-01-01");
    var untilDate = OaiPmhDateTime.from("2017-01-01");
    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea64",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f1",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0c");

    runListRecordsTest(
        method, currentPageFromDate, fromDate, untilDate, null, 3, expectedIdentifiers, true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldListRecordsWithResumptionToken(String method) throws Exception {
    var fromDate = OaiPmhDateTime.from("2016-01-01");
    var untilDate = OaiPmhDateTime.from("2017-01-01");
    var metadataPrefix = MetadataPrefix.OAI_DC;
    var currentPageFromDate = "2016-01-04T05:48:31Z";
    var listRecordsRequest = new ListRecordsRequest(fromDate, untilDate, NO_SET, metadataPrefix);
    var resumptionToken =
        new ResumptionToken(listRecordsRequest, currentPageFromDate, 8).getValue();

    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea65",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f2",
            "https://api.unittests.nva.aws.unit.no/publication/019527b845e4-182ebbf0-9481-4a98-aad2-76b617cc1b0d");

    runListRecordsTest(
        method,
        currentPageFromDate,
        fromDate,
        untilDate,
        resumptionToken,
        3,
        expectedIdentifiers,
        true);
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldNotReturnResumptionTokenOnLastPage(String method) throws Exception {
    var fromDate = OaiPmhDateTime.from("2016-01-01");
    var untilDate = OaiPmhDateTime.from("2017-01-01");
    var currentPageFromDate = "2016-01-07T05:48:31Z";
    var metadataPrefix = MetadataPrefix.OAI_DC;
    var listRecordsRequest = new ListRecordsRequest(fromDate, untilDate, NO_SET, metadataPrefix);

    var resumptionToken =
        new ResumptionToken(listRecordsRequest, currentPageFromDate, 8).getValue();

    var expectedIdentifiers =
        List.of(
            "https://api.unittests.nva.aws.unit.no/publication/019527b847ad-ee78bdbe-3f70-4ff4-930c-b4ace492ea66",
            "https://api.unittests.nva.aws.unit.no/publication/019527b84693-a86c1cae-24da-4c9d-9bff-e097fd9be2f3");

    runListRecordsTest(
        method,
        currentPageFromDate,
        fromDate,
        untilDate,
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

    assertThat(identifier, is(equalTo("https://localhost/publication/1")));
  }

  @Test
  void shouldPopulateRecordHeaderDatestamp() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var datestamp =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:header/oai:datestamp",
            response);

    assertThat(datestamp, is(not(nullValue())));
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
  void shouldUsePublicationDateAsRecordMetadataDcDate() throws IOException, JAXBException {
    var inputStream = defaultHitAndRequest();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    var date =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:date",
            response);
    assertThat(date, is(equalTo("2020-01-01")));
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

    var type =
        extractTextNodeValueFromResponse(
            xpathEngine,
            "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:identifier",
            response);
    assertThat(
        type, is(equalTo("https://localhost/publication/" + DEFAULT_PUBLICATION_IDENTIFIER)));
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
    assertThat(publisher, is(equalTo("My publisher name")));
  }

  @Test
  void shouldOmitJournalNameAsPublisherIfDataIsIncomplete() throws IOException, JAXBException {
    var inputStream = requestWithReportBasicHitWithIncompletePublisherInformation();

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    var xpathEngine = getXpathEngine();

    assertNotPresent(
        xpathEngine,
        "/oai:OAI-PMH/oai:ListRecords/oai:record/oai:metadata/oai-dc:dc/dc:publisher",
        response);
  }

  @Test
  void shouldOmitPublisherNameAsPublisherIfDataIsIncomplete() throws IOException, JAXBException {
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

  private void assertNotPresent(JAXPXPathEngine xpathEngine, String expression, Source source) {
    assertThat(xpathEngine.selectNodes(expression, source), is(emptyIterable()));
  }

  private void runListRecordsTest(
      final String method,
      final String currentPageFromDate,
      final OaiPmhDateTime fromDate,
      final OaiPmhDateTime untilDate,
      final String resumptionToken,
      final int expectedRecordCount,
      final List<String> expectedIdentifiers,
      final boolean expectResumptionToken)
      throws Exception {

    var matcher =
        buildMatcher(
            nonNull(currentPageFromDate)
                ? currentPageFromDate
                : nonNull(fromDate) ? fromDate.asString() : null,
            nonNull(untilDate) ? untilDate.asString() : null);
    var swsResponse = resolveMockResponse(currentPageFromDate, expectedRecordCount);
    when(resourceClient.doSearch(argThat(matcher), any())).thenReturn(swsResponse);

    var metadataPrefix = MetadataPrefix.OAI_DC.getPrefix();
    var response =
        performListRecordsOperation(
            method,
            nonNull(fromDate) ? fromDate.asString() : null,
            nonNull(untilDate) ? untilDate.asString() : null,
            metadataPrefix,
            null,
            resumptionToken);
    var xpathEngine = getXpathEngine();

    assertResponseRequestContains(VerbType.LIST_RECORDS, response, xpathEngine);

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
      var expectedCurrentInToken =
          nonNull(currentPageFromDate)
              ? "2016-01-06T08:55:42.820948673Z"
              : "2016-01-03T08:55:42.820948673Z";
      assertThat(
          "Expects current in token to be 1 ms ahead of the last record in page",
          token.current(),
          is(equalTo(expectedCurrentInToken)));
    } else {
      assertNoResumptionToken(xpathEngine, response);
    }
  }

  private SwsResponse resolveMockResponse(String currentPageFromDate, int expectedRecordCount)
      throws Exception {
    return switch (expectedRecordCount) {
      case 3 -> isNull(currentPageFromDate) ? firstPageSwsResponse() : secondPageSwsResponse();
      // on context
      case 2 -> lastPageSwsResponse();
      default ->
          throw new IllegalArgumentException("Unexpected record count: " + expectedRecordCount);
    };
  }

  private ResourceSearchQueryMatcher buildMatcher(String from, String until) {
    var builder =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "3")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE);

    if (nonNull(from)) {
      builder.withSearchParameter(ResourceParameter.MODIFIED_SINCE, from);
    }
    if (nonNull(until)) {
      builder.withSearchParameter(ResourceParameter.MODIFIED_BEFORE, until);
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
      String set,
      String resumptionToken)
      throws JAXBException, IOException {
    var inputStream =
        request(
            VerbType.LIST_RECORDS.value(),
            method,
            from,
            until,
            metadataPrefix,
            set,
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
    return defaultHitAndRequest(null);
  }

  private InputStream defaultHitAndRequest(SetSpec setSpec) throws JsonProcessingException {
    var hits =
        new ArrayNode(
            JsonNodeFactory.instance,
            List.of(
                HitBuilder.academicArticle(port, "My journal")
                    .withIdentifier(DEFAULT_PUBLICATION_IDENTIFIER)
                    .withTitle("My title")
                    .withContributors("Ola Nordmann")
                    .build()));
    return hitAndRequest(hits, setSpec);
  }

  private InputStream academicArticleWithIncompleteJournalInformation()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(
            JsonNodeFactory.instance,
            List.of(
                HitBuilder.academicArticleWithMissingJournalInformation(port)
                    .withIdentifier(DEFAULT_PUBLICATION_IDENTIFIER)
                    .withTitle("My title")
                    .withContributors("Ola Nordmann")
                    .build()));
    return hitAndRequest(hits);
  }

  private InputStream hitAndRequest(ArrayNode hits) throws JsonProcessingException {
    return hitAndRequest(hits, null);
  }

  private InputStream hitAndRequest(ArrayNode hits, SetSpec setSpec)
      throws JsonProcessingException {
    var scrollId = randomString();
    var queryBuilder =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "3")
            .withPageParameter(ResourceParameter.SORT, "modifiedDate:asc,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, Words.NONE)
            .withSearchParameter(ResourceParameter.MODIFIED_BEFORE, "2016-01-02T00:00:00Z")
            .withSearchParameter(ResourceParameter.MODIFIED_SINCE, "2016-01-01T00:00:00Z");
    if (nonNull(setSpec) && SetRoot.RESOURCE_TYPE_GENERAL.equals(setSpec.root())) {
      queryBuilder.withSearchParameter(ResourceParameter.INSTANCE_TYPE, setSpec.children()[0]);
    }
    var resourceQueryMatcher =
        queryBuilder
            .withNamedFilterQuery(
                "status",
                new TermsQueryBuilderExpectation(
                    "status.keyword", "PUBLISHED", "PUBLISHED_METADATA"))
            .build();

    when(resourceClient.doSearch(argThat(resourceQueryMatcher), any()))
        .thenReturn(initialSwsResponse(hits, scrollId));

    var from = "2016-01-01";
    var until = "2016-01-02";
    var metadataPrefix = MetadataPrefix.OAI_DC.getPrefix();
    String resumptionToken = null;
    return request(
        VerbType.LIST_RECORDS.value(),
        "GET",
        from,
        until,
        metadataPrefix,
        nonNull(setSpec) ? setSpec.asString() : null,
        resumptionToken);
  }

  private InputStream requestWithReportBasicHit() throws JsonProcessingException {
    var hits =
        new ArrayNode(
            JsonNodeFactory.instance,
            List.of(
                HitBuilder.reportBasic(port, "My publisher name", "My series name")
                    .withIdentifier("1")
                    .withTitle("My title")
                    .build()));
    return hitAndRequest(hits);
  }

  private InputStream requestWithReportBasicHitWithIncompletePublisherInformation()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(
            JsonNodeFactory.instance,
            List.of(
                HitBuilder.reportBasicWithMissingChannelName(port)
                    .withIdentifier("1")
                    .withTitle("My title")
                    .build()));
    return hitAndRequest(hits);
  }

  private InputStream requestWithReportBasicHitMissingPublicationDate()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(
            JsonNodeFactory.instance,
            List.of(
                HitBuilder.reportBasic(port, "My publisher name", "My series name")
                    .withIdentifier("1")
                    .withTitle("My title")
                    .withNoPublicationDate()
                    .build()));
    return hitAndRequest(hits);
  }

  private InputStream requestWithReportBasicHitEmptyPublicationDate()
      throws JsonProcessingException {
    var hits =
        new ArrayNode(
            JsonNodeFactory.instance,
            List.of(
                HitBuilder.reportBasic(port, "My publisher name", "My series name")
                    .withIdentifier("1")
                    .withTitle("My title")
                    .withEmptyPublicationDate()
                    .build()));
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

  private SwsResponse initialSwsResponse(JsonNode hits, String scrollId) {
    var hitList = new ArrayList<Hit>();
    var iterator = hits.elements();

    while (iterator.hasNext()) {
      var element = iterator.next();
      hitList.add(new Hit("", "", "", 1.0, element, null, List.of()));
    }
    return new SwsResponse(
        0,
        false,
        null,
        new HitsInfo(new TotalInfo(hitList.size(), ""), 1.0, hitList),
        null,
        scrollId);
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

  private Source invokeHandlerAndAssertHttpStatus(InputStream inputStream, int statusCode)
      throws JAXBException, IOException {
    var context = JAXBContext.newInstance(OAIPMHtype.class);
    var marshaller = context.createMarshaller();
    JaxbUtils.configureMarshaller(marshaller);
    var gatewayResponse =
        invokeHandler(environment, new JaxbXmlSerializer(marshaller), inputStream);
    assertThat(gatewayResponse.getStatusCode(), is(equalTo(statusCode)));
    return Input.fromString(gatewayResponse.getBody()).build();
  }

  private void assertResponseRequestContains(
      VerbType verbType, Source source, JAXPXPathEngine xpathEngine) {
    var requestNodes = xpathEngine.selectNodes("/oai:OAI-PMH/oai:request", source);
    assertThat(requestNodes, iterableWithSize(1));

    var verb =
        requestNodes
            .iterator()
            .next()
            .getAttributes()
            .getNamedItem(VERB_ATTRIBUTE_NAME)
            .getNodeValue();
    assertThat(verb, is(equalTo(verbType.value())));
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
    var dataProvider = new DefaultOaiPmhMethodRouter(resourceClient, 3);
    var handler = new OaiPmhHandler(environment, dataProvider, marshaller);
    handler.handleRequest(inputStream, outputStream, new FakeContext());

    return GatewayResponse.fromOutputStream(outputStream, String.class);
  }

  private static InputStream emptyRequest() throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(new ObjectMapper()).build();
  }

  private static Stream<Arguments> verbsAndMethodCombinations() {
    return Stream.of(
        Arguments.of(VerbType.GET_RECORD, GET_METHOD),
        Arguments.of(VerbType.LIST_IDENTIFIERS, GET_METHOD),
        Arguments.of(VerbType.GET_RECORD, POST_METHOD),
        Arguments.of(VerbType.LIST_IDENTIFIERS, POST_METHOD));
  }
}
