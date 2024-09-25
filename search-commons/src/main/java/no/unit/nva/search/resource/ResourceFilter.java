package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.CURATING_INSTITUTIONS;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.KEYWORD;
import static no.unit.nva.constants.Words.ORGANIZATION;
import static no.unit.nva.constants.Words.PUBLISHER;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.resource.Constants.PUBLISHER_ID_KEYWORD;
import static no.unit.nva.search.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.resource.ResourceParameter.STATISTICS;

import no.unit.nva.search.common.enums.PublicationStatus;
import no.unit.nva.search.common.records.FilterBuilder;

import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.Arrays;

/**
 * @author Stig Norland
 * @author Sondre Vestad
 */
public class ResourceFilter implements FilterBuilder<ResourceSearchQuery> {

    public static final String CURATING_INST_KEYWORD = CURATING_INSTITUTIONS + DOT + KEYWORD;
    private final ResourceSearchQuery resourceSearchQuery;

    public ResourceFilter(ResourceSearchQuery query) {
        this.resourceSearchQuery = query;
        this.resourceSearchQuery.filters.set();
    }

    @Override
    public ResourceSearchQuery apply() {
        return resourceSearchQuery;
    }

    @Override
    public ResourceSearchQuery fromRequestInfo(RequestInfo requestInfo)
            throws UnauthorizedException {
        final var organization =
                requestInfo.getTopLevelOrgCristinId().orElse(requestInfo.getPersonAffiliation());

        return organization(organization).requiredStatus(PUBLISHED, PUBLISHED_METADATA).apply();
    }

    /**
     * Filter on Required Status.
     *
     * <p>Only STATUES specified here will be available for the Query.
     *
     * <p>This is to avoid the Query to return documents that are not available for the user.
     *
     * <p>See {@link PublicationStatus} for available values.
     *
     * @param publicationStatus the required statues
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter requiredStatus(PublicationStatus... publicationStatus) {
        final var values =
                Arrays.stream(publicationStatus)
                        .map(PublicationStatus::toString)
                        .toArray(String[]::new);
        final var filter = new TermsQueryBuilder(STATUS_KEYWORD, values).queryName(STATUS);
        this.resourceSearchQuery.filters.add(filter);
        return this;
    }

    /**
     * Filter on organization.
     *
     * <p>Only documents belonging to organization specified are searchable (for the user)
     *
     * @param requestInfo fetches getCurrentCustomer
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter organization(RequestInfo requestInfo) throws UnauthorizedException {
        if (isSearchingForAllPublications(requestInfo)) {
            return this;
        } else {
            return organization(requestInfo.getCurrentCustomer());
        }
    }

    /**
     * Filter on organization and curationInstitutions.
     *
     * <p>Only documents belonging to organization specified are searchable (for the user)
     *
     * @param requestInfo fetches getCurrentCustomer
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter organizationCurationInstitutions(RequestInfo requestInfo)
            throws UnauthorizedException {
        if (isSearchingForAllPublications(requestInfo)) {
            return this;
        } else {
            var customer = requestInfo.getCurrentCustomer();
            var curationInstitution =
                    requestInfo.getTopLevelOrgCristinId().isPresent()
                            ? requestInfo.getTopLevelOrgCristinId().get()
                            : requestInfo.getPersonAffiliation();
            return organizationCurationInstitutions(customer, curationInstitution);
        }
    }

    /**
     * Filter on organization.
     *
     * <p>Only documents belonging to organization specified are searchable (for the user)
     *
     * @param organization the organization
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter organization(URI organization) throws UnauthorizedException {
        final var filter =
                new TermQueryBuilder(PUBLISHER_ID_KEYWORD, organization.toString())
                        .queryName(PUBLISHER);
        this.resourceSearchQuery.filters.add(filter);
        return this;
    }

    /**
     * Filter on organization and curationInstitutions.
     *
     * <p>Only documents belonging to organization specified are searchable (for the user)
     *
     * @param organization the organization
     * @return ResourceQuery (builder pattern)
     */
    public ResourceFilter organizationCurationInstitutions(
            URI organization, URI curationInstitutions) {
        final var filter =
                QueryBuilders.boolQuery()
                        .should(
                                new TermQueryBuilder(PUBLISHER_ID_KEYWORD, organization.toString())
                                        .queryName(PUBLISHER))
                        .should(
                                new TermQueryBuilder(
                                                CURATING_INST_KEYWORD,
                                                curationInstitutions.toString())
                                        .queryName(CURATING_INSTITUTIONS))
                        .minimumShouldMatch(1)
                        .queryName(ORGANIZATION);
        this.resourceSearchQuery.filters.add(filter);
        return this;
    }

    private boolean isSearchingForAllPublications(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.MANAGE_CUSTOMERS)
                && resourceSearchQuery.parameters().isPresent(STATISTICS);
    }
}
