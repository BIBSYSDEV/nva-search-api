package no.unit.nva.search2.importcandidate;

import static no.unit.nva.search2.common.constant.Functions.branchBuilder;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Functions.labels;
import static no.unit.nva.search2.common.constant.Functions.multipleFields;
import static no.unit.nva.search2.common.constant.Functions.nestedBranchBuilder;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.common.constant.Words.DOI;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.FILES;
import static no.unit.nva.search2.common.constant.Words.FILES_STATUS;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.IDENTITY;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search2.common.constant.Words.NAME;
import static no.unit.nva.search2.common.constant.Words.PIPE;
import static no.unit.nva.search2.common.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.common.constant.Words.TYPE;

import java.util.List;
import java.util.Map;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;


public final class Constants {

    public static final String ADDITIONAL_IDENTIFIERS_KEYWORD = "additionalIdentifiers.value.keyword";
    public static final String CANDIDATE_STATUS = "candidateStatus";
    public static final String COLLABORATION_TYPE = "collaborationType";
    public static final String IMPORT_STATUS = "importStatus";
    public static final String COLLABORATION_TYPE_KEYWORD = COLLABORATION_TYPE + DOT + KEYWORD;
    public static final String CONTRIBUTORS_IDENTITY_ID = "contributors.identity.id.keyword";
    public static final String CONTRIBUTORS_IDENTITY_NAME = "contributors.identity.name.keyword";
    public static final String CONTRIBUTOR_IDENTITY_KEYWORDS =
        CONTRIBUTORS_IDENTITY_ID + PIPE + CONTRIBUTORS_IDENTITY_NAME;
    public static final String DOI_KEYWORD = DOI + DOT + KEYWORD;
    public static final String IMPORT_CANDIDATES_INDEX_NAME = "import-candidates";
    public static final String ID_KEYWORD = ID + DOT + KEYWORD;
    public static final String MODIFIED_DATE_PATH = IMPORT_STATUS + DOT + MODIFIED_DATE + DOT + KEYWORD;
    public static final String MAIN_TITLE_KEYWORD = "mainTitle.keyword";
    public static final String ORGANIZATIONS = "organizations";
    public static final String ORGANIZATIONS_PATH = ORGANIZATIONS + DOT + ID_KEYWORD;
    public static final String PUBLICATION_INSTANCE_TYPE = "publicationInstance.type.keyword";
    public static final String PUBLICATION_YEAR = "publicationYear";
    public static final String PUBLICATION_YEAR_KEYWORD = PUBLICATION_YEAR + DOT + KEYWORD;
    public static final String PUBLISHER_ID_KEYWORD = "publisher.id.keyword";
    public static final String IMPORT_STATUS_PATH = multipleFields(
        jsonPath(IMPORT_STATUS, CANDIDATE_STATUS, KEYWORD),
        jsonPath(IMPORT_STATUS, "setBy", KEYWORD)
    );
    public static final String TYPE_KEYWORD = "type.keyword";
    public static final String FILES_STATUS_PATH = FILES_STATUS + DOT + KEYWORD;

    public static final List<AggregationBuilder> IMPORT_CANDIDATES_AGGREGATIONS =
        List.of(
            branchBuilder(COLLABORATION_TYPE, COLLABORATION_TYPE_KEYWORD),
            branchBuilder(TYPE, PUBLICATION_INSTANCE_TYPE),
            branchBuilder(PUBLICATION_YEAR, PUBLICATION_YEAR_KEYWORD),
            branchBuilder(IMPORT_STATUS, IMPORT_STATUS, CANDIDATE_STATUS, KEYWORD),
            branchBuilder(FILES, FILES_STATUS, KEYWORD),
            contributor(),
            topLevelOrganisationsHierarchy()
        );

    public static final Map<String, String> FACET_IMPORT_CANDIDATE_PATHS = Map.of(
        CONTRIBUTOR, "/withAppliedFilter/contributor/id",
        COLLABORATION_TYPE, "/withAppliedFilter/collaborationType",
        TYPE, "/withAppliedFilter/type",
        IMPORT_STATUS, "/withAppliedFilter/importStatus",
        FILES, "/withAppliedFilter/files",
        PUBLICATION_YEAR, "/withAppliedFilter/publicationYear",
        TOP_LEVEL_ORGANIZATION, "/withAppliedFilter/organizations/id"
        //        LICENSE, "/withAppliedFilter/associatedArtifacts/license"
    );

    private static NestedAggregationBuilder contributor() {
        return nestedBranchBuilder(CONTRIBUTOR, CONTRIBUTORS)
            .subAggregation(
                branchBuilder(ID, CONTRIBUTORS, IDENTITY, ID, KEYWORD)
                    .subAggregation(
                        branchBuilder(NAME, CONTRIBUTORS, IDENTITY, NAME, KEYWORD)
                    )
            );
    }

    public static NestedAggregationBuilder topLevelOrganisationsHierarchy() {
        return
            nestedBranchBuilder(ORGANIZATIONS, ORGANIZATIONS)
                .subAggregation(
                    branchBuilder(ID, ORGANIZATIONS, ID, KEYWORD)
                        .subAggregation(
                            labels(ORGANIZATIONS)
                        )
                );
    }

    @JacocoGenerated
    public Constants() {
    }
}