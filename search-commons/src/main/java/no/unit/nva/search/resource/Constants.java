package no.unit.nva.search.resource;

import static no.unit.nva.search.common.constant.Functions.branchBuilder;
import static no.unit.nva.search.common.constant.Functions.filterBranchBuilder;
import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static no.unit.nva.search.common.constant.Functions.labels;
import static no.unit.nva.search.common.constant.Functions.multipleFields;
import static no.unit.nva.search.common.constant.Functions.nestedBranchBuilder;
import static no.unit.nva.search.common.constant.Words.ABSTRACT;
import static no.unit.nva.search.common.constant.Words.AFFILIATIONS;
import static no.unit.nva.search.common.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search.common.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search.common.constant.Words.CODE;
import static no.unit.nva.search.common.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search.common.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search.common.constant.Words.COURSE;
import static no.unit.nva.search.common.constant.Words.DOI;
import static no.unit.nva.search.common.constant.Words.DOT;
import static no.unit.nva.search.common.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search.common.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.common.constant.Words.FILES;
import static no.unit.nva.search.common.constant.Words.FILES_STATUS;
import static no.unit.nva.search.common.constant.Words.FUNDINGS;
import static no.unit.nva.search.common.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search.common.constant.Words.HANDLE;
import static no.unit.nva.search.common.constant.Words.ID;
import static no.unit.nva.search.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search.common.constant.Words.IDENTITY;
import static no.unit.nva.search.common.constant.Words.ISBN_LIST;
import static no.unit.nva.search.common.constant.Words.ISBN_PREFIX;
import static no.unit.nva.search.common.constant.Words.JOURNAL;
import static no.unit.nva.search.common.constant.Words.JOURNAL_AS_TYPE;
import static no.unit.nva.search.common.constant.Words.KEYWORD;
import static no.unit.nva.search.common.constant.Words.LABELS;
import static no.unit.nva.search.common.constant.Words.LANGUAGE;
import static no.unit.nva.search.common.constant.Words.LICENSE;
import static no.unit.nva.search.common.constant.Words.MAIN_TITLE;
import static no.unit.nva.search.common.constant.Words.NAME;
import static no.unit.nva.search.common.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search.common.constant.Words.ONLINE_ISSN;
import static no.unit.nva.search.common.constant.Words.ORC_ID;
import static no.unit.nva.search.common.constant.Words.OWNER;
import static no.unit.nva.search.common.constant.Words.OWNER_AFFILIATION;
import static no.unit.nva.search.common.constant.Words.PIPE;
import static no.unit.nva.search.common.constant.Words.PRINT_ISSN;
import static no.unit.nva.search.common.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search.common.constant.Words.PUBLICATION_DATE;
import static no.unit.nva.search.common.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search.common.constant.Words.PUBLISHER;
import static no.unit.nva.search.common.constant.Words.REFERENCE;
import static no.unit.nva.search.common.constant.Words.RESOURCE_OWNER;
import static no.unit.nva.search.common.constant.Words.ROOT;
import static no.unit.nva.search.common.constant.Words.SAMI_CODE;
import static no.unit.nva.search.common.constant.Words.SCIENTIFIC_INDEX;
import static no.unit.nva.search.common.constant.Words.SCIENTIFIC_VALUE;
import static no.unit.nva.search.common.constant.Words.SERIES;
import static no.unit.nva.search.common.constant.Words.SERIES_AS_TYPE;
import static no.unit.nva.search.common.constant.Words.SOURCE;
import static no.unit.nva.search.common.constant.Words.STATUS;
import static no.unit.nva.search.common.constant.Words.TAGS;
import static no.unit.nva.search.common.constant.Words.TITLE;
import static no.unit.nva.search.common.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search.common.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search.common.constant.Words.TYPE;
import static no.unit.nva.search.common.constant.Words.VALUE;
import static no.unit.nva.search.common.constant.Words.YEAR;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.search.aggregations.metrics.CardinalityAggregationBuilder;

/**
 * @author Stig Norland
 */
public final class Constants {

