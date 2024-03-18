package no.unit.nva.search2.ticket;

import static no.unit.nva.search2.common.constant.Functions.branchBuilder;
import static no.unit.nva.search2.common.constant.Functions.filterBranchBuilder;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.MAIN_TITLE;
import static no.unit.nva.search2.common.constant.Words.MESSAGES;
import static no.unit.nva.search2.common.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.common.constant.Words.OWNER;
import static no.unit.nva.search2.common.constant.Words.PIPE;
import static no.unit.nva.search2.common.constant.Words.STATUS;
import static no.unit.nva.search2.common.constant.Words.TYPE;
import static no.unit.nva.search2.common.constant.Words.VIEWED_BY;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import no.unit.nva.search2.common.enums.TicketStatus;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;

public final class Constants {

    public static final String CUSTOMER_ID = "customerId";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ORGANIZATION = "organization";
    public static final String PUBLICATION = "publication";
    public static final String PUBLICATION_STATUS = "publicationStatus";
    public static final String USERNAME = "username";
    public static final String STATUS_KEYWORD = STATUS + DOT + KEYWORD;
    public static final String TYPE_KEYWORD = TYPE + DOT + KEYWORD;
    public static final String CUSTOMER_ID_KEYWORD = CUSTOMER_ID + DOT + KEYWORD;
    public static final String DEFAULT_TICKET_SORT = TicketSort.CREATED_DATE.name().toLowerCase(Locale.getDefault());
    public static final String ID_KEYWORD = ID + DOT + KEYWORD;
    public static final String ORGANIZATION_ID_KEYWORD = ORGANIZATION + DOT + ID_KEYWORD;
    public static final String OWNER_KEYWORD = OWNER + DOT + KEYWORD;
    public static final String PUBLICATION_ID_OR_IDENTIFIER_KEYWORD =
        PUBLICATION + DOT + ID + DOT + KEYWORD + PIPE
        + PUBLICATION + DOT + IDENTIFIER + DOT + KEYWORD;
    public static final String PUBLICATION_MAIN_TITLE_KEYWORD = PUBLICATION + DOT + MAIN_TITLE + DOT + KEYWORD;
    public static final String PUBLICATION_MODIFIED_DATE = PUBLICATION + DOT + MODIFIED_DATE;
    public static final String PUBLICATION_OWNER_KEYWORD = PUBLICATION + DOT + OWNER_KEYWORD;
    public static final String PUBLICATION_STATUS_KEYWORD = PUBLICATION + DOT + STATUS_KEYWORD;
    public static final String OWNER_USERNAME = OWNER + DOT + USERNAME + DOT + KEYWORD;
    public static final String MESSAGE_FIELDS =
        MESSAGES + DOT + TYPE_KEYWORD + PIPE
        + MESSAGES + DOT + "text" + DOT + KEYWORD + PIPE
        + MESSAGES + DOT + STATUS + DOT + KEYWORD;
    public static final String OWNER_FIELDS =
        OWNER + DOT + TYPE_KEYWORD + PIPE
        + OWNER + DOT + FIRST_NAME + DOT + KEYWORD + PIPE
        + OWNER + DOT + LAST_NAME + DOT + KEYWORD + PIPE
        + OWNER_USERNAME;
    public static final String VIEWED_BY_FIELDS =
        VIEWED_BY + DOT + TYPE_KEYWORD + PIPE
        + VIEWED_BY + DOT + FIRST_NAME + DOT + KEYWORD + PIPE
        + VIEWED_BY + DOT + LAST_NAME + DOT + KEYWORD + PIPE
        + VIEWED_BY + DOT + USERNAME + DOT + KEYWORD;
    public static final String USER_NOTIFICATIONS = "UserNotification";
    public static final String UNASSIGNED_NOTIFICATIONS = "UnassignedNotification";
    public static final String DOI_REQUEST_NOTIFICATIONS = "DoiRequestNotification";
    public static final String PUBLISHING_REQUEST_NOTIFICATIONS = "PublishingRequestNotification";
    public static final String GENERAL_SUPPORT_NOTIFICATIONS = "GeneralSupportNotification";
    public static final String ASSIGNEE = "assignee";
    public static final String ASSIGNEE_FIELDS =
        ASSIGNEE + DOT + TYPE_KEYWORD + PIPE
        + ASSIGNEE + DOT + FIRST_NAME + DOT + KEYWORD + PIPE
        + ASSIGNEE + DOT + LAST_NAME + DOT + KEYWORD + PIPE
        + ASSIGNEE + DOT + USERNAME + DOT + KEYWORD;
    private static final String FINALIZED_BY = "finalizedBy";
    public static final String FINALIZED_BY_FIELDS =
        FINALIZED_BY + DOT + TYPE_KEYWORD + PIPE
        + FINALIZED_BY + DOT + FIRST_NAME + DOT + KEYWORD + PIPE
        + FINALIZED_BY + DOT + LAST_NAME + DOT + KEYWORD + PIPE
        + FINALIZED_BY + DOT + USERNAME + DOT + KEYWORD;

