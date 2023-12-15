package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.Functions.generateFundingSource;
import static no.unit.nva.search2.constant.Functions.generateHasFileAggregation;
import static no.unit.nva.search2.constant.Functions.generateObjectLabelsAggregation;
import static no.unit.nva.search2.constant.Functions.jsonPath;
import static no.unit.nva.search2.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.constant.Words.DOI;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.IDENTITY;
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
import static no.unit.nva.search2.constant.Words.YEAR;
import java.util.List;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;

public class Resource {

    public static final String IDENTIFIER_KEYWORD = IDENTIFIER + DOT + KEYWORD;
    public static final String CONTRIBUTORS_AFFILIATION_ID_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD);
    public static final String CONTRIBUTORS_AFFILIATION_LABELS =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, LABELS);
    public static final String CONTRIBUTORS_IDENTITY_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID);
    public static final String CONTRIBUTORS_IDENTITY_NAME_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME, KEYWORD);
    public static final String CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ORC_ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
        jsonPath(ENTITY_DESCRIPTION, PUBLICATION_DATE, YEAR);
    public static final String REFERENCE_DOI_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, DOI, KEYWORD) + PIPE + jsonPath(DOI, KEYWORD);
    public static final String VISIBLE_FOR_NON_OWNER = jsonPath(ASSOCIATED_ARTIFACTS, "visibleForNonOwner");
    public static final String PUBLICATION_CONTEXT_ISBN_LIST =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "isbnList");
    public static final String PUBLICATION_CONTEXT_ONLINE_ISSN_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "onlineIssn", KEYWORD);
    public static final String PUBLICATION_CONTEXT_PRINT_ISSN_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "printIssn", KEYWORD);
    public static final String PUBLICATION_CONTEXT_TYPE_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, TYPE, KEYWORD);
    public static final String PUBLICATION_INSTANCE_TYPE =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE);
    public static final String ENTITY_DESCRIPTION_MAIN_TITLE = ENTITY_DESCRIPTION + DOT + Words.MAIN_TITLE;
    public static final String ENTITY_DESCRIPTION_MAIN_TITLE_KEYWORD = ENTITY_DESCRIPTION_MAIN_TITLE + DOT + KEYWORD;
    public static final String FUNDINGS_SOURCE_LABELS = jsonPath(FUNDINGS, SOURCE, LABELS);
    public static final String RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD =
        jsonPath(RESOURCE_OWNER, OWNER_AFFILIATION, KEYWORD);
    public static final String RESOURCE_OWNER_OWNER_KEYWORD = jsonPath(RESOURCE_OWNER, OWNER, KEYWORD);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD =
        jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, ENGLISH_CODE, KEYWORD) + PIPE
        + jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, NYNORSK_CODE, KEYWORD) + PIPE
        + jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, BOKMAAL_CODE, KEYWORD) + PIPE
        + jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, SAMI_CODE, KEYWORD);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION =
        CONTRIBUTORS_AFFILIATION_ID_KEYWORD + PIPE
        + ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD;


    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN =
        PUBLICATION_CONTEXT_ONLINE_ISSN_KEYWORD + PIPE + PUBLICATION_CONTEXT_PRINT_ISSN_KEYWORD;

    public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER =
        jsonPath(FUNDINGS, IDENTIFIER_KEYWORD + PIPE + FUNDINGS, SOURCE, IDENTIFIER);

    public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS =
        FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER + PIPE
        + jsonPath(FUNDINGS_SOURCE_LABELS, ENGLISH_CODE, KEYWORD) + PIPE
        + jsonPath(FUNDINGS_SOURCE_LABELS, NYNORSK_CODE, KEYWORD) + PIPE
        + jsonPath(FUNDINGS_SOURCE_LABELS, BOKMAAL_CODE, KEYWORD) + PIPE
        + jsonPath(FUNDINGS_SOURCE_LABELS, SAMI_CODE, KEYWORD);

    public static final String PARENT_PUBLICATION_ID =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, "corrigendumFor", KEYWORD) + PIPE
        + jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, "manifestations", ID, KEYWORD) + PIPE
        + jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, ID, KEYWORD);

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        RESOURCES_AGGREGATIONS = List.of(
        //        generateSimpleAggregation(CONTRIBUTOR_ID, CONTRIBUTORS_IDENTITY_ID)
        //            .subAggregation(generateSimpleAggregation(NAME, CONTRIBUTORS_IDENTITY_NAME_KEYWORD)),
        //        generateSimpleAggregation(TYPE, PUBLICATION_INSTANCE_TYPE),
        generateFundingSource(),
        generateObjectLabelsAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS),
        generateHasFileAggregation()
    );
}