    public static final String PERSON_PREFERENCES = "/person-preferences/";
    public static final String UNIQUE_PUBLICATIONS = "unique_publications";
    public static final String CRISTIN_ORGANIZATION_PATH = "/cristin/organization/";
    public static final String CRISTIN_PERSON_PATH = "/cristin/person/";
    public static final String IDENTIFIER_KEYWORD = IDENTIFIER + DOT + KEYWORD;
    public static final String FILES_STATUS_KEYWORD = FILES_STATUS + DOT + KEYWORD;
    public static final String ENTITY_CONTRIBUTORS_DOT = ENTITY_DESCRIPTION + DOT + CONTRIBUTORS + DOT;
    public static final String ENTITY_PUBLICATION_CONTEXT_DOT =
        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + PUBLICATION_CONTEXT + DOT;

    public static final String REFERENCE_PUBLICATION_CONTEXT_ID_KEYWORD =
        ENTITY_PUBLICATION_CONTEXT_DOT + ID + DOT + KEYWORD;

    public static final String ENTITY_PUBLICATION_INSTANCE_DOT =
        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + PUBLICATION_INSTANCE + DOT;

    //    public static final String REFERENCE_PUBLICATION =
    //        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + PUBLICATION_CONTEXT + ASTERISK + PIPE
    //            + ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + PUBLICATION_INSTANCE + ASTERISK;

    public static final String CONTRIBUTORS_AFFILIATION_ID_KEYWORD =
        ENTITY_CONTRIBUTORS_DOT + AFFILIATIONS + DOT + ID + DOT + KEYWORD;

    public static final String CONTRIBUTORS_AFFILIATION_LABELS =
        ENTITY_CONTRIBUTORS_DOT + AFFILIATIONS + DOT + LABELS;
    public static final String CONTRIBUTORS_IDENTITY_ID =
        ENTITY_CONTRIBUTORS_DOT + IDENTITY + DOT + ID + DOT + KEYWORD;
    public static final String CONTRIBUTORS_IDENTITY_NAME_KEYWORD =
        ENTITY_CONTRIBUTORS_DOT + IDENTITY + DOT + NAME + DOT + KEYWORD + PIPE +
            CONTRIBUTORS_IDENTITY_ID;
    public static final String CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD =
        ENTITY_CONTRIBUTORS_DOT + IDENTITY + DOT + ORC_ID + DOT + KEYWORD;
    public static final String SCIENTIFIC_LEVEL_SEARCH_FIELD = multipleFields(
        ENTITY_PUBLICATION_CONTEXT_DOT + PUBLISHER + DOT + SCIENTIFIC_VALUE + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + SCIENTIFIC_VALUE + DOT + KEYWORD
    );
    public static final String COURSE_CODE_KEYWORD =
        ENTITY_PUBLICATION_CONTEXT_DOT + COURSE + DOT + CODE + DOT + KEYWORD;

    public static final String ENTITY_DESCRIPTION_PUBLICATION_PAGES = ENTITY_PUBLICATION_INSTANCE_DOT + "pages.pages";
    public static final String SUBJECTS = "subjects";

    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
        ENTITY_DESCRIPTION + DOT + PUBLICATION_DATE + DOT + YEAR;
    public static final String REFERENCE_DOI_KEYWORD =
        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + DOI + DOT + KEYWORD + PIPE + DOI + DOT + KEYWORD;
    public static final String ASSOCIATED_ARTIFACTS_LABELS = ASSOCIATED_ARTIFACTS + DOT + LICENSE + DOT + LABELS;
    public static final String ASSOCIATED_ARTIFACTS_LICENSE = multipleFields(
        ASSOCIATED_ARTIFACTS + DOT + LICENSE + DOT + NAME + DOT + KEYWORD,
        ASSOCIATED_ARTIFACTS + DOT + LICENSE + DOT + VALUE + DOT + KEYWORD,
        jsonPath(ASSOCIATED_ARTIFACTS_LABELS, ENGLISH_CODE, KEYWORD),
        jsonPath(ASSOCIATED_ARTIFACTS_LABELS, NYNORSK_CODE, KEYWORD),
        jsonPath(ASSOCIATED_ARTIFACTS_LABELS, BOKMAAL_CODE, KEYWORD),
        jsonPath(ASSOCIATED_ARTIFACTS_LABELS, SAMI_CODE, KEYWORD)
    );
    public static final String PUBLISHER_ID_KEYWORD = PUBLISHER + DOT + ID + DOT + KEYWORD;
    public static final String STATUS_KEYWORD = STATUS + DOT + KEYWORD;
    public static final String PUBLICATION_CONTEXT_ISBN_LIST =
        ENTITY_PUBLICATION_CONTEXT_DOT + ISBN_LIST;
    public static final String PUBLICATION_CONTEXT_TYPE_KEYWORD =
        ENTITY_PUBLICATION_CONTEXT_DOT + TYPE + DOT + KEYWORD;
    public static final String PUBLICATION_INSTANCE_TYPE =
        ENTITY_PUBLICATION_INSTANCE_DOT + TYPE + DOT + KEYWORD;
    public static final String PUBLICATION_CONTEXT_PUBLISHER = multipleFields(
        ENTITY_PUBLICATION_CONTEXT_DOT + PUBLISHER + DOT + NAME + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + PUBLISHER + DOT + ID + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + PUBLISHER + DOT + ISBN_PREFIX + DOT + KEYWORD
    );
    public static final String ENTITY_DESCRIPTION_REFERENCE_SERIES = multipleFields(
        ENTITY_PUBLICATION_CONTEXT_DOT + SERIES + DOT + "issn" + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + SERIES + DOT + NAME + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + SERIES + DOT + TITLE + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + SERIES + DOT + ID + DOT + KEYWORD
    );

    public static final String ENTITY_DESCRIPTION_REFERENCE_JOURNAL = multipleFields(
        ENTITY_PUBLICATION_CONTEXT_DOT + NAME + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + ID + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + PRINT_ISSN + DOT + KEYWORD,
        ENTITY_PUBLICATION_CONTEXT_DOT + ONLINE_ISSN + DOT + KEYWORD
    );

    public static final String ENTITY_DESCRIPTION_MAIN_TITLE = ENTITY_DESCRIPTION + DOT + MAIN_TITLE;
    public static final String ENTITY_DESCRIPTION_MAIN_TITLE_KEYWORD = ENTITY_DESCRIPTION_MAIN_TITLE + DOT + KEYWORD;
    public static final String FUNDINGS_SOURCE_LABELS = FUNDINGS + DOT + SOURCE + DOT + LABELS + DOT;
    public static final String HANDLE_KEYWORD = multipleFields(
        jsonPath(HANDLE, KEYWORD),
        jsonPath("additionalIdentifiers", VALUE, KEYWORD));
    public static final String RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD =
        RESOURCE_OWNER + DOT + OWNER_AFFILIATION + DOT + KEYWORD;
    public static final String RESOURCE_OWNER_OWNER_KEYWORD = RESOURCE_OWNER + DOT + OWNER + DOT + KEYWORD;
    public static final String ENTITY_TAGS = ENTITY_DESCRIPTION + DOT + TAGS + DOT + KEYWORD;

    //-----------------------------------
    public static final String TOP_LEVEL_ORG_ID = jsonPath(TOP_LEVEL_ORGANIZATIONS, ID, KEYWORD);


