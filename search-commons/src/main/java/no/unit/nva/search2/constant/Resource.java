package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.Functions.generateHasFileAggregation;
import static no.unit.nva.search2.constant.Functions.generateLabelsAggregation;
import static no.unit.nva.search2.constant.Functions.generateObjectLabelsAggregation;
import static no.unit.nva.search2.constant.Functions.generateSimpleAggregation;
import static no.unit.nva.search2.constant.Functions.jsonPath;
import static no.unit.nva.search2.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.CONTEXT_TYPE;
import static no.unit.nva.search2.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.constant.Words.DOI;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.IDENTITY;
import static no.unit.nva.search2.constant.Words.INSTANCE_TYPE;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NAME;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.ORC_ID;
import static no.unit.nva.search2.constant.Words.OWNER;
import static no.unit.nva.search2.constant.Words.OWNER_AFFILIATION;
import static no.unit.nva.search2.constant.Words.PIPE;
import static no.unit.nva.search2.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search2.constant.Words.PUBLICATION_DATE;
import static no.unit.nva.search2.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search2.constant.Words.REFERENCE;
import static no.unit.nva.search2.constant.Words.RESOURCE_OWNER;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.constant.Words.USER;
import static no.unit.nva.search2.constant.Words.USER_AFFILIATION;
import static no.unit.nva.search2.constant.Words.YEAR;
import java.util.List;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;

public class Resource {

    public static final String IDENTIFIER_KEYWORD = IDENTIFIER + DOT + KEYWORD;
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, LABELS);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME, KEYWORD);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ORC_ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
        jsonPath(ENTITY_DESCRIPTION, PUBLICATION_DATE, YEAR);
    public static final String ENTITY_DESCRIPTION_REFERENCE_DOI =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, DOI, KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "isbnList");
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "onlineIssn", KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "printIssn", KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, TYPE, KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE);
    public static final String FUNDINGS_SOURCE_LABELS = jsonPath(FUNDINGS, SOURCE, LABELS);
    public static final String RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD =
        jsonPath(RESOURCE_OWNER, OWNER_AFFILIATION, KEYWORD);
    public static final String RESOURCE_OWNER_OWNER_KEYWORD = jsonPath(RESOURCE_OWNER, OWNER, KEYWORD);

    public static final String MAIN_TITLE = ENTITY_DESCRIPTION + DOT + "mainTitle";

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, ENGLISH_CODE, KEYWORD)
        + PIPE + jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, NYNORSK_CODE, KEYWORD)
        + PIPE + jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, BOKMAAL_CODE, KEYWORD)
        + PIPE + jsonPath(ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS, SAMI_CODE, KEYWORD);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION =
        ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID
        + PIPE + ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD;

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY =
        ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID
        + PIPE + ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME;

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN =
        ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN
        + PIPE + ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN;

    public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER =
        jsonPath(FUNDINGS, IDENTIFIER_KEYWORD + PIPE + FUNDINGS, SOURCE, IDENTIFIER);

    public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS =
        FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER
        + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, ENGLISH_CODE, KEYWORD)
        + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, NYNORSK_CODE, KEYWORD)
        + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, BOKMAAL_CODE, KEYWORD)
        + PIPE + jsonPath(FUNDINGS_SOURCE_LABELS, SAMI_CODE, KEYWORD);

    public static final String PARENT_PUBLICATION_ID =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, "corrigendumFor", KEYWORD)
        + PIPE + jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, "manifestations", ID, KEYWORD)
        + PIPE + jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, ID, KEYWORD);

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        RESOURCES_AGGREGATIONS = List.of(
        generateSimpleAggregation(USER, RESOURCE_OWNER_OWNER_KEYWORD),
        generateSimpleAggregation(USER_AFFILIATION, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
        generateSimpleAggregation(INSTANCE_TYPE, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE),
        generateSimpleAggregation(CONTEXT_TYPE, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD),
        generateSimpleAggregation(FUNDING_SOURCE, jsonPath(FUNDINGS, SOURCE, IDENTIFIER))
            .subAggregation(generateLabelsAggregation(jsonPath(FUNDINGS, SOURCE))),
        generateObjectLabelsAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS),
        generateHasFileAggregation()
    );
}
