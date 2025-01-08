package no.unit.nva.search.ticket;

import static no.unit.nva.constants.Words.ASTERISK;
import static no.unit.nva.constants.Words.CUSTOMER_ID;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.FIRST_NAME;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.IDENTIFIER;
import static no.unit.nva.constants.Words.KEYWORD;
import static no.unit.nva.constants.Words.LAST_NAME;
import static no.unit.nva.constants.Words.MAIN_TITLE;
import static no.unit.nva.constants.Words.MESSAGES;
import static no.unit.nva.constants.Words.MODIFIED_DATE;
import static no.unit.nva.constants.Words.ORGANIZATION;
import static no.unit.nva.constants.Words.OWNER;
import static no.unit.nva.constants.Words.PIPE;
import static no.unit.nva.constants.Words.PUBLICATION;
import static no.unit.nva.constants.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.constants.Words.PUBLICATION_STATUS;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.constants.Words.TYPE;
import static no.unit.nva.constants.Words.USERNAME;
import static no.unit.nva.constants.Words.VIEWED_BY;
import static no.unit.nva.search.common.constant.Functions.branchBuilder;
import static no.unit.nva.search.common.constant.Functions.filterBranchBuilder;
import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static no.unit.nva.search.common.constant.Functions.multipleFields;

import static org.opensearch.index.query.QueryBuilders.boolQuery;

import nva.commons.core.JacocoGenerated;

import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constants for the ticket search.
 *
 * @author Stig Norland
 */
public final class Constants {

  static final String ASSIGNEE_ASTERISK = "assignee.*";
  static final String BY_USER_PENDING = "byUserPending";
  static final String CANNOT_SEARCH_AS_BOTH_ASSIGNEE_AND_OWNER_AT_THE_SAME_TIME =
      "Cannot search as both assignee and owner at the same time";
  static final String FILTER = "filter";
  static final String FILTER_BY_ASSIGNEE = "filterByAssignee";
  static final String FILTER_BY_ORGANIZATION = "filterByOrganization";
  static final String FILTER_BY_OWNER = "filterByOwner";
  static final String FILTER_BY_TICKET_TYPES = "filterByTicketTypes";
  static final String FILTER_BY_UN_PUBLISHED = "filterByUnPublished";
  static final String FILTER_BY_USER_AND_TICKET_TYPES = "filterByUserAndTicketTypes";
  static final String ORGANIZATION_IS_REQUIRED = "Organization is required";
  static final String USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES =
      "User is not allowed to search for tickets not owned by themselves";
  static final String USER_IS_REQUIRED = "User is required";
  static final Map<String, String> facetTicketsPaths =
      Map.of(
          BY_USER_PENDING, "/withAppliedFilter/byUserPending/type",
          STATUS, "/withAppliedFilter/status",
          TYPE, "/withAppliedFilter/type",
          PUBLICATION_STATUS, "/withAppliedFilter/publicationStatus");
  static final String ASSIGNEE = "assignee";
  static final String ASSIGNEE_FIELDS =
      multipleFields(
          jsonPath(ASSIGNEE, FIRST_NAME, KEYWORD),
          jsonPath(ASSIGNEE, LAST_NAME, KEYWORD),
          jsonPath(ASSIGNEE, USERNAME, KEYWORD));
  static final String CUSTOMER_ID_KEYWORD = CUSTOMER_ID + DOT + KEYWORD;
  static final String ID_KEYWORD = ID + DOT + KEYWORD;
  static final String MESSAGE_FIELDS =
      jsonPath(MESSAGES, "text", KEYWORD) + PIPE + jsonPath(MESSAGES, STATUS, KEYWORD);
  static final String ORGANIZATION_IDENTIFIER_KEYWORD =
      ORGANIZATION + DOT + IDENTIFIER + DOT + KEYWORD;
  static final String ORGANIZATION_ID_KEYWORD = ORGANIZATION + DOT + ID_KEYWORD;
  static final String ORGANIZATION_PATHS = ORGANIZATION + DOT + ASTERISK;
  static final String OWNER_USERNAME = OWNER + DOT + USERNAME + DOT + KEYWORD;
  static final String OWNER_FIELDS =
      multipleFields(
          OWNER_USERNAME,
          jsonPath(OWNER, FIRST_NAME, KEYWORD),
          jsonPath(OWNER, LAST_NAME, KEYWORD));
  static final String OWNER_KEYWORD = OWNER + DOT + KEYWORD;
  static final String PUBLICATION_ID_OR_IDENTIFIER_KEYWORD =
      jsonPath(PUBLICATION, ID, KEYWORD) + PIPE + jsonPath(PUBLICATION, IDENTIFIER, KEYWORD);
  static final String PUBLICATION_INSTANCE_KEYWORD =
      jsonPath(PUBLICATION, PUBLICATION_INSTANCE, TYPE, KEYWORD);
  static final String PUBLICATION_MAIN_TITLE_KEYWORD = jsonPath(PUBLICATION, MAIN_TITLE, KEYWORD);
  static final String PUBLICATION_MODIFIED_DATE = PUBLICATION + DOT + MODIFIED_DATE;
  static final String PUBLICATION_OWNER_KEYWORD = PUBLICATION + DOT + OWNER_KEYWORD;
  static final String STATUS_KEYWORD = STATUS + DOT + KEYWORD;
  static final String PUBLICATION_STATUS_KEYWORD = PUBLICATION + DOT + STATUS_KEYWORD;
  static final String TYPE_KEYWORD = TYPE + DOT + KEYWORD;
  static final String UNHANDLED_KEY = "unhandled key -> ";
  static final String VIEWED_BY_FIELDS =
      multipleFields(
          jsonPath(VIEWED_BY, USERNAME, KEYWORD),
          jsonPath(VIEWED_BY, FIRST_NAME, KEYWORD),
          jsonPath(VIEWED_BY, LAST_NAME, KEYWORD));
  private static final String FINALIZED_BY = "finalizedBy";
  static final String FINALIZED_BY_FIELDS =
      multipleFields(
          jsonPath(FINALIZED_BY, USERNAME, KEYWORD),
          jsonPath(FINALIZED_BY, FIRST_NAME, KEYWORD),
          jsonPath(FINALIZED_BY, LAST_NAME, KEYWORD));

  @JacocoGenerated
  public Constants() {}

  public static List<AggregationBuilder> getTicketsAggregations(
      String username, String... deniedTypes) {
    return List.of(
        branchBuilder(STATUS, STATUS_KEYWORD),
        branchBuilder(TYPE, TYPE_KEYWORD),
        branchBuilder(PUBLICATION_STATUS, PUBLICATION_STATUS_KEYWORD),
        notificationsAsCurator(username, Set.of(deniedTypes)));
  }

  private static QueryBuilder filterByAssignee(String userName) {
    return QueryBuilders.multiMatchQuery(userName, ASSIGNEE_ASTERISK)
        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
        .operator(Operator.AND)
        .queryName(FILTER_BY_ASSIGNEE);
  }

  private static AggregationBuilder notificationsAsCurator(
      String username, Set<String> deniedTypes) {
    var pending = TicketStatus.PENDING.toString();
    var queryFilter =
        boolQuery()
            .mustNot(new TermsQueryBuilder(TYPE_KEYWORD, deniedTypes))
            .must(new TermsQueryBuilder(STATUS_KEYWORD, pending))
            .must(filterByAssignee(username))
            .queryName(BY_USER_PENDING + FILTER);

    return filterBranchBuilder(BY_USER_PENDING, queryFilter)
        .subAggregation(branchBuilder(TYPE, TYPE_KEYWORD));
  }
}
