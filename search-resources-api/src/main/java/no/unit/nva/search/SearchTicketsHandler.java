package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.function.Predicate.isEqual;
import static no.unit.nva.search.RequestUtil.toQueryTickets;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;

import java.net.URI;
import java.util.Optional;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search.models.SearchResponseDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchTicketsHandler extends ApiGatewayHandler<Void, SearchResponseDto> {

    public static final String VIEWING_SCOPE_QUERY_PARAMETER = "viewingScope";
    public static final String CRISTIN_ORG_LEVEL_DELIMITER = "\\.";
    public static final int HIGHEST_LEVEL_ORGANIZATION = 0;
    public static final String EXPECTED_ACCESS_RIGHT_FOR_VIEWING_MESSAGES_AND_DOI_REQUESTS = "APPROVE_DOI_REQUEST";
    private static final Logger logger = LoggerFactory.getLogger(SearchTicketsHandler.class);
    private final SearchClient searchClient;
    private final IdentityClient identityClient;

    @JacocoGenerated
    public SearchTicketsHandler() {
        this(new Environment(), defaultSearchClient(), defaultIdentityClient());
    }

    public SearchTicketsHandler(Environment environment, SearchClient searchClient, IdentityClient identityClient) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.searchClient = searchClient;
        this.identityClient = identityClient;
    }

    @Override
    protected SearchResponseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var indexName = getIndexName(requestInfo);
        logger.info("Index name: {}", indexName);
        assertUserHasAppropriateAccessRights(requestInfo);
        ViewingScope viewingScope = getViewingScopeForUser(requestInfo);
        logger.info("ViewingScope: {}", attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(viewingScope))
            .orElseThrow());

        var searchResponse = searchClient.findTicketsForOrganizationIds(viewingScope,
                                                                        toQueryTickets(requestInfo),
                                                                        indexName);
        URI requestUri = RequestUtil.getRequestUri(requestInfo);
        return SearchResponseDto.fromSearchResponse(searchResponse, requestUri);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponseDto output) {
        return HTTP_OK;
    }

    @JacocoGenerated
    private static IdentityClient defaultIdentityClient() {
        return new IdentityClientImpl();
    }

    private void assertUserHasAppropriateAccessRights(RequestInfo requestInfo) throws ForbiddenException {
        if (!requestInfo.userIsAuthorized(EXPECTED_ACCESS_RIGHT_FOR_VIEWING_MESSAGES_AND_DOI_REQUESTS)) {
            throw new ForbiddenException();
        }
    }

    private ViewingScope getViewingScopeForUser(RequestInfo requestInfo) throws ApiGatewayException {
        var defaultScope = getUserDefinedViewingScore(requestInfo);
        if (defaultScope.isPresent()) {
            var json = attempt(() ->
                                   JsonUtils.dtoObjectMapper.writeValueAsString(
                                       defaultScope.orElseThrow())).orElseThrow();
            logger.info("Orestis, ViewingScope defined by user:{}", json);
        } else {
            logger.info("Orestis, Viewing scope not defined by user.");
        }
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

    //This is quick fix for implementing authorization. It is based on the assumption that
    // all Organizations have a common prefix in their Cristin Ids.
    //TODO: When the Cristin proxy is mature and quick, we should query the Cristin proxy in
    // order to avoid using semantically charged identifiers.
    private ViewingScope authorizeCustomViewingScope(ViewingScope viewingScope, RequestInfo requestInfo)
        throws ForbiddenException {
        var customerCristinId = requestInfo.getTopLevelOrgCristinId().orElseThrow();
        logger.info("customerCristinId: {}", customerCristinId);
        return userIsAuthorized(viewingScope, customerCristinId);
    }

    private Try<ViewingScope> defaultViewingScope(RequestInfo requestInfo) {
        return attempt(requestInfo::getUserName)
            .map(nvaUsername -> identityClient.getUser(nvaUsername, requestInfo.getAuthHeader()))
            .map(Optional::orElseThrow)
            .map(UserResponse::getViewingScope);
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
        logger.info("viewingScope for requeest institution: {}", requestedOrg);
        logger.info("viewingScope for institution: {}", customerCristinId);
        String requestedOrgInstitutionNumber = extractInstitutionNumberFromRequestedOrganization(requestedOrg);
        String customerCristinInstitutionNumber = extractInstitutionNumberFromRequestedOrganization(customerCristinId);
        return customerCristinInstitutionNumber.equals(requestedOrgInstitutionNumber);
    }

    private String extractInstitutionNumberFromRequestedOrganization(URI requestedOrg) {
        String requestedOrgCristinIdentifier = UriWrapper.fromUri(requestedOrg).getLastPathElement();
        return requestedOrgCristinIdentifier.split(CRISTIN_ORG_LEVEL_DELIMITER)[HIGHEST_LEVEL_ORGANIZATION];
    }

    private String getIndexName(RequestInfo requestInfo) {
        String requestPath = RequestUtil.getRequestPath(requestInfo);
        return UnixPath.of(requestPath).getLastPathElement();
    }
}
