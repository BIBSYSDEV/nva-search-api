package no.unit.nva.search;

import no.unit.nva.search.exception.InputException;
import no.unit.nva.search.exception.SearchException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtil {

    private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);

    public static final String SEARCH_TERM_KEY = "query";
    private static final String MISSING_MANDATORY_PARAMETER = "Missing mandatory parameter '%s'";

    /**
     * Get searchTerm from request query parameters.
     * @param requestInfo requestInfo
     * @return searchTerm given in query parameter
     * @throws ApiGatewayException exception containing explanatory message when parameter missing or inaccessible
     */
    public static String getSearchTerm(RequestInfo requestInfo) throws ApiGatewayException {

        String searchTerm = null;
        try {
            logger.info("Trying to read query parameter {} containing searchTerm...", SEARCH_TERM_KEY);
            if (requestInfo.getQueryParameters().containsKey(SEARCH_TERM_KEY)) {
                searchTerm = requestInfo.getQueryParameters().get(SEARCH_TERM_KEY);
                logger.info("Got query searchTerm : {}", searchTerm);
                return searchTerm;
            } else {
                throw new InputException(String.format(MISSING_MANDATORY_PARAMETER,SEARCH_TERM_KEY));
            }
        } catch (Exception e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

}
