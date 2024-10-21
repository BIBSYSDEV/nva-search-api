package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.resource.Constants.GLOBAL_EXCLUDED_FIELDS;
import static no.unit.nva.search.resource.ResourceClient.defaultClient;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;

import no.unit.nva.search.resource.ContributorCopyMutator;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;

import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handler for searching resources.
 *
 * <p>Searches for resources in the OpenSearch index.
 */
public class SearchResourceHandler extends ApiGatewayHandler<Void, String> {

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS = "entityDescription.contributors";
    private final ResourceClient opensearchClient;

    @JacocoGenerated
    public SearchResourceHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchResourceHandler(Environment environment, ResourceClient resourceClient) {
        super(Void.class, environment);
        this.opensearchClient = resourceClient;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) {
        // Do nothing
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
            throws BadRequestException {
        return ResourceSearchQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, AGGREGATION, SORT)
                .withAlwaysExcludedFields(getExcludedFields())
                .validate()
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .apply()
                .doSearch(opensearchClient)
                .withMutators(new ContributorCopyMutator())
                .toString();
    }

    private List<String> getExcludedFields() {
        return Stream.of(GLOBAL_EXCLUDED_FIELDS, List.of(ENTITY_DESCRIPTION_CONTRIBUTORS))
                .flatMap(Collection::stream)
                .toList();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }
}
