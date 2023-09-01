package no.unit.nva.search2;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Path;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.common.OpenSearchResponseDto;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.URI;
import java.util.stream.Stream;
import static no.unit.nva.search2.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.SwsOpenSearchClient.prepareWithSecretReader;
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
    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";

    private RawContentRetriever contentRetriever;

    @BeforeEach
    public void setUp() {
        contentRetriever = mock(RawContentRetriever.class);
        swsOpenSearchClient = new SwsOpenSearchClient(contentRetriever, MEDIA_TYPE);
    }

    @Test
    void constructorWithSecretsReaderDefinedShouldCreateInstance() {
        var secretsReaderMock = mock(SecretsReader.class);
        var testCredentials = new UsernamePasswordWrapper("user", "password");
        when(secretsReaderMock.fetchClassSecret(anyString(), eq(UsernamePasswordWrapper.class)))
            .thenReturn(testCredentials);
        var swsOpenSearchClient = prepareWithSecretReader(secretsReaderMock);
        assertNotNull(swsOpenSearchClient);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsResponse(URI uri) throws IOException {
        var swsOpenSearchClient = mock(SwsOpenSearchClient.class);
        var jsonResponse = stringFromResources(Path.of(NO_HITS_RESPONSE_JSON));
        var typeReference = new TypeReference<OpenSearchResponseDto>() { };
        var response = objectMapperWithEmpty.readValue(jsonResponse,typeReference);

        when(swsOpenSearchClient.doSearch(any())).thenReturn(response.toSearchResponseDto(uri));

        var searchResponseDto =
            swsOpenSearchClient.doSearch(uri);

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