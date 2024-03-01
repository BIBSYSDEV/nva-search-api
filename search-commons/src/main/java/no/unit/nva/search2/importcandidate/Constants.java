package no.unit.nva.search2.importcandidate;

import static no.unit.nva.search2.common.constant.Functions.branchBuilder;
import static no.unit.nva.search2.common.constant.Functions.topLevelOrganisationsHierarchy;
import static no.unit.nva.search2.common.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.common.constant.Words.DOI;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.PIPE;
import static no.unit.nva.search2.common.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.resource.Constants.associatedArtifactsHierarchy;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

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
    public static final String IDENTIFIER = "id.keyword";
    public static final String IMPORTED_BY_USER = "importedByUser";
    public static final String IMPORT_CANDIDATES_INDEX_NAME = "import-candidates";
    public static final String IMPORT_STATUS_SET_BY_KEYWORD = "importStatus.setBy.keyword";
    public static final String INSTANCE_TYPE = "instanceType";
    public static final String INSTANCE_TYPE_KEYWORD = "publicationInstance.type";
    public static final String MAIN_TITLE_KEYWORD = "mainTitle.keyword";
    public static final String PUBLICATION_INSTANCE_TYPE = "publicationInstance.type";
    public static final String PUBLICATION_YEAR = "publicationYear";
    public static final String PUBLICATION_YEAR_KEYWORD = PUBLICATION_YEAR + DOT + KEYWORD;
    public static final String PUBLISHER_ID_KEYWORD = "publisher.id.keyword";
    public static final String STATUS_TYPE_KEYWORD = "importStatus.candidateStatus.keyword";
    public static final String TYPE_KEYWORD = "type.keyword";

    public static final String DEFAULT_IMPORT_CANDIDATE_SORT =
        ImportCandidateSort.CREATED_DATE.name().toLowerCase(Locale.getDefault());

    public static final List<AggregationBuilder> IMPORT_CANDIDATES_AGGREGATIONS =
        List.of(
            associatedArtifactsHierarchy(),
            branchBuilder(COLLABORATION_TYPE, COLLABORATION_TYPE_KEYWORD),
            branchBuilder(INSTANCE_TYPE, PUBLICATION_INSTANCE_TYPE),
            branchBuilder(PUBLICATION_YEAR, PUBLICATION_YEAR_KEYWORD),
            importStatusHierarchy(),
            topLevelOrganisationsHierarchy()
        );

    public static final Map<String, String> FACET_IMPORT_CANDIDATE_PATHS = Map.of(
        ASSOCIATED_ARTIFACTS, "/filter/associatedArtifacts/license",
        COLLABORATION_TYPE, "/filter/collaborationType/id",
        IMPORT_STATUS, "/filter/status",
        INSTANCE_TYPE, "/filter/publicationInstance.type",
        PUBLICATION_YEAR, "/filter/publicationYear",
        TOP_LEVEL_ORGANIZATIONS, "/filter/status"
    );

    private static TermsAggregationBuilder importStatusHierarchy() {
        return
            branchBuilder(IMPORT_STATUS, IMPORT_STATUS)
                .subAggregation(branchBuilder(CANDIDATE_STATUS, STATUS_TYPE_KEYWORD))
                .subAggregation(branchBuilder(IMPORTED_BY_USER, IMPORT_STATUS_SET_BY_KEYWORD));
    }


    @JacocoGenerated
    public Constants() {
    }
}