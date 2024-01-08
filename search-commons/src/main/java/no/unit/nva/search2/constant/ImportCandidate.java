package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.Functions.generateHasFileAggregation;
import static no.unit.nva.search2.constant.Functions.generateObjectLabelsAggregation;
import static no.unit.nva.search2.constant.Functions.generateSimpleAggregation;
import static no.unit.nva.search2.constant.Words.DOI;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.PIPE;
import java.util.List;
import java.util.Locale;
import no.unit.nva.search2.enums.ImportCandidateSort;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;

public final class ImportCandidate {

    public static final String ADDITIONAL_IDENTIFIERS_KEYWORD = "additionalIdentifiers.value.keyword";
    public static final String CANDIDATE_STATUS = "candidateStatus";
    public static final String COLLABORATION_TYPE = "collaborationType";
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

    public static final String ORGANIZATION = "organization";
    public static final String ORGANIZATIONS = "organizations";

    public static final String DEFAULT_IMPORT_CANDIDATE_SORT =
        ImportCandidateSort.CREATED_DATE.name().toLowerCase(Locale.getDefault());

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        IMPORT_CANDIDATES_AGGREGATIONS = List.of(
        generateSimpleAggregation(CANDIDATE_STATUS, STATUS_TYPE_KEYWORD),
        generateSimpleAggregation(PUBLICATION_YEAR, PUBLICATION_YEAR_KEYWORD),
        generateSimpleAggregation(INSTANCE_TYPE, PUBLICATION_INSTANCE_TYPE),
        generateSimpleAggregation(COLLABORATION_TYPE, COLLABORATION_TYPE_KEYWORD),
        generateSimpleAggregation(IMPORTED_BY_USER, IMPORT_STATUS_SET_BY_KEYWORD),
        generateObjectLabelsAggregation(ORGANIZATION, ORGANIZATIONS),
        generateHasFileAggregation()
    );

    @JacocoGenerated
    public ImportCandidate() {
    }

}