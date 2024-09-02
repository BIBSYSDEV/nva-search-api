package no.unit.nva.search.ticket;

import static no.unit.nva.search.common.constant.Functions.branchBuilder;
import static no.unit.nva.search.common.constant.Functions.filterBranchBuilder;
import static no.unit.nva.search.common.constant.Words.DOT;
import static no.unit.nva.search.common.constant.Words.ID;
import static no.unit.nva.search.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search.common.constant.Words.KEYWORD;
import static no.unit.nva.search.common.constant.Words.MAIN_TITLE;
import static no.unit.nva.search.common.constant.Words.MESSAGES;
import static no.unit.nva.search.common.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search.common.constant.Words.OWNER;
import static no.unit.nva.search.common.constant.Words.PART_OF;
import static no.unit.nva.search.common.constant.Words.PIPE;
import static no.unit.nva.search.common.constant.Words.STATUS;
import static no.unit.nva.search.common.constant.Words.TYPE;
import static no.unit.nva.search.common.constant.Words.VIEWED_BY;

import nva.commons.core.JacocoGenerated;

import org.opensearch.search.aggregations.AggregationBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author Stig Norland
 */
public final class Constants {

    public static final String CUSTOMER_ID = "customerId";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ORGANIZATION = "organization";
    public static final String PUBLICATION = "publication";
    public static final String PUBLICATION_STATUS = "publicationStatus";
    public static final String PUBLICATION_INSTANCE = "publicationInstance";
    public static final String USERNAME = "username";
    public static final String UNHANDLED_KEY = "unhandled key -> ";


    public static final String STATUS_KEYWORD = STATUS + DOT + KEYWORD;
    public static final String TYPE_KEYWORD = TYPE + DOT + KEYWORD;
    public static final String CUSTOMER_ID_KEYWORD = CUSTOMER_ID + DOT + KEYWORD;
    public static final String DEFAULT_TICKET_SORT = TicketSort.CREATED_DATE.asCamelCase();
    public static final String ID_KEYWORD = ID + DOT + KEYWORD;
    public static final String ORGANIZATION_ID_KEYWORD = ORGANIZATION + DOT + ID_KEYWORD;
    public static final String ORGANIZATION_IDENTIFIER_KEYWORD =
            ORGANIZATION + DOT + IDENTIFIER + DOT + KEYWORD;
    public static final String ORGANIZATION_PART_OF =
            ORGANIZATION
                    + DOT
                    + PART_OF
                    + DOT
                    + ID
                    + PIPE
                    + ORGANIZATION
                    + DOT
                    + PART_OF
                    + DOT
                    + IDENTIFIER;

    public static final String ORGANIZATION_PATHS =
            ORGANIZATION_ID_KEYWORD
                    + PIPE
                    + ORGANIZATION_IDENTIFIER_KEYWORD
                    + PIPE
                    + ORGANIZATION_PART_OF;
    public static final String OWNER_KEYWORD = OWNER + DOT + KEYWORD;
    public static final String PUBLICATION_ID_OR_IDENTIFIER_KEYWORD =
            PUBLICATION
                    + DOT
                    + ID
                    + DOT
                    + KEYWORD
                    + PIPE
                    + PUBLICATION
                    + DOT
                    + IDENTIFIER
                    + DOT
                    + KEYWORD;

    public static final String PUBLICATION_INSTANCE_KEYWORD =
            PUBLICATION + DOT + PUBLICATION_INSTANCE + DOT + TYPE_KEYWORD;

    public static final String PUBLICATION_MAIN_TITLE_KEYWORD =
            PUBLICATION + DOT + MAIN_TITLE + DOT + KEYWORD;
    public static final String PUBLICATION_MODIFIED_DATE = PUBLICATION + DOT + MODIFIED_DATE;
    public static final String PUBLICATION_OWNER_KEYWORD = PUBLICATION + DOT + OWNER_KEYWORD;
    public static final String PUBLICATION_STATUS_KEYWORD = PUBLICATION + DOT + STATUS_KEYWORD;
    public static final String OWNER_USERNAME = OWNER + DOT + USERNAME + DOT + KEYWORD;
    public static final String MESSAGE_FIELDS =
            MESSAGES + DOT + "text" + DOT + KEYWORD + PIPE + MESSAGES + DOT + STATUS + DOT
                    + KEYWORD;
    public static final String OWNER_FIELDS =
            OWNER
                    + DOT
                    + FIRST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + OWNER
                    + DOT
                    + LAST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + OWNER_USERNAME;
    public static final String VIEWED_BY_FIELDS =
            VIEWED_BY
                    + DOT
                    + FIRST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + VIEWED_BY
                    + DOT
                    + LAST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + VIEWED_BY
                    + DOT
                    + USERNAME
                    + DOT
                    + KEYWORD;
    public static final String ASSIGNEE = "assignee";
    public static final String ASSIGNEE_FIELDS =
            ASSIGNEE
                    + DOT
                    + FIRST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + ASSIGNEE
                    + DOT
                    + LAST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + ASSIGNEE
                    + DOT
                    + USERNAME
                    + DOT
                    + KEYWORD;
    private static final String FINALIZED_BY = "finalizedBy";
    public static final String FINALIZED_BY_FIELDS =
            FINALIZED_BY
                    + DOT
                    + FIRST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + FINALIZED_BY
                    + DOT
                    + LAST_NAME
                    + DOT
                    + KEYWORD
                    + PIPE
                    + FINALIZED_BY
                    + DOT
                    + USERNAME
                    + DOT
                    + KEYWORD;

    public static final String BY_USER_PENDING = "byUserPending";
    public static final Map<String, String> facetTicketsPaths =
            Map.of(
                    BY_USER_PENDING, "/withAppliedFilter/byUserPending/status/type",
                    STATUS, "/withAppliedFilter/status",
                    TYPE, "/withAppliedFilter/type",
                    PUBLICATION_STATUS, "/withAppliedFilter/publicationStatus");

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
