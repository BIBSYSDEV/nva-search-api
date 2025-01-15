package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

public class OaiPmhHandlerTest {

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private Environment environment;

  @BeforeEach
  public void setUp() {
    this.environment = Mockito.mock(Environment.class);
    when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");
  }

  @Test
  void shouldReturnInternalServerErrorWithProperLoggingWhenXmlMarshallingFails()
      throws IOException, JAXBException {
    final var appender = LogUtils.getTestingAppenderForRootLogger();

    var inputStream = emptyRequest();

    var marshaller = new FakeMarshaller();

    var gatewayResponse =
        invokeHandler(environment, new JaxbXmlSerializer(marshaller), inputStream);

    assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    assertThat(appender.getMessages(), CoreMatchers.containsString("Ha ha!"));
  }

  @Test
  void shouldReturnKnownResponseWhenNotImplemented() throws IOException, JAXBException {
    var environment = Mockito.mock(Environment.class);
    when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");

    var inputStream = emptyRequest();

    var context = JAXBContext.newInstance(OAIPMHtype.class);
    var marshaller = context.createMarshaller();
    var gatewayResponse =
        invokeHandler(environment, new JaxbXmlSerializer(marshaller), inputStream);
    assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

    var response = gatewayResponse.getBody();

    var xpathEngine = new JAXPXPathEngine();
    xpathEngine.setNamespaceContext(Map.of("oai", "http://www.openarchives.org/OAI/2.0/"));

    var errorNodes =
        xpathEngine.selectNodes("/oai:OAI-PMH/oai:error", Input.fromString(response).build());

    assertThat(errorNodes, iterableWithSize(1));

    var errorNode = errorNodes.iterator().next();
    assertThat(
        errorNode.getAttributes().getNamedItem("code").getNodeValue(), is(equalTo("badVerb")));
    assertThat(errorNode.getFirstChild().getNodeValue(), is(equalTo("Not implemented yet!")));
  }

  private GatewayResponse<String> invokeHandler(
      Environment environment, JaxbXmlSerializer marshaller, InputStream inputStream)
      throws IOException {
    var handler = new OaiPmhHandler(environment, marshaller);
    handler.handleRequest(inputStream, outputStream, new FakeContext());

    return GatewayResponse.fromOutputStream(outputStream, String.class);
  }

  private static InputStream emptyRequest() throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(new ObjectMapper()).build();
  }
}
