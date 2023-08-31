package no.unit.nva.search2;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Path;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.common.OpenSearchResponseDto;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static no.unit.nva.search2.SwsOpenSearchClient.prepareWithSecretReader;
import static no.unit.nva.search2.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwsOpenSearchClientTest {

    private SwsOpenSearchClient swsOpenSearchClient;
    private static final String MEDIA_TYPE = "application/json";
    private static final URI REQUEST_URI = URI.create("https://example.com/?name=hello+world&lang=en");
    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";

    @Mock
    private RawContentRetriever contentRetriever;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        //swsOpenSearchClient = SwsOpenSearchClient.defaultSwsClient();
        swsOpenSearchClient = new SwsOpenSearchClient(contentRetriever, MEDIA_TYPE);
    }

    @Test
    void constructorWithSecretsReaderDefinedShouldCreateInstance() {
        var secretsReaderMock = mock(SecretsReader.class);
        var testCredentials = new UsernamePasswordWrapper("user", "password");
        when(secretsReaderMock.fetchClassSecret(anyString(), eq(UsernamePasswordWrapper.class)))
            .thenReturn(testCredentials);
        var searchClient = prepareWithSecretReader(secretsReaderMock);
        assertNotNull(searchClient);
    }

    @Test
    void searchSingleTermReturnsResponse() throws IOException {
        var swsOpenSearchClient = mock(SwsOpenSearchClient.class);
        var jsonResponse = stringFromResources(Path.of(NO_HITS_RESPONSE_JSON));
        var typeReference = new TypeReference<OpenSearchResponseDto>() { };
        var response = objectMapperWithEmpty.readValue(jsonResponse,typeReference);

        when(swsOpenSearchClient.doSearch(any())).thenReturn(response.toSearchResponseDto(REQUEST_URI));

        var searchResponseDto =
            swsOpenSearchClient.doSearch(REQUEST_URI);

        assertNotNull(searchResponseDto);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void testWithExplicitLocalMethodSource(URI uri) {
        var response = swsOpenSearchClient.doSearch(uri);
        assertNotNull(response);
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?name=hello+world&lang=en"),
            URI.create("https://example.com/?q=hello+world&lang=en"),
            URI.create("https://example.com/?q=hello+world&lang=en"));
    }
}