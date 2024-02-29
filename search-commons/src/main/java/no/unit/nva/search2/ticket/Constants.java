package no.unit.nva.search2.ticket;

import static no.unit.nva.search2.common.constant.Functions.branchBuilder;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AggregationBuilder;

public final class Constants {

    private static final String ASSIGNEE = "assignee";
    private static final String FINALIZED_BY = "finalizedBy";
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
    public static final String PUBLICATION_ID_KEYWORD_PUBLICATION_IDENTIFIER_KEYWORD =
        PUBLICATION + DOT + ID + DOT + KEYWORD + PIPE + PUBLICATION + DOT + IDENTIFIER + DOT + KEYWORD;
    public static final String PUBLICATION_MAIN_TITLE_KEYWORD = PUBLICATION + DOT + MAIN_TITLE + DOT + KEYWORD;
    public static final String PUBLICATION_MODIFIED_DATE = PUBLICATION + DOT + MODIFIED_DATE;
    public static final String PUBLICATION_OWNER_KEYWORD = PUBLICATION + DOT + OWNER_KEYWORD;
    public static final String PUBLICATION_STATUS_KEYWORD = PUBLICATION + DOT + STATUS_KEYWORD;

    public static final String ASSIGNEE_FIELDS = ASSIGNEE + DOT + TYPE_KEYWORD + PIPE +
                                                 ASSIGNEE + DOT + FIRST_NAME + DOT + KEYWORD + PIPE +
                                                 ASSIGNEE + DOT + LAST_NAME + DOT + KEYWORD + PIPE +
                                                 ASSIGNEE + DOT + USERNAME + DOT + KEYWORD;

    public static final String FINALIZED_BY_FIELDS = FINALIZED_BY + DOT + TYPE_KEYWORD + PIPE +
                                                     FINALIZED_BY + DOT + FIRST_NAME + DOT + KEYWORD + PIPE +
                                                     FINALIZED_BY + DOT + LAST_NAME + DOT + KEYWORD + PIPE +
                                                     FINALIZED_BY + DOT + USERNAME + DOT + KEYWORD;

    public static final String MESSAGE_FIELDS = MESSAGES + DOT + TYPE_KEYWORD + PIPE +
                                                MESSAGES + DOT + "text" + DOT + KEYWORD + PIPE +
                                                MESSAGES + DOT + STATUS + DOT + KEYWORD;

    public static final String OWNER_FIELDS = OWNER + DOT + TYPE_KEYWORD + PIPE +
                                              OWNER + DOT + FIRST_NAME + DOT + KEYWORD + PIPE +
                                              OWNER + DOT + LAST_NAME + DOT + KEYWORD + PIPE +
                                              OWNER + DOT + USERNAME + DOT + KEYWORD;

    public static final String VIEWED_BY_FIELDS = VIEWED_BY + DOT + TYPE_KEYWORD + PIPE +
                                                  VIEWED_BY + DOT + FIRST_NAME + DOT + KEYWORD + PIPE +
                                                  VIEWED_BY + DOT + LAST_NAME + DOT + KEYWORD + PIPE +
                                                  VIEWED_BY + DOT + USERNAME + DOT + KEYWORD;

    public static final List<AggregationBuilder> TICKET_AGGREGATIONS =
        List.of(
            branchBuilder(STATUS, STATUS_KEYWORD),
            branchBuilder(TYPE, TYPE_KEYWORD),
            branchBuilder(PUBLICATION_STATUS, PUBLICATION_STATUS_KEYWORD)
        );

    public static final Map<String, String> facetResourcePaths = Map.of(
        STATUS, "/filter/status",
        TYPE, "/filter/type",
        PUBLICATION_STATUS, "/filter/publication/status"
    );

    @JacocoGenerated
    public Constants() {
    }
}