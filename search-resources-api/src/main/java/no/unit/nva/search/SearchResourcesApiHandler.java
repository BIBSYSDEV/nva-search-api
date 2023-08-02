package no.unit.nva.search;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.search.RequestUtil.toQuery;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.Callables;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import no.unit.nva.search.model.PersonPreferencesResponse;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.utils.UriRetriever;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.RestRequestHandler;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.HttpStatus;

public class SearchResourcesApiHandler extends ApiGatewayHandler<Void, String> {

    public static final String CONTRIBUTOR_ID = "entityDescription.contributors.identity.id";
    public static final String CONTENT_TYPE = "application/json";
    private final SearchClient openSearchClient;
    private final UriRetriever uriRetriever;

    @JacocoGenerated
    public SearchResourcesApiHandler() {
        this(new Environment(), defaultSearchClient(), new UriRetriever());
    }

    public SearchResourcesApiHandler(Environment environment, SearchClient openSearchClient,
                                     UriRetriever uriRetriever) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.openSearchClient = openSearchClient;
        this.uriRetriever = uriRetriever;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(MediaType.JSON_UTF_8, MediaType.CSV_UTF_8, MediaType.ANY_TEXT_TYPE);
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by
     * {@link RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var query = toQuery(requestInfo, RESOURCES_AGGREGATIONS);
        if (containsSingleContributorIdOnly(query)) {
            var contributorId = extractId(query);
            var promotedPublications =
                attempt(() -> fetchPromotedPublications(contributorId)).or(Callables.returning(List.of())).get();
            var searchResponse = openSearchClient.searchPromotedQuery(contributorId, promotedPublications,
                                                                      query, OPENSEARCH_ENDPOINT_INDEX);
            return createResponse(requestInfo, searchResponse);
        }
        var searchResponse = openSearchClient.searchWithSearchDocumentQuery(query, OPENSEARCH_ENDPOINT_INDEX);
        return createResponse(requestInfo, searchResponse);
    }

    private boolean containsSingleContributorIdOnly(SearchDocumentsQuery query) {
        return query.getSearchTerm().split(CONTRIBUTOR_ID).length == 2 && !query.getSearchTerm().contains("AND");
    }

    /**
     * Define the success status code.
     *
     * @param input  The request input.
     * @param output The response output
     * @return the success status code.
     */
    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpStatus.SC_OK;
    }

    private List<String> fetchPromotedPublications(String contributorId) throws IOException, InterruptedException {
        var uri = UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                      .addChild("person-preferences")
                      .addChild(URLEncoder.encode(contributorId, StandardCharsets.UTF_8))
                      .getUri();
        var response = uriRetriever.getRawContent(uri, CONTENT_TYPE);
        return dtoObjectMapper.readValue(response, PersonPreferencesResponse.class).getPromotedPublications();
    }

    private String extractId(SearchDocumentsQuery query) {
        return query.getSearchTerm().split(CONTRIBUTOR_ID)[1].replaceAll("[:()\"]", StringUtils.EMPTY_STRING)
                   .split("&")[0];
    }

    private String createResponse(RequestInfo requestInfo, SearchResponseDto searchResponse)
        throws UnsupportedAcceptHeaderException {
        var contentType = getDefaultResponseContentTypeHeaderValue(requestInfo);

        if (MediaType.ANY_TEXT_TYPE.type().equalsIgnoreCase(contentType.type())) {
            return CsvTransformer.transform(searchResponse);
        } else {
            return attempt(() -> dtoObjectMapper.writeValueAsString(searchResponse)).orElseThrow();
        }
    }
}
