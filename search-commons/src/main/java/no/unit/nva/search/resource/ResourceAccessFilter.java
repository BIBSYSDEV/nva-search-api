package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.AUTHORIZATION;
import static no.unit.nva.constants.Words.OWNER;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.search.common.enums.PublicationStatus.DELETED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search.resource.Constants.CONTRIBUTOR_ORG_KEYWORD;
import static no.unit.nva.search.resource.Constants.RESOURCE_OWNER_OWNER_KEYWORD;
import static no.unit.nva.search.resource.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.resource.ResourceParameter.STATISTICS;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.search.common.enums.PublicationStatus;
import no.unit.nva.search.common.records.FilterBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final String CONTRIBUTOR_ORGANIZATION_FILTER = "ContributorOrganizationFilter";

  protected static final Logger logger = LoggerFactory.getLogger(ResourceAccessFilter.class);
  private final ResourceSearchQuery searchQuery;

  public ResourceAccessFilter(ResourceSearchQuery query) {
    this.searchQuery = query;
    this.searchQuery.filters().set();
  }

  public ResourceAccessFilter ownResourcesOnly(String username) {
    this.searchQuery
        .filters()
        .add(new TermsQueryBuilder(RESOURCE_OWNER_OWNER_KEYWORD, username).queryName(OWNER));
    return this;
  }

  private static URI getInstitutionId(RequestInfo requestInfo) throws UnauthorizedException {
    return requestInfo.getTopLevelOrgCristinId().isPresent()
        ? requestInfo.getTopLevelOrgCristinId().get()
        : requestInfo.getPersonAffiliation();
  }

  @Override
  public ResourceSearchQuery apply() {
    return searchQuery;
  }

  @Override
  public ResourceSearchQuery fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
    if (isAuthorized(requestInfo)) {
      return customerContributorOrganizations(requestInfo).apply();
    } else {
      return requiredStatus(PUBLISHED).apply();
    }
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
        Arrays.stream(publicationStatus).map(PublicationStatus::toString).toArray(String[]::new);
    this.searchQuery.filters().add(new TermsQueryBuilder(STATUS_KEYWORD, values).queryName(STATUS));
    return this;
  }

  /**
   * Filter on contributor organizations.
   *
   * <p>Only documents belonging to organization specified are searchable (for the user)
   *
   * @param requestInfo fetches TopLevelOrgCristinId PersonAffiliation
   * @return {@link ResourceAccessFilter} (builder pattern)
   */
  public ResourceAccessFilter customerContributorOrganizations(RequestInfo requestInfo)
      throws UnauthorizedException {
    if (isAppAdmin() && isStatisticsQuery()) {
      return this;
    }

    final var statuses =
        Stream.of(PUBLISHED, DELETED, UNPUBLISHED)
            .filter(this::isStatusAllowed)
            .toArray(PublicationStatus[]::new);

    requiredStatus(statuses);

    var institutionId = getInstitutionId(requestInfo).toString();
    if (isCurator() || isEditor()) {
      this.searchQuery.filters().add(filterByContributingOrg(institutionId));
    }
    if (statusOrOrgfilterIsMissing() && !isAppAdmin()) {
      throw new UnauthorizedException();
    }
    return this;
  }

  private boolean statusOrOrgfilterIsMissing() {
    return this.searchQuery.filters().size() < 2;
  }

  private QueryBuilder filterByContributingOrg(String institutionId) {
    return new TermsQueryBuilder(CONTRIBUTOR_ORG_KEYWORD, institutionId)
        .queryName(CONTRIBUTOR_ORGANIZATION_FILTER);
  }

  private boolean isAuthorized(RequestInfo requestInfo) {
    return requestInfo.getHeaders().containsKey(AUTHORIZATION);
  }

  /**
   * Only Editors are allowed to see UNPUBLISHED publications
   *
   * @param publicationStatus status to check
   * @return true if allowed
   */
  private boolean isStatusAllowed(PublicationStatus publicationStatus) {
    return isAppAdmin()
        || (isEditor() && publicationStatus == DELETED)
        || publicationStatus == PUBLISHED
        || publicationStatus == UNPUBLISHED;
  }

  private boolean isStatisticsQuery() {
    return searchQuery.parameters().isPresent(STATISTICS);
  }

  private boolean isAppAdmin() {
    return searchQuery.hasAccessRights(MANAGE_CUSTOMERS);
  }

  private boolean isCurator() {
    return searchQuery.hasAccessRights(MANAGE_RESOURCES_STANDARD);
  }

  private boolean isEditor() {
    return searchQuery.hasAccessRights(MANAGE_RESOURCES_ALL);
  }
}
