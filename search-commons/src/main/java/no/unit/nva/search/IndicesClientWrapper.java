package no.unit.nva.search;

import nva.commons.core.JacocoGenerated;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.IndicesClient;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.GetIndexResponse;

import java.io.IOException;

/**
 * Wrapper class for being able to test calls to the final class IndicesClient.
 */
@JacocoGenerated
public class IndicesClientWrapper {

    private final IndicesClient indicesClient;

    public IndicesClientWrapper(IndicesClient indices) {
        this.indicesClient = indices;
    }

    /**
     * Do not use this method. This method is only for experimenting. If you want to use a method of {@link
     * IndicesClient} replicate the method in {@link IndicesClientWrapper} and call the respective
     * {@link IndicesClient} one.
     *
     * @return the contained client.
     */
    public IndicesClient getIndicesClient() {
        return indicesClient;
    }

    public CreateIndexResponse create(CreateIndexRequest createIndexRequest, RequestOptions requestOptions)
            throws IOException {
        return indicesClient.create(createIndexRequest, requestOptions);
    }

    public AcknowledgedResponse putSettings(UpdateSettingsRequest updateSettingsRequest, RequestOptions requestOptions)
            throws IOException {
        return indicesClient.putSettings(updateSettingsRequest, requestOptions);
    }

    public GetIndexResponse get(GetIndexRequest getIndexRequest, RequestOptions requestOptions) throws IOException {
        return indicesClient.get(getIndexRequest, requestOptions);
    }

    public AcknowledgedResponse delete(DeleteIndexRequest deleteIndexRequest, RequestOptions requestOptions)
            throws IOException {
        return indicesClient.delete(deleteIndexRequest, requestOptions);
    }
}
