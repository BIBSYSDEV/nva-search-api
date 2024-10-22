package nva.indexingclient.utils;

import org.mockito.ArgumentMatcher;
import org.opensearch.client.RequestOptions;

public class RequestOptionsHeaderMatcher implements ArgumentMatcher<RequestOptions> {

    private final RequestOptions sourceRequestOptions;

    public RequestOptionsHeaderMatcher(RequestOptions sourceRequestOptions) {
        this.sourceRequestOptions = sourceRequestOptions;
    }

    @Override
    public boolean matches(RequestOptions requestOptions) {
        var sourceHeaders = sourceRequestOptions.getHeaders();
        var matchingHeaders = requestOptions.getHeaders();

        return sourceHeaders.equals(matchingHeaders);
    }
}