    public static final String ENTITY_ABSTRACT = ENTITY_DESCRIPTION + DOT + ABSTRACT;
    public static final String ENTITY_DESCRIPTION_LANGUAGE = ENTITY_DESCRIPTION + DOT + LANGUAGE + DOT + KEYWORD;
    public static final String SCIENTIFIC_INDEX_YEAR = SCIENTIFIC_INDEX + DOT + YEAR;
    public static final String SCIENTIFIC_INDEX_STATUS_KEYWORD = SCIENTIFIC_INDEX + DOT + STATUS_KEYWORD;

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD = multipleFields(
        jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, ENGLISH_CODE, KEYWORD),
        jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, NYNORSK_CODE, KEYWORD),
        jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, BOKMAAL_CODE, KEYWORD),
        jsonPath(CONTRIBUTORS_AFFILIATION_LABELS, SAMI_CODE, KEYWORD)
    );

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION = multipleFields(
        CONTRIBUTORS_AFFILIATION_ID_KEYWORD,
        ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD
    );

    public static final String PUBLICATION_CONTEXT_PATH = jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN = multipleFields(
        jsonPath(PUBLICATION_CONTEXT_PATH, ONLINE_ISSN, KEYWORD),
        jsonPath(PUBLICATION_CONTEXT_PATH, PRINT_ISSN, KEYWORD),
        jsonPath(PUBLICATION_CONTEXT_PATH, SERIES, ONLINE_ISSN, KEYWORD),
        jsonPath(PUBLICATION_CONTEXT_PATH, SERIES, PRINT_ISSN, KEYWORD));

    public static final String FUNDING_IDENTIFIER_KEYWORD = FUNDINGS + DOT + IDENTIFIER_KEYWORD;

    public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER = multipleFields(
        FUNDINGS + DOT + IDENTIFIER_KEYWORD,
        FUNDINGS + DOT + SOURCE + DOT + IDENTIFIER + DOT + KEYWORD
    );

    public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS = multipleFields(
        FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER,
        FUNDINGS_SOURCE_LABELS + ENGLISH_CODE + DOT + KEYWORD,
        FUNDINGS_SOURCE_LABELS + NYNORSK_CODE + DOT + KEYWORD,
        FUNDINGS_SOURCE_LABELS + BOKMAAL_CODE + DOT + KEYWORD,
        FUNDINGS_SOURCE_LABELS + SAMI_CODE + DOT + KEYWORD
    );

    public static final String PARENT_PUBLICATION_ID = multipleFields(
        ENTITY_PUBLICATION_INSTANCE_DOT + "compliesWith" + DOT + KEYWORD,
        ENTITY_PUBLICATION_INSTANCE_DOT + "referencedBy" + DOT + KEYWORD,
        ENTITY_PUBLICATION_INSTANCE_DOT + "corrigendumFor" + DOT + KEYWORD,
        ENTITY_PUBLICATION_INSTANCE_DOT + "manifestations" + DOT + ID + DOT + KEYWORD,
        ENTITY_PUBLICATION_INSTANCE_DOT + ID + DOT + KEYWORD
    );
    public static final String PAINLESS = "painless";

    private static final Map<String, String> facetResourcePaths1 = Map.of(
        TYPE, "/withAppliedFilter/entityDescription/reference/publicationInstance/type",
        SERIES, "/withAppliedFilter/entityDescription/reference/publicationContext/series/id",
        LICENSE, "/withAppliedFilter/associatedArtifacts/license"
    );
    private static final Map<String, String> facetResourcePaths2 = Map.of(
        FILES, "/withAppliedFilter/files",
        PUBLISHER, "/withAppliedFilter/entityDescription/reference/publicationContext/publisher",
        JOURNAL, "/withAppliedFilter/entityDescription/reference/publicationContext/journal/id",
        CONTRIBUTOR, "/withAppliedFilter/entityDescription/contributor/id",
        FUNDING_SOURCE, "/withAppliedFilter/fundings/id",
        TOP_LEVEL_ORGANIZATION, "/withAppliedFilter/topLevelOrganization/id",
        SCIENTIFIC_INDEX, "/withAppliedFilter/scientificIndex/year"
    );

    public static final List<AggregationBuilder> RESOURCES_AGGREGATIONS =
        List.of(
            filesHierarchy(),
            associatedArtifactsHierarchy(),
            entityDescriptionHierarchy(),
            fundingSourceHierarchy(),
            scientificIndexHierarchy(),
            topLevelOrganisationsHierarchy()
        );

    public static final Map<String, String> facetResourcePaths = Stream.of(facetResourcePaths1, facetResourcePaths2)
        .flatMap(map -> map.entrySet().stream())
        .sorted(Map.Entry.comparingByValue())
        .collect(
            LinkedHashMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            LinkedHashMap::putAll
        );

    public static NestedAggregationBuilder topLevelOrganisationsHierarchy() {
        return
            nestedBranchBuilder(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS)
                .subAggregation(
                    branchBuilder(ID, TOP_LEVEL_ORGANIZATIONS, ID, KEYWORD)
                        .subAggregation(
                            labels(TOP_LEVEL_ORGANIZATIONS)
                        )
                );
    }


    public static TermsAggregationBuilder filesHierarchy() {
        return branchBuilder(FILES, jsonPath(FILES_STATUS, KEYWORD));
    }

    public static NestedAggregationBuilder associatedArtifactsHierarchy() {
        return nestedBranchBuilder(ASSOCIATED_ARTIFACTS, ASSOCIATED_ARTIFACTS)
            .subAggregation(license());
    }

    private static NestedAggregationBuilder scientificIndexHierarchy() {
        return nestedBranchBuilder(SCIENTIFIC_INDEX, SCIENTIFIC_INDEX)
            .subAggregation(
                branchBuilder(YEAR, SCIENTIFIC_INDEX, YEAR, KEYWORD)
                    .subAggregation(
                        branchBuilder(NAME, SCIENTIFIC_INDEX, STATUS, KEYWORD)
                    )
            );
    }

    public static NestedAggregationBuilder fundingSourceHierarchy() {
        return
            nestedBranchBuilder(FUNDINGS, FUNDINGS)
                .subAggregation(
                    branchBuilder(ID, FUNDINGS, SOURCE, IDENTIFIER, KEYWORD)
                        .subAggregation(labels(jsonPath(FUNDINGS, SOURCE)))
                        .subAggregation(getReverseNestedAggregationBuilder())
                );
    }

    public static NestedAggregationBuilder entityDescriptionHierarchy() {
        return
            nestedBranchBuilder(ENTITY_DESCRIPTION, ENTITY_DESCRIPTION)
                .subAggregation(contributor())
                .subAggregation(reference());
    }

    private static NestedAggregationBuilder contributor() {
        return nestedBranchBuilder(CONTRIBUTOR, ENTITY_DESCRIPTION, CONTRIBUTORS)
            .subAggregation(
                branchBuilder(ID, ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID, KEYWORD)
                    .subAggregation(
                        branchBuilder(NAME, ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME, KEYWORD)
                    )
            );
    }

    private static NestedAggregationBuilder reference() {
        return
            nestedBranchBuilder(REFERENCE, ENTITY_DESCRIPTION, REFERENCE)
                .subAggregation(
                    publicationContext()
                        .subAggregation(publisher())
                        .subAggregation(series())
                        .subAggregation(journal())
                )
                .subAggregation(
                    publicationInstance()            // Split or just a branch?
                        .subAggregation(instanceType())
                );
    }

    private static NestedAggregationBuilder publicationContext() {
        return
            nestedBranchBuilder(PUBLICATION_CONTEXT, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT);
    }

    private static NestedAggregationBuilder publicationInstance() {
        return
            nestedBranchBuilder(PUBLICATION_INSTANCE, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE);
    }

    private static TermsAggregationBuilder instanceType() {
        return
            branchBuilder(TYPE, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE, KEYWORD);
    }

    private static FilterAggregationBuilder series() {
        var filterBySeriesType =
            filterBranchBuilder(
                SERIES,
                SERIES_AS_TYPE,
                ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, SERIES, TYPE, KEYWORD);

        return filterBySeriesType
            .subAggregation(
                branchBuilder(ID, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, SERIES, IDENTIFIER_KEYWORD)
                    .subAggregation(
                        branchBuilder(NAME, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, SERIES, NAME, KEYWORD)
                    )
            );
    }

    private static AggregationBuilder journal() {
        var filterByJournalType =
            filterBranchBuilder(
                JOURNAL,
                JOURNAL_AS_TYPE,
                ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, TYPE, KEYWORD);

        var seriesName =
            branchBuilder(NAME, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, NAME, KEYWORD);

        return filterByJournalType
            .subAggregation(
                branchBuilder(ID, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, IDENTIFIER_KEYWORD)
                    .subAggregation(seriesName)
            );
    }


    private static TermsAggregationBuilder publisher() {
        return
            branchBuilder(PUBLISHER, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, PUBLISHER, IDENTIFIER_KEYWORD)
                .subAggregation(
                    branchBuilder(NAME, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, PUBLISHER, NAME, KEYWORD)
                );
    }

    private static TermsAggregationBuilder license() {
        return
            branchBuilder(LICENSE, ASSOCIATED_ARTIFACTS, LICENSE, NAME, KEYWORD)
                .subAggregation(labels(jsonPath(ASSOCIATED_ARTIFACTS, LICENSE)))
                .subAggregation(getReverseNestedAggregationBuilder());
    }

    private static ReverseNestedAggregationBuilder getReverseNestedAggregationBuilder() {
        return
            AggregationBuilders.reverseNested(ROOT)
                .subAggregation(uniquePublications());
    }

    private static CardinalityAggregationBuilder uniquePublications() {
        return AggregationBuilders.cardinality(UNIQUE_PUBLICATIONS).field(jsonPath(ID, KEYWORD));
    }


    @JacocoGenerated
    public Constants() {
    }
}