package no.unit.nva.search.ticket;

import static java.util.Objects.isNull;
import static no.unit.nva.search.ticket.Constants.CANNOT_SEARCH_AS_BOTH_ASSIGNEE_AND_OWNER_AT_THE_SAME_TIME;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_NOT_APPLICABLE;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_ORGANIZATION;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_OWNER;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_STATUS;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_TICKET_TYPES;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_UN_PUBLISHED;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_USER_AND_TICKET_TYPES;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_IS_REQUIRED;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_PATHS;
import static no.unit.nva.search.ticket.Constants.STATUS_KEYWORD;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.ticket.Constants.USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES;
import static no.unit.nva.search.ticket.Constants.USER_IS_REQUIRED;
import static no.unit.nva.search.ticket.TicketParameter.ASSIGNEE;
import static no.unit.nva.search.ticket.TicketParameter.ORGANIZATION_ID;
import static no.unit.nva.search.ticket.TicketParameter.OWNER;
import static no.unit.nva.search.ticket.TicketParameter.STATISTICS;
import static no.unit.nva.search.ticket.TicketStatus.NOT_APPLICABLE;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.core.attempt.Try.attempt;
import static org.opensearch.index.query.QueryBuilders.boolQuery;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.search.common.AsType;
import no.unit.nva.search.common.records.FilterBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.ViewingScope;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;

@SuppressWarnings("PMD.GodClass")
public class TicketAccessFilter implements FilterBuilder<TicketSearchQuery> {

  private final TicketSearchQuery query;
  private String currentUser;
  private Set<URI> organizationSet;
  private Set<AccessRight> accessRightEnumSet = EnumSet.noneOf(AccessRight.class);

  public TicketAccessFilter(TicketSearchQuery query) {
    this.query = query;
    this.query.filters().set();
  }

  public String getCurrentUser() {
    return currentUser;
  }

  public Set<TicketType> getAccessRightAsTicketTypes() {
    return accessRightsToTicketTypes(accessRightEnumSet);
  }

  protected boolean hasOrganization(URI organizationId) {
    return organizationSet.contains(organizationId);
  }

  /**
   * Filter on access rights.
   *
   * @param accessRights access rights
   * @return TicketQuery (builder pattern)
   * @apiNote ONLY USE IN TESTS, in handlers use: {@link #fromRequestInfo(RequestInfo)}
   */
  public TicketAccessFilter accessRights(AccessRight... accessRights) {
    return accessRights(List.of(accessRights));
  }

