package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.CURATING_INSTITUTIONS;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.KEYWORD;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search.resource.Constants.CONTRIBUTOR_ORG_KEYWORD;
import static no.unit.nva.search.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.resource.ResourceParameter.STATISTICS;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;

import no.unit.nva.search.common.enums.PublicationStatus;
import no.unit.nva.search.common.records.FilterBuilder;

import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.Arrays;

/**
 * ResourceAccessFilter is a class that filters tickets based on access rights.
 *
 * @author Stig Norland
 * @author Sondre Vestad
 * @author Lars-Olav Vågene
 * @see <a
 *     href="https://github.com/BIBSYSDEV/nva-identity-service/blob/main/user-access-handlers/src/main/java/no/unit/nva/handlers/data/DefaultRoleSource.java">DefaultRoleSource</a>
 *     Definitions of roles
 * @see <a href="https://sikt.atlassian.net/browse/NP-47357">NP-47357</a> Role definitions
 */
public class ResourceAccessFilter implements FilterBuilder<ResourceSearchQuery> {

    private static final String CURATING_INST_KEYWORD = CURATING_INSTITUTIONS + DOT + KEYWORD;
    private static final String EDITOR_CURATOR_FILTER = "EditorCuratorFilter";
    private static final String EDITOR_FILTER = "EditorFilter";
    private static final String CURATOR_FILTER = "CuratorFilter";

    private final ResourceSearchQuery searchQuery;

    public ResourceAccessFilter(ResourceSearchQuery query) {
        this.searchQuery = query;
        this.searchQuery.filters().set();
    }

    private static URI getCurationInstitutionId(RequestInfo requestInfo)
            throws UnauthorizedException {
        return requestInfo.getTopLevelOrgCristinId().isPresent()
                ? requestInfo.getTopLevelOrgCristinId().get()
                : requestInfo.getPersonAffiliation();
    }

    @Override
    public ResourceSearchQuery apply() {
        return searchQuery;
    }

    @Override
    public ResourceSearchQuery fromRequestInfo(RequestInfo requestInfo)
            throws UnauthorizedException {

        return customerCurationInstitutions(requestInfo)
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .apply();
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
     * @return {@link ResourceAccessFilter} (builder pattern)
     */
    public ResourceAccessFilter requiredStatus(PublicationStatus... publicationStatus) {
        final var values =
                Arrays.stream(publicationStatus)
                        .filter(this::isStatusAllowed)
                        .map(PublicationStatus::toString)
                        .toArray(String[]::new);
        final var filter = new TermsQueryBuilder(STATUS_KEYWORD, values).queryName(STATUS);
        this.searchQuery.filters().add(filter);
        return this;
    }

    /**
     * Filter on organization and curationInstitutions.
     *
     * <p>Only documents belonging to organization specified are searchable (for the user)
     *
     * @param requestInfo fetches TopLevelOrgCristinId PersonAffiliation
     * @return {@link ResourceAccessFilter} (builder pattern)
     */
    public ResourceAccessFilter customerCurationInstitutions(RequestInfo requestInfo)
            throws UnauthorizedException {
        if (isCurator() && isStatisticsQuery()) {
            return this;
        }
        final var filter =
                QueryBuilders.boolQuery().minimumShouldMatch(1).queryName(EDITOR_CURATOR_FILTER);
        var curationInstitutionId = getCurationInstitutionId(requestInfo).toString();
        if (isCurator()) {
            filter.should(getCuratingInstitutionAccessFilter(curationInstitutionId));
            filter.should(getContributingOrganisationAccessFilter(curationInstitutionId));
        } else if (isEditor()) {
            filter.should(getContributingOrganisationAccessFilter(curationInstitutionId));
        }
        if (!filter.hasClauses()) {
            throw new UnauthorizedException();
        }
        this.searchQuery.filters().add(filter);
        return this;
    }

    private QueryBuilder getContributingOrganisationAccessFilter(String institutionId) {
        return QueryBuilders.termQuery(CONTRIBUTOR_ORG_KEYWORD, institutionId)
                .queryName(EDITOR_FILTER);
    }

    private QueryBuilder getCuratingInstitutionAccessFilter(String institutionId) {
        return QueryBuilders.termQuery(CURATING_INST_KEYWORD, institutionId)
                .queryName(CURATOR_FILTER);
    }

    /**
     * Only Editors are allowed to see UNPUBLISHED publications
     *
     * @param publicationStatus status to check
     * @return true if allowed
     */
    private boolean isStatusAllowed(PublicationStatus publicationStatus) {
        return isEditor() || publicationStatus != UNPUBLISHED;
    }

    private boolean isStatisticsQuery() {
        return searchQuery.parameters().isPresent(STATISTICS);
    }

    private boolean isCurator() {
        return searchQuery.hasAccessRights(MANAGE_CUSTOMERS);
    }

    private boolean isEditor() {
        return searchQuery.hasAccessRights(MANAGE_RESOURCES_ALL);
    }
}
