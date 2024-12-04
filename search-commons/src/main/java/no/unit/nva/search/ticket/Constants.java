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

import nva.commons.core.JacocoGenerated;

import org.opensearch.search.aggregations.AggregationBuilder;

import java.util.List;
import java.util.Map;

/**
 * Constants for the ticket search.
 *
 * @author Stig Norland
 */
public final class Constants {

    static final String ORG_AND_TYPE_OR_USER_NAME = "organizationAndAnyOfTicketTypeOrUserName";
    static final String USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES =
            "User is not allowed to search for tickets not owned by themselves";
    static final String CANNOT_SEARCH_AS_BOTH_ASSIGNEE_AND_OWNER_AT_THE_SAME_TIME =
            "Cannot search as both assignee and owner at the same time";

    public static final String BY_USER_PENDING = "byUserPending";
    private static final String FINALIZED_BY = "finalizedBy";
    public static final Map<String, String> facetTicketsPaths =
            Map.of(
                    BY_USER_PENDING, "/withAppliedFilter/byUserPending/status/type",
                    STATUS, "/withAppliedFilter/status",
                    TYPE, "/withAppliedFilter/type",
                    PUBLICATION_STATUS, "/withAppliedFilter/publicationStatus");
    public static final String ASSIGNEE = "assignee";
    public static final String ASSIGNEE_FIELDS =
            multipleFields(
                    jsonPath(ASSIGNEE, FIRST_NAME, KEYWORD),
                    jsonPath(ASSIGNEE, LAST_NAME, KEYWORD),
                    jsonPath(ASSIGNEE, USERNAME, KEYWORD));
    public static final String CUSTOMER_ID_KEYWORD = CUSTOMER_ID + DOT + KEYWORD;
    public static final String FINALIZED_BY_FIELDS =
            multipleFields(
                    jsonPath(FINALIZED_BY, USERNAME, KEYWORD),
                    jsonPath(FINALIZED_BY, FIRST_NAME, KEYWORD),
                    jsonPath(FINALIZED_BY, LAST_NAME, KEYWORD));
    public static final String ID_KEYWORD = ID + DOT + KEYWORD;
    public static final String MESSAGE_FIELDS =
            jsonPath(MESSAGES, "text", KEYWORD) + PIPE + jsonPath(MESSAGES, STATUS, KEYWORD);
    public static final String ORGANIZATION_IDENTIFIER_KEYWORD =
            ORGANIZATION + DOT + IDENTIFIER + DOT + KEYWORD;
    public static final String ORGANIZATION_ID_KEYWORD = ORGANIZATION + DOT + ID_KEYWORD;
    public static final String ORGANIZATION_PATHS = ORGANIZATION + DOT + ASTERISK;
    public static final String OWNER_USERNAME = OWNER + DOT + USERNAME + DOT + KEYWORD;
    public static final String OWNER_FIELDS =
            multipleFields(
                    OWNER_USERNAME,
                    jsonPath(OWNER, FIRST_NAME, KEYWORD),
                    jsonPath(OWNER, LAST_NAME, KEYWORD));
    public static final String OWNER_KEYWORD = OWNER + DOT + KEYWORD;
    public static final String PUBLICATION_ID_OR_IDENTIFIER_KEYWORD =
            jsonPath(PUBLICATION, ID, KEYWORD) + PIPE + jsonPath(PUBLICATION, IDENTIFIER, KEYWORD);
    public static final String PUBLICATION_INSTANCE_KEYWORD =
            jsonPath(PUBLICATION, PUBLICATION_INSTANCE, TYPE, KEYWORD);
    public static final String PUBLICATION_MAIN_TITLE_KEYWORD =
            jsonPath(PUBLICATION, MAIN_TITLE, KEYWORD);
    public static final String PUBLICATION_MODIFIED_DATE = PUBLICATION + DOT + MODIFIED_DATE;
    public static final String PUBLICATION_OWNER_KEYWORD = PUBLICATION + DOT + OWNER_KEYWORD;
    public static final String STATUS_KEYWORD = STATUS + DOT + KEYWORD;
    public static final String PUBLICATION_STATUS_KEYWORD = PUBLICATION + DOT + STATUS_KEYWORD;
    public static final String TYPE_KEYWORD = TYPE + DOT + KEYWORD;
    public static final String UNHANDLED_KEY = "unhandled key -> ";
    public static final String VIEWED_BY_FIELDS =
            multipleFields(
                    jsonPath(VIEWED_BY, USERNAME, KEYWORD),
                    jsonPath(VIEWED_BY, FIRST_NAME, KEYWORD),
                    jsonPath(VIEWED_BY, LAST_NAME, KEYWORD));

    @JacocoGenerated
    public Constants() {}

    public static List<AggregationBuilder> getTicketsAggregations(String username) {
        return List.of(
                branchBuilder(STATUS, STATUS_KEYWORD),
                branchBuilder(TYPE, TYPE_KEYWORD),
                branchBuilder(PUBLICATION_STATUS, PUBLICATION_STATUS_KEYWORD),
                notificationsByUser(username));
    }

    private static AggregationBuilder notificationsByUser(String username) {
        return filterBranchBuilder(BY_USER_PENDING, username, ASSIGNEE, USERNAME, KEYWORD)
                .subAggregation(
                        filterBranchBuilder(STATUS, TicketStatus.PENDING.toString(), STATUS_KEYWORD)
                                .subAggregation(branchBuilder(TYPE, TYPE_KEYWORD)));
    }
}