  private TicketAccessFilter accessRights(List<AccessRight> accessRights) {
    this.accessRightEnumSet =
        accessRights.stream()
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AccessRight.class)));
    return this;
  }

  /**
   * Filter on owner (user).
   *
   * @param userName current user
   * @return TicketQuery (builder pattern)
   * @apiNote ONLY USE IN TESTS, in handlers use: {@link #fromRequestInfo(RequestInfo)}
   */
  public TicketAccessFilter user(String userName) {
    this.currentUser = userName;
    return this;
  }

  public TicketAccessFilter organizations(Set<URI> organizationSet) {
    this.organizationSet = organizationSet;
    return this;
  }

  /**
   * Authorize and set 'ViewScope'.
   *
   * <p>Authorize and set filters -> ticketTypes, organization and owner
   *
   * <p>This is to avoid the Query to return documents that are not available for the user.
   *
   * @param requestInfo all required is here
   * @return TicketQuery (builder pattern)
   */
  @Override
  public TicketSearchQuery fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
    var viewingScopes = getViewingScopesToInclude(requestInfo);
    final var organizations = new HashSet<URI>();
    if (viewingScopes.isEmpty()) {
      organizations.add(
          requestInfo.getTopLevelOrgCristinId().orElse(requestInfo.getPersonAffiliation()));
    } else {
      organizations.addAll(viewingScopes.stream().map(URI::create).collect(Collectors.toSet()));
    }
    if (query.parameters().isPresent(ORGANIZATION_ID)) {
      validateAssigneeAndOwnerParameters();
    }

    return user(requestInfo.getUserName())
        .accessRights(requestInfo.getAccessRights())
        .organizations(organizations)
        .apply();
  }

  private static Set<String> getViewingScopesToInclude(RequestInfo requestInfo) {
    return attempt(requestInfo::getViewingScope)
        .map(ViewingScope::includes)
        .orElse(failure -> Set.<String>of());
  }

  @Override
  public TicketSearchQuery apply() throws UnauthorizedException {

    if (searchAsSiktAdmin() && validateSiktAdmin(accessRightEnumSet)) {
      return query; // See everything, NO FILTERS!!!
    }

    validateOrganization();
    validateUser();
    final var curatorTicketTypes = accessRightsToTicketTypes(accessRightEnumSet);

    if (hasNoCuratorRoles(curatorTicketTypes) && searchAsTicketOwner()) {
      validateOwner(currentUser);
    }
    if (searchAsTicketAssignee() && searchAsTicketOwner()) {
      validateAssigneeAndOwnerParameters();
    }

    var filters = query.filters();
    if (!searchAsTicketOwner()) {
      filters.add(filterByOrganization(organizationSet));
    }
    filters
        .add(filterByUserAndTicketTypes(currentUser, curatorTicketTypes))
        .add(filterByDeniedUnpublishRequest())
        .add(filterNotApplicableTickets());
    return query;
  }

  private QueryBuilder filterNotApplicableTickets() {
    return boolQuery()
        .mustNot(
            new TermsQueryBuilder(STATUS_KEYWORD, NOT_APPLICABLE.toString())
                .queryName(FILTER_BY_STATUS))
        .queryName(FILTER_BY_NOT_APPLICABLE);
  }

  /**
   * Apply business rules to determine which ticket types are allowed.
   *
   * <ul>
   *   <li>doi -> DOI_REQUEST
   *   <li>support -> GENERAL_SUPPORT_CASE
   *   <li>publishing -> PUBLISHING_REQUEST
   *   <li>degree -> FILES_APPROVAL_THESIS
   * </ul>
   */
  private Set<TicketType> accessRightsToTicketTypes(Set<AccessRight> accessRights) {
    var allowed = EnumSet.noneOf(TicketType.class);
    if (accessRights.contains(MANAGE_DOI)) {
      allowed.add(TicketType.DOI_REQUEST);
    }
    if (accessRights.contains(AccessRight.SUPPORT)) {
      allowed.add(TicketType.GENERAL_SUPPORT_CASE);
    }
    if (accessRights.contains(MANAGE_PUBLISHING_REQUESTS)) {
      allowed.add(TicketType.PUBLISHING_REQUEST);
    }
    if (accessRights.contains(MANAGE_DEGREE)) {
      allowed.add(TicketType.FILES_APPROVAL_THESIS);
    }
    if (allowed.isEmpty()) {
      allowed.add(TicketType.NONE);
    }
    return allowed;
  }

  private BoolQueryBuilder filterByOrganization(Set<URI> organizationIds) {
    BoolQueryBuilder boolQuery = boolQuery();

    for (URI organizationId : organizationIds) {
      MultiMatchQueryBuilder multiMatchQuery =
          QueryBuilders.multiMatchQuery(organizationId.toString(), ORGANIZATION_PATHS)
              .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
              .operator(Operator.AND)
              .queryName(FILTER_BY_ORGANIZATION);

      boolQuery.should(multiMatchQuery); // At least one match required
    }

    return boolQuery.minimumShouldMatch(1); // Ensures at least one must match
    //    return QueryBuilders.multiMatchQuery(organizationId.toString(), ORGANIZATION_PATHS)
    //        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
    //        .operator(Operator.AND)
    //        .queryName(FILTER_BY_ORGANIZATION);
  }

  private BoolQueryBuilder filterByUserAndTicketTypes(
      String userName, Set<TicketType> curatorTicketTypes) {
    return boolQuery()
        .should(filterByOwner(userName))
        .should(filterByTicketTypes(curatorTicketTypes))
        .minimumShouldMatch(1)
        .queryName(FILTER_BY_USER_AND_TICKET_TYPES);
  }

  private MultiMatchQueryBuilder filterByOwner(String userName) {
    return QueryBuilders.multiMatchQuery(userName, OWNER.searchFields().toArray(String[]::new))
        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
        .operator(Operator.AND)
        .queryName(FILTER_BY_OWNER);
  }

  private TermsQueryBuilder filterByTicketTypes(Set<TicketType> curatorTicketTypes) {
    var ticketTypes = curatorTicketTypes.stream().map(TicketType::toString).toArray(String[]::new);
    return new TermsQueryBuilder(TYPE_KEYWORD, ticketTypes).queryName(FILTER_BY_TICKET_TYPES);
  }

  private BoolQueryBuilder filterByDeniedUnpublishRequest() {
    return boolQuery()
        .mustNot(filterByTicketTypes(Set.of(TicketType.UNPUBLISH_REQUEST)))
        .queryName(FILTER_BY_UN_PUBLISHED);
  }

  private void validateOrganization() throws UnauthorizedException {
    if (isNull(organizationSet) || organizationSet.isEmpty()) {
      throw new UnauthorizedException(ORGANIZATION_IS_REQUIRED);
    }
  }

  private void validateOwner(String userName) throws UnauthorizedException {
    if (isCurrentUserNotOwner(userName)) {
      throw new UnauthorizedException(
          USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
    }
  }

  private void validateAssigneeAndOwnerParameters() throws UnauthorizedException {
    if (Optional.ofNullable(query.parameters().get(OWNER))
        .map(AsType::as)
        .map(v -> v.equals(query.parameters().get(ASSIGNEE).as()))
        .orElse(false)) {
      throw new UnauthorizedException(CANNOT_SEARCH_AS_BOTH_ASSIGNEE_AND_OWNER_AT_THE_SAME_TIME);
    }
  }

  private void validateUser() throws UnauthorizedException {
    if (isNull(currentUser)) {
      throw new UnauthorizedException(USER_IS_REQUIRED);
    }
  }

  private boolean validateSiktAdmin(Set<AccessRight> accessRights) throws UnauthorizedException {
    if (!accessRights.contains(MANAGE_CUSTOMERS)) {
      throw new UnauthorizedException(
          USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
    }
    return true;
  }

  private boolean searchAsTicketAssignee() {
    return query.parameters().isPresent(ASSIGNEE);
  }

  private boolean hasNoCuratorRoles(Set<TicketType> curatorTicketTypes) {
    return curatorTicketTypes.contains(TicketType.NONE);
  }

  private boolean isCurrentUserNotOwner(String userName) {
    return !userName.equalsIgnoreCase(query.parameters().get(OWNER).as());
  }

  private boolean searchAsTicketOwner() {
    return query.parameters().isPresent(OWNER);
  }

  private boolean searchAsSiktAdmin() {
    return query.parameters().isPresent(STATISTICS);
  }
}