    public static final String BY_USER_PENDING = "byUserPending";
    public static final Map<String, String> facetTicketsPaths = Map.of(
        USER_NOTIFICATIONS, "/withAppliedFilter/UserNotification",
        UNASSIGNED_NOTIFICATIONS, "/withAppliedFilter/UnassignedNotification",
        DOI_REQUEST_NOTIFICATIONS, "/withAppliedFilter/DoiRequestNotification",
        GENERAL_SUPPORT_NOTIFICATIONS, "/withAppliedFilter/GeneralSupportNotification",
        PUBLISHING_REQUEST_NOTIFICATIONS, "/withAppliedFilter/PublishingRequestNotification",
        BY_USER_PENDING, "/withAppliedFilter/byUserPending/status/type",
        STATUS, "/withAppliedFilter/status",
        TYPE, "/withAppliedFilter/type",
        PUBLICATION_STATUS, "/withAppliedFilter/publication/status");

    @JacocoGenerated
    public Constants() {
    }

    public static List<AggregationBuilder> getTicketsAggregations(String username) {
        return List.of(
            branchBuilder(STATUS, STATUS_KEYWORD),
            branchBuilder(TYPE, TYPE_KEYWORD),
            branchBuilder(PUBLICATION_STATUS, PUBLICATION_STATUS_KEYWORD),
            notificationsByUser(username),
            unassignedNotifications(),
            notifications(username, Arrays.stream(TicketType.values()).toList()),
            doiRequestNotifications(username),
            publishingRequestNotifications(username),
            generalSupportNotifications(username)
        );
    }

    private static AggregationBuilder notificationsByUser(String username) {
        return
            filterBranchBuilder(BY_USER_PENDING, username, ASSIGNEE, USERNAME, KEYWORD)
                .subAggregation(
                    filterBranchBuilder(STATUS, TicketStatus.PENDING.toString(), STATUS_KEYWORD)
                        .subAggregation(
                            branchBuilder(TYPE, TYPE_KEYWORD)
                        )
                );
    }


    private static AggregationBuilder generalSupportNotifications(String username) {
        return getTicketAggregationFor(GENERAL_SUPPORT_NOTIFICATIONS, username, TicketType.GENERAL_SUPPORT_CASE);
    }

    private static AggregationBuilder publishingRequestNotifications(String username) {
        return getTicketAggregationFor(PUBLISHING_REQUEST_NOTIFICATIONS, username, TicketType.PUBLISHING_REQUEST);
    }

    private static AggregationBuilder doiRequestNotifications(String username) {
        return getTicketAggregationFor(DOI_REQUEST_NOTIFICATIONS, username, TicketType.DOI_REQUEST);
    }

    private static FilterAggregationBuilder getTicketAggregationFor(String aggregationName, String username,
                                                                    TicketType... types) {
        var query = QueryBuilders.boolQuery();
        query.must(QueryBuilders.termQuery(jsonPath(STATUS, KEYWORD), TicketStatus.PENDING.toString()));
        query.must(QueryBuilders.termQuery(jsonPath(ASSIGNEE, USERNAME, KEYWORD), username));
        query.must(QueryBuilders.termsQuery(jsonPath(TYPE, KEYWORD), types));
        return AggregationBuilders.filter(aggregationName, query);
    }

    private static AggregationBuilder notifications(String username, List<TicketType> ticketTypes) {
        return getTicketAggregationFor(USER_NOTIFICATIONS,
                                       username,
                                       ticketTypes.toArray(new TicketType[0]));
    }

    private static AggregationBuilder unassignedNotifications() {
        var query = QueryBuilders.boolQuery();
        query.must(QueryBuilders.termQuery(jsonPath(STATUS, KEYWORD), TicketStatus.NEW.toString()));
        //        query.must(QueryBuilders.termsQuery(jsonPath(TYPE, KEYWORD), ticketTypes));
        return AggregationBuilders.filter(UNASSIGNED_NOTIFICATIONS, query);
    }
}