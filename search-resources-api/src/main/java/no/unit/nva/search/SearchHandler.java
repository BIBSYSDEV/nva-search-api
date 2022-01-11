package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search.restclients.IdentityClient;
import no.unit.nva.search.restclients.IdentityClientImpl;
import no.unit.nva.search.restclients.responses.UserResponse;
import no.unit.nva.search.restclients.responses.ViewingScope;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.elasticsearch.action.search.SearchResponse;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.function.Predicate.isEqual;
import static no.unit.nva.search.SearchClientConfig.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;

public class SearchHandler extends ApiGatewayHandler<Void, JsonNode> {

    public static final String VIEWING_SCOPE_QUERY_PARAMETER = "viewingScope";
    public static final String CRISTIN_ORG_LEVEL_DELIMITER = "\\.";
    public static final int HIGHEST_LEVEL_ORGANIZATION = 0;
    public static final String EXPECTED_ACCESS_RIGHT_FOR_VIEWING_MESSAGES_AND_DOI_REQUESTS = "APPROVE_DOI_REQUEST";
    private final SearchClient searchClient;
    private final IdentityClient identityClient;

    @JacocoGenerated
    public SearchHandler() {
        this(new Environment(), defaultSearchClient(), defaultIdentityClient());
    }

    public SearchHandler(Environment environment, SearchClient searchClient, IdentityClient identityClient) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.searchClient = searchClient;
        this.identityClient = identityClient;
    }

    @Override
    protected JsonNode processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        String indexName = getIndexName(requestInfo);
        assertUserHasAppropriateAccessRights(requestInfo);
        ViewingScope viewingScope = getViewingScopeForUser(requestInfo);
        SearchResponse searchResponse = searchClient.findResourcesForOrganizationIds(indexName, viewingScope);
        return toJsonNode(searchResponse);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, JsonNode output) {
        return HTTP_OK;
    }

    @JacocoGenerated
    private static IdentityClient defaultIdentityClient() {
        return new IdentityClientImpl();
    }

    private void assertUserHasAppropriateAccessRights(RequestInfo requestInfo) throws ForbiddenException {
        Set<String> accessRights = requestInfo.getAccessRights();
        if (!accessRights.contains(EXPECTED_ACCESS_RIGHT_FOR_VIEWING_MESSAGES_AND_DOI_REQUESTS)) {
            throw new ForbiddenException();
        }
    }

    private JsonNode toJsonNode(SearchResponse searchResponse) {
        return attempt(() -> objectMapperWithEmpty.readTree(searchResponse.toString())).orElseThrow();
    }

    private ViewingScope getViewingScopeForUser(RequestInfo requestInfo) throws ApiGatewayException {
        var defaultScope = getUserDefinedViewingScore(requestInfo);
        return defaultScope
            .map(attempt(viewingScope -> authorizeCustomViewingScope(viewingScope, requestInfo)))
            .orElseGet(() -> defaultViewingScope(requestInfo))
            .orElseThrow(failure -> handleFailure(failure.getException()));
    }

    private ApiGatewayException handleFailure(Exception exception) {
        if (exception instanceof ForbiddenException) {
            return (ForbiddenException) exception;
        }
        throw new RuntimeException(exception);
    }

    //This is quick fic for implementing authorization. It is based on the assumption that
    // all Organizations have a common prefix in their Cristin Ids.
    //TODO: When the Cristin proxy is mature and quick, we should query the Cristin proxy in
    // order to avoid using semantically charged identifiers.
    private ViewingScope authorizeCustomViewingScope(ViewingScope viewingScope, RequestInfo requestInfo)
        throws ForbiddenException {
        var customerCristinId = requestInfo.getCustomerCristinId().orElseThrow();
        return userIsAuthorized(viewingScope, customerCristinId);
    }

    private Try<ViewingScope> defaultViewingScope(RequestInfo requestInfo) {
        String username = requestInfo.getFeideId().orElseThrow();
        UserResponse userResponse = identityClient.getUser(username).orElseThrow();
        return Try.of(userResponse.getViewingScope());
    }

    private Optional<ViewingScope> getUserDefinedViewingScore(RequestInfo requestInfo) {
        return requestInfo.getQueryParameterOpt(VIEWING_SCOPE_QUERY_PARAMETER)
            .map(URI::create)
            .map(ViewingScope::create);
    }

    private ViewingScope userIsAuthorized(ViewingScope viewingScope, URI customerCristinId) throws ForbiddenException {
        if (allIncludedUnitsAreLegal(viewingScope, customerCristinId)) {
            return viewingScope;
        }
        throw new ForbiddenException();
    }

    private boolean allIncludedUnitsAreLegal(ViewingScope viewingScope, URI customerCristinId) {
        return viewingScope.getIncludedUnits().stream()
            .map(requestedOrg -> isUnderUsersInstitution(requestedOrg, customerCristinId))
            .allMatch(isEqual(true));
    }

    private boolean isUnderUsersInstitution(URI requestedOrg, URI customerCristinId) {
        String requestedOrgInstitutionNumber = extractInstitutionNumberFromRequestedOrganization(requestedOrg);
        String customerCristinInstitutionNumber = extractInstitutionNumberFromRequestedOrganization(customerCristinId);
        return customerCristinInstitutionNumber.equals(requestedOrgInstitutionNumber);
    }

    private String extractInstitutionNumberFromRequestedOrganization(URI requestedOrg) {
        String requestedOrgCristinIdentifier = new UriWrapper(requestedOrg).getFilename();
        return requestedOrgCristinIdentifier.split(CRISTIN_ORG_LEVEL_DELIMITER)[HIGHEST_LEVEL_ORGANIZATION];
    }

    private String getIndexName(RequestInfo requestInfo) {
        return UnixPath.of(requestInfo.getPath()).getFilename();
    }
}
