package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

public class OaiPmhHandlerTest {

  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  @Test
  void shouldReturnKnownResponseWhenNotImplemented() throws IOException {
    var environment = Mockito.mock(Environment.class);
    when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");

    var inputStream = new HandlerRequestBuilder<Void>(new ObjectMapper()).build();

    var handler = new OaiPmhHandler(environment);
    handler.handleRequest(inputStream, outputStream, new FakeContext());

    var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, String.class);
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
}
