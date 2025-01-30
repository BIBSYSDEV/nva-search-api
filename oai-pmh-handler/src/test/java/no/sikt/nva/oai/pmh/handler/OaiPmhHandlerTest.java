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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.transform.Source;
import no.unit.nva.commons.json.JsonUtils;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.VerbType;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

public class OaiPmhHandlerTest {

  private static final String OAI_PMH_NAMESPACE_PREFIX = "oai";
  private static final String OAI_PMH_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";

  private static final String[] EXPECTED_SET_SPECS = {
    "PublicationInstanceType",
    "PublicationInstanceType:AcademicArticle",
    "PublicationInstanceType:AcademicChapter"
  };
  private static final String EMPTY_STRING = "";
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private Environment environment;
  private ResourceClient resourceClient;

  @BeforeEach
  public void setUp() {
    this.environment = mock(Environment.class);
    when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");
    when(environment.readEnv("SEARCH_INFRASTRUCTURE_API_URI"))
        .thenReturn("https://example.com/search");

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
  @ValueSource(
      strings = {"Identify", "ListMetadataFormats", "GetRecord", "ListIdentifiers", "ListRecords"})
  void shouldReturnErrorResponseWhenVerbIsKnownButNotSupported(final String verb)
      throws IOException, JAXBException {
    var inputStream = request(verb);

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);
    assertXmlResponseWithError(response, OAIPMHerrorcodeType.BAD_VERB, "Unsupported verb.");
  }

  @Test
  void shouldReturnErrorResponseWhenVerbIsUnknown() throws IOException, JAXBException {
    var inputStream = request("Unknown");

    var response = invokeHandlerAndAssertHttpStatusCodeOk(inputStream);

    assertXmlResponseWithError(
        response, OAIPMHerrorcodeType.BAD_VERB, "Unknown or no verb supplied.");
  }

  @Test
  void shouldReturnExpectedSetsWhenAskingForListSets() throws IOException, JAXBException {
    when(resourceClient.doSearch(argThat(new ResourceSearchQueryMatcher(0, 0, "all"))))
        .thenReturn(swsResponse());

    var inputStream = request(VerbType.LIST_SETS.value());

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

  @Test
  void shouldReturnExpectedErrorAndLogWhenSearchFailsForListSets()
      throws IOException, JAXBException {
    final var appender = LogUtils.getTestingAppenderForRootLogger();

    doThrow(new RuntimeException(EMPTY_STRING)).when(resourceClient).doSearch(any());

    var inputStream = request(VerbType.LIST_SETS.value());

    invokeHandlerAndAssertHttpStatus(inputStream, HTTP_INTERNAL_ERROR);

    assertThat(
        appender.getMessages(),
        containsString(
            "Failed to search for publication instance types using 'type' aggregation."));
  }

  private static void assertXmlResponseWithError(
      Source response, OAIPMHerrorcodeType errorCode, String message) {
    var xpathEngine = getXpathEngine();
    var errorNodes = xpathEngine.selectNodes("/oai:OAI-PMH/oai:error", response);

    assertThat(errorNodes, iterableWithSize(1));

    var errorNode = errorNodes.iterator().next();
    assertThat(
        errorNode.getAttributes().getNamedItem("code").getNodeValue(),
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

    var verb = requestNodes.iterator().next().getAttributes().getNamedItem("verb").getNodeValue();
    assertThat(verb, is(equalTo(verbType.value())));
  }

  private static JaxbXmlSerializer createXmlSerializer() throws JAXBException {
    var context = JAXBContext.newInstance(OAIPMHtype.class);
    var marshaller = context.createMarshaller();
    return new JaxbXmlSerializer(marshaller);
  }

  private static JAXPXPathEngine getXpathEngine() {
    var xpathEngine = new JAXPXPathEngine();
    xpathEngine.setNamespaceContext(Map.of(OAI_PMH_NAMESPACE_PREFIX, OAI_PMH_NAMESPACE));
    return xpathEngine;
  }

  private static InputStream request(String verb) throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(new ObjectMapper())
        .withQueryParameters(Map.of("verb", verb))
        .build();
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
}
