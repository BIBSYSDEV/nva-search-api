package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.constant.Words.DOI;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.IDENTITY;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NAME;
import static no.unit.nva.search2.constant.Words.ORC_ID;
import static no.unit.nva.search2.constant.Words.OWNER;
import static no.unit.nva.search2.constant.Words.OWNER_AFFILIATION;
import static no.unit.nva.search2.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search2.constant.Words.PUBLICATION_DATE;
import static no.unit.nva.search2.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search2.constant.Words.REFERENCE;
import static no.unit.nva.search2.constant.Words.RESOURCE_OWNER;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.constant.Words.YEAR;

public class ResourceFields {

    public static final String IDENTIFIER_KEYWORD = IDENTIFIER + DOT + KEYWORD;
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, LABELS);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME, KEYWORD);
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ORC_ID, KEYWORD);
    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, PUBLICATION_DATE, YEAR);
    public static final String ENTITY_DESCRIPTION_REFERENCE_DOI =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, DOI, KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "isbnList");
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "onlineIssn", KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "printIssn", KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, TYPE, KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE, KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE_KEYWORD =
        ApplicationConstants.jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE, KEYWORD);
    public static final String FUNDINGS_SOURCE_LABELS = ApplicationConstants.jsonPath(FUNDINGS, SOURCE, LABELS);
    public static final String RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD =
        ApplicationConstants.jsonPath(RESOURCE_OWNER, OWNER_AFFILIATION, KEYWORD);
    public static final String RESOURCE_OWNER_OWNER_KEYWORD = ApplicationConstants.jsonPath(RESOURCE_OWNER, OWNER,
                                                                                            KEYWORD);
}
