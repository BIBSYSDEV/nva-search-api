package no.unit.nva.indexing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static no.unit.nva.indexing.handlers.InitHandler.FAILED;
import static no.unit.nva.indexing.handlers.InitHandler.SUCCESS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImportCandidateInitHandlerTest {

    private ImportCandidateInitHandler initHandler;
    private IndexingClient indexingClient;
    private Context context;

    @BeforeEach
    void init() {
        indexingClient = mock(IndexingClient.class);
        initHandler = new ImportCandidateInitHandler(indexingClient);
        context = mock(Context.class);
    }

    @Test
    void shouldNotThrowExceptionIfIndicesClientDoesNotThrowException() throws IOException {
        doNothing().when(indexingClient).createIndex(any(String.class));
        var response = initHandler.handleRequest(null, context);
        assertEquals(response, SUCCESS);
    }

    @Test
    void shouldLogWarningAndReturnFailedWhenIndexingClientFailedToCreateIndex() throws IOException {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        String expectedMessage = randomString();
        when(indexingClient.createIndex(Mockito.anyString(), Mockito.anyMap())).thenThrow(
                new IOException(expectedMessage));
        var response = initHandler.handleRequest(null, context);
        assertEquals(FAILED, response);

        assertThat(logger.getMessages(), containsString(expectedMessage));
    }
}
