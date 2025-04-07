package no.sikt.nva.oai.pmh.handler;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.transform.Source;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.TotalInfo;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
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

  private static final String[] EXPECTED_SET_SPECS = {
    "PublicationInstanceType",
    "PublicationInstanceType:AcademicArticle",
    "PublicationInstanceType:AcademicChapter"
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
    this.resourceClient = mock(ResourceClient.class);
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
        response, OAIPMHerrorcodeType.BAD_VERB, "Unknown or no verb supplied.");
  }

  @ParameterizedTest
  @MethodSource("verbsAndMethodCombinations")
  void shouldReturnErrorResponseWhenVerbIsKnownButNotSupported(
      final VerbType verb, final String method) throws IOException, JAXBException {
    var inputStream = request(verb.value(), method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertXmlResponseWithError(response, OAIPMHerrorcodeType.BAD_VERB, "Unsupported verb.");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnErrorResponseWhenVerbIsUnknown(String method) throws IOException, JAXBException {
    var inputStream = request("Unknown", method);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertXmlResponseWithError(
        response, OAIPMHerrorcodeType.BAD_VERB, "Unknown or no verb supplied.");
  }

  @ParameterizedTest
  @ValueSource(strings = {GET_METHOD, POST_METHOD})
  void shouldReturnExpectedSetsWhenAskingForListSets(String method)
      throws IOException, JAXBException {
    when(resourceClient.doSearch(
            argThat(new ResourceSearchQueryMatcher(0, 0, "all")), eq(Words.RESOURCES)))
        .thenReturn(swsResponse());

    var inputStream = request(VerbType.LIST_SETS.value(), method);

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
  void shouldReturnExpectedErrorAndLogWhenSearchFailsForListSets(String method)
      throws IOException, JAXBException {
    final var appender = LogUtils.getTestingAppenderForRootLogger();

    doThrow(new RuntimeException(EMPTY_STRING))
        .when(resourceClient)
        .doSearch(any(), eq(Words.RESOURCES));

    var inputStream = request(VerbType.LIST_SETS.value(), method);

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

    assertThat(granularity, is(equalTo("YYYY-MM-DD")));
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

    assertThat(metadataPrefix.orElseThrow(), is(equalTo("oai-dc")));
    assertThat(schema.orElseThrow(), is(equalTo("http://www.openarchives.org/OAI/2.0/oai_dc.xsd")));
    assertThat(
        metadataNamespace.orElseThrow(),
        is(equalTo("http://www.openarchives.org/OAI/2.0/oai_dc/")));
  }

  private Optional<String> getTextValueOfNamedChild(Node node, String childName) {
    var children = node.getChildNodes();
    for (var index = 0; index < children.getLength(); index++) {
      var child = children.item(index);
      if (child.getNodeName().equals(childName)) {
        return Optional.of(child.getFirstChild().getNodeValue());
      }
    }
    return Optional.empty();
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
    xpathEngine.setNamespaceContext(Map.of(OAI_PMH_NAMESPACE_PREFIX, OAI_PMH_NAMESPACE));
    return xpathEngine;
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
    var handler = new OaiPmhHandler(environment, marshaller, resourceClient);
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
        Arguments.of(VerbType.LIST_RECORDS, GET_METHOD),
        Arguments.of(VerbType.GET_RECORD, POST_METHOD),
        Arguments.of(VerbType.LIST_IDENTIFIERS, POST_METHOD),
        Arguments.of(VerbType.LIST_RECORDS, POST_METHOD));
  }
}
