package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.search.RequestUtil.toQueryTickets;
import static no.unit.nva.search.RequestUtil.toQueryTicketsWithViewingScope;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.STATUS_TERMS_AGGREGATION;
import static no.unit.nva.search.constants.ApplicationConstants.TICKETS_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.TYPE_TERMS_AGGREGATION;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.SearchTicketsQuery;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchTicketsHandler extends ApiGatewayHandler<Void, SearchResponseDto> {

    public static final String ROLE_CURATOR = "curator";
    public static final String PARAM_ROLE = "role";
    public static final String ROLE_CREATOR = "creator";
    private static final Logger logger = LoggerFactory.getLogger(SearchTicketsHandler.class);
    public static final String APPLICATION_JSON = "application/json";
    private static final String HAS_PART_PROPERTY = "https://nva.sikt.no/ontology/publication#hasPart";
    private static final CharSequence COMMA_AND_SPACE = ", ";
    public static final String USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_VIEWINGSCOPE
        = "User of %s is not allowed to search viewingScope: %s";
    private final SearchClient searchClient;
    private final AuthorizedBackendUriRetriever uriRetriever;

    @JacocoGenerated
    public SearchTicketsHandler() {
        this(new Environment(), defaultSearchClient(), defaultUriRetriver());
    }

    public SearchTicketsHandler(Environment environment,
                                SearchClient searchClient,
                                AuthorizedBackendUriRetriever uriRetriever) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.searchClient = searchClient;
        this.uriRetriever = uriRetriever;
    }

    @Override
    protected SearchResponseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var indexName = getIndexName(requestInfo);
        logger.info("Index name: {}", indexName);
        return handleSearch(requestInfo, indexName);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponseDto output) {
        return HTTP_OK;
    }

    private static BoolQueryBuilder notInFilter(RequestInfo requestInfo, String owner, String mustField,
                                                String notInField) {
        return new BoolQueryBuilder()
                   .must(QueryBuilders.queryStringQuery(RequestUtil.getSearchTerm(requestInfo)))
                   .must(QueryBuilders.matchQuery(mustField, owner).operator(Operator.AND))
                   .mustNot(QueryBuilders.matchQuery(notInField, owner).operator(Operator.AND));
    }

    private static boolean userHasAccessRights(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(MANAGE_DOI);
    }

    private SearchResponseDto handleSearch(RequestInfo requestInfo, String indexName)
        throws ApiGatewayException {
        var role = requestInfo.getQueryParameterOpt(PARAM_ROLE).orElse(ROLE_CURATOR);
        if (ROLE_CURATOR.equals(role)) {
            assertCuratorHasAppropriateAccessRights(requestInfo);
            return handleCuratorSearch(requestInfo, indexName);
        } else {
            return handleCreatorSearch(requestInfo, indexName);
        }
    }

    private SearchResponseDto handleCreatorSearch(RequestInfo requestInfo, String indexName)
        throws UnauthorizedException, BadGatewayException, ForbiddenException {
        final var owner = requestInfo.getUserName();
        logger.info("OwnerScope: {}", owner);
        var unread = new FilterAggregationBuilder(
            "unread",
            notInFilter(requestInfo, owner, "owner", "viewedBy"));
        var aggregations = List.of(
            TYPE_TERMS_AGGREGATION, STATUS_TERMS_AGGREGATION, unread);
        var query = toQueryTickets(requestInfo, aggregations);
        assertUserIsAllowedViewingScope(requestInfo.getTopLevelOrgCristinId().orElseThrow(), query);
        return searchClient.searchOwnerTickets(query, owner, indexName);
    }

    private SearchResponseDto handleCuratorSearch(RequestInfo requestInfo, String indexName)
        throws ApiGatewayException {
        var query = toQueryTicketsWithViewingScope(requestInfo, TICKETS_AGGREGATIONS);
        assertUserIsAllowedViewingScope(requestInfo.getTopLevelOrgCristinId().orElseThrow(), query);
        return searchClient.searchWithSearchTicketQuery(query, indexName);
    }

    private void assertUserIsAllowedViewingScope(URI topLevelOrg, SearchTicketsQuery query)
        throws ForbiddenException {
        var allowed = attempt(() -> this.uriRetriever.getRawContent(topLevelOrg,
                                                                    APPLICATION_JSON)).map(
                Optional::orElseThrow)
                          .map(document -> createModel(dtoObjectMapper.readTree(document)))
                          .map(model -> model.listObjectsOfProperty(model.createProperty(HAS_PART_PROPERTY)))
                          .map(node -> node.toList().stream().map(RDFNode::toString))
                          .map(hasPartOrgs -> Stream.concat(hasPartOrgs, Stream.of(topLevelOrg.toString())))
                          .orElseThrow()
                          .collect(Collectors.toSet());

        var illegal = Sets.difference(new HashSet<>(new HashSet<>(query.getViewingScope())), allowed);

        if (!illegal.isEmpty()) {
            logger.info(String.format(USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_VIEWINGSCOPE,
                                      topLevelOrg.toString(),
                                      illegal.stream().collect(Collectors.joining(
                                          COMMA_AND_SPACE.toString()))));
            throw new ForbiddenException();
        }
    }

    private void assertCuratorHasAppropriateAccessRights(RequestInfo requestInfo)
        throws ForbiddenException {
        if (!userHasAccessRights(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private String getIndexName(RequestInfo requestInfo) {
        String requestPath = RequestUtil.getRequestPath(requestInfo);
        return UnixPath.of(requestPath).getLastPathElement();
    }

    public static Model createModel(JsonNode body) {
        var model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, stringToStream(body.toString()), Lang.JSONLD);
        } catch (RiotException e) {
            logger.warn(e.getMessage());
        }

        return model;
    }

    @JacocoGenerated
    private static AuthorizedBackendUriRetriever defaultUriRetriver() {
        return new AuthorizedBackendUriRetriever(new Environment().readEnv("COGNITO_HOST"),
                                                 new Environment().readEnv("BACKEND_CLIENT_SECRET_NAME"));
    }
}
