package no.unit.nva.search2.ticket;

import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;

import java.util.List;
import java.util.Locale;

import static no.unit.nva.search2.common.constant.Functions.branchBuilder;
import static no.unit.nva.search2.common.constant.Words.CREATED_DATE;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.MAIN_TITLE;
import static no.unit.nva.search2.common.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.common.constant.Words.OWNER;
import static no.unit.nva.search2.common.constant.Words.PIPE;
import static no.unit.nva.search2.common.constant.Words.STATUS;
import static no.unit.nva.search2.common.constant.Words.TYPE;

public final class Constants {

    public static final String CUSTOMER_ID = "customerId";
    public static final String PUBLICATION = "publication";
    public static final String ORGANIZATION = "organization";
    public static final String STATUS_KEYWORD = STATUS + DOT + KEYWORD;
    public static final String CUSTOMER_ID_KEYWORD = CUSTOMER_ID + DOT + KEYWORD;
    public static final String DEFAULT_TICKET_SORT =  TicketSort.CREATED_DATE.name().toLowerCase(Locale.getDefault());
    public static final String ID_KEYWORD = ID + DOT + KEYWORD;
    public static final String ORGANIZATION_ID_KEYWORD =  ORGANIZATION + DOT + ID_KEYWORD;
    public static final String OWNER_KEYWORD = OWNER + DOT + KEYWORD;
    public static final String PUBLICATION_CREATED_DATE = PUBLICATION + DOT + CREATED_DATE;
    public static final String PUBLICATION_ID_KEYWORD_PUBLICATION_IDENTIFIER_KEYWORD =
        PUBLICATION + DOT + ID + DOT + KEYWORD + PIPE + PUBLICATION + DOT + IDENTIFIER + DOT + KEYWORD;
    public static final String PUBLICATION_MAIN_TITLE_KEYWORD = PUBLICATION + DOT + MAIN_TITLE + DOT + KEYWORD;
    public static final String PUBLICATION_MODIFIED_DATE = PUBLICATION + DOT + MODIFIED_DATE;
    public static final String PUBLICATION_OWNER_KEYWORD = PUBLICATION + DOT + OWNER_KEYWORD;
    public static final String PUBLICATION_STATUS_KEYWORD = PUBLICATION + DOT + STATUS_KEYWORD;
    public static final String TYPE_KEYWORD = TYPE + DOT + KEYWORD;

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        TICKET_AGGREGATIONS = List.of(
        branchBuilder(TYPE, TYPE_KEYWORD),
        branchBuilder(STATUS, STATUS_KEYWORD)
    );


    @JacocoGenerated
    public Constants() {
    }

}