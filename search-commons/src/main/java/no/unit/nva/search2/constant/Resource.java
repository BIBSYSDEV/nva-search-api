package no.unit.nva.search2.constant;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Functions.generateAssociatedArtifactsAggregation;
import static no.unit.nva.search2.constant.Functions.generateFundingSource;
import static no.unit.nva.search2.constant.Functions.generateLabelsAggregation;
import static no.unit.nva.search2.constant.Functions.generateObjectLabelsAggregation;
import static no.unit.nva.search2.constant.Functions.jsonPath;
import static no.unit.nva.search2.constant.Words.ABSTRACT;
import static no.unit.nva.search2.constant.Words.ADMINSTRATIVE_AGREEMENT;
import static no.unit.nva.search2.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search2.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.constant.Words.DOI;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDING;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.HAS_FILE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.IDENTITY;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.MAIN_TITLE;
import static no.unit.nva.search2.constant.Words.NAME;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.ORC_ID;
import static no.unit.nva.search2.constant.Words.OWNER;
import static no.unit.nva.search2.constant.Words.OWNER_AFFILIATION;
import static no.unit.nva.search2.constant.Words.PIPE;
import static no.unit.nva.search2.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search2.constant.Words.PUBLICATION_DATE;
import static no.unit.nva.search2.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search2.constant.Words.PUBLISHED_FILE;
import static no.unit.nva.search2.constant.Words.PUBLISHER;
import static no.unit.nva.search2.constant.Words.REFERENCE;
import static no.unit.nva.search2.constant.Words.RESOURCE_OWNER;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.SERIES;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.constant.Words.TAGS;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.constant.Words.YEAR;
import static no.unit.nva.search2.enums.ResourceParameter.TITLE;
import static nva.commons.core.StringUtils.EMPTY_STRING;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import no.unit.nva.search2.enums.ResourceSort;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.IncludeExclude;

public final class Resource {

    public static final String DEFAULT_RESOURCE_SORT =
        ResourceSort.PUBLISHED_DATE.name().toLowerCase(Locale.getDefault());
    public static final String IDENTIFIER_KEYWORD = IDENTIFIER + DOT + KEYWORD;
    public static final String ENTITY_CONTRIBUTORS_DOT = ENTITY_DESCRIPTION + DOT + CONTRIBUTORS + DOT;
    public static final String ENTITY_PUBLICATION_CONTEXT_DOT =
        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + PUBLICATION_CONTEXT + DOT;
    public static final String ENTITY_PUBLICATION_INSTANCE_DOT =
        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + PUBLICATION_INSTANCE + DOT;
    public static final String CONTRIBUTORS_AFFILIATION_ID_KEYWORD =
        ENTITY_CONTRIBUTORS_DOT + AFFILIATIONS + DOT + ID + DOT + KEYWORD;
    public static final String CONTRIBUTORS_AFFILIATION_LABELS =
        ENTITY_CONTRIBUTORS_DOT + AFFILIATIONS + DOT + LABELS + DOT;
    public static final String CONTRIBUTORS_IDENTITY_ID =
        ENTITY_CONTRIBUTORS_DOT + IDENTITY + DOT + ID + DOT + KEYWORD;
    public static final String CONTRIBUTORS_IDENTITY_NAME_KEYWORD =
        ENTITY_CONTRIBUTORS_DOT + IDENTITY + DOT + NAME + DOT + KEYWORD;
    public static final String CONTRIBUTORS_IDENTITY_ORC_ID_KEYWORD =
        ENTITY_CONTRIBUTORS_DOT + IDENTITY + DOT + ORC_ID + DOT + KEYWORD;
    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
        ENTITY_DESCRIPTION + DOT + PUBLICATION_DATE + DOT + YEAR;
    public static final String REFERENCE_DOI_KEYWORD =
        ENTITY_DESCRIPTION + DOT + REFERENCE + DOT + DOI + DOT + KEYWORD + PIPE + DOI + DOT + KEYWORD;
    public static final String VISIBLE_FOR_NON_OWNER = ASSOCIATED_ARTIFACTS + DOT + "visibleForNonOwner";
    public static final String PUBLICATION_CONTEXT_ISBN_LIST =
        ENTITY_PUBLICATION_CONTEXT_DOT + "isbnList";
    public static final String PUBLICATION_CONTEXT_ONLINE_ISSN_KEYWORD =
        ENTITY_PUBLICATION_CONTEXT_DOT + "onlineIssn" + DOT + KEYWORD;
    public static final String PUBLICATION_CONTEXT_PRINT_ISSN_KEYWORD =
        ENTITY_PUBLICATION_CONTEXT_DOT + "printIssn" + DOT + KEYWORD;
    public static final String PUBLICATION_CONTEXT_TYPE_KEYWORD =
        ENTITY_PUBLICATION_CONTEXT_DOT + TYPE + DOT + KEYWORD;
    public static final String PUBLICATION_INSTANCE_TYPE =
        ENTITY_PUBLICATION_INSTANCE_DOT + TYPE + DOT + KEYWORD;
    public static final String ENTITY_DESCRIPTION_MAIN_TITLE = ENTITY_DESCRIPTION + DOT + MAIN_TITLE;
    public static final String ENTITY_DESCRIPTION_MAIN_TITLE_KEYWORD = ENTITY_DESCRIPTION_MAIN_TITLE + DOT + KEYWORD;
    public static final String FUNDINGS_SOURCE_LABELS = FUNDINGS + DOT + SOURCE + DOT + LABELS + DOT;
    public static final String RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD =
        RESOURCE_OWNER + DOT + OWNER_AFFILIATION + DOT + KEYWORD;
    public static final String RESOURCE_OWNER_OWNER_KEYWORD = RESOURCE_OWNER + DOT + OWNER + DOT + KEYWORD;

    public static final String ENTITY_TAGS = ENTITY_DESCRIPTION + DOT + TAGS + DOT + KEYWORD;
    public static final String TOP_LEVEL_ORG_ID = TOP_LEVEL_ORGANIZATIONS + DOT + ID + DOT + KEYWORD;
    public static final String ENTITY_ABSTRACT = ENTITY_DESCRIPTION + DOT + ABSTRACT;
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD =
        CONTRIBUTORS_AFFILIATION_LABELS + ENGLISH_CODE + DOT + KEYWORD + PIPE
        + CONTRIBUTORS_AFFILIATION_LABELS + NYNORSK_CODE + DOT + KEYWORD + PIPE
        + CONTRIBUTORS_AFFILIATION_LABELS + BOKMAAL_CODE + DOT + KEYWORD + PIPE
        + CONTRIBUTORS_AFFILIATION_LABELS + SAMI_CODE + DOT + KEYWORD;

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION =
        CONTRIBUTORS_AFFILIATION_ID_KEYWORD + PIPE
        + ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS_KEYWORD;

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISSN =
        PUBLICATION_CONTEXT_ONLINE_ISSN_KEYWORD + PIPE + PUBLICATION_CONTEXT_PRINT_ISSN_KEYWORD;

    public static final String FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER =
        FUNDINGS + DOT + IDENTIFIER_KEYWORD + PIPE + FUNDINGS + DOT + SOURCE + DOT + IDENTIFIER + DOT + KEYWORD;

    public static final String FUNDINGS_SOURCE_IDENTIFIER_FUNDINGS_SOURCE_LABELS =
        FUNDINGS_IDENTIFIER_FUNDINGS_SOURCE_IDENTIFIER + PIPE
        + FUNDINGS_SOURCE_LABELS + ENGLISH_CODE + DOT + KEYWORD + PIPE
        + FUNDINGS_SOURCE_LABELS + NYNORSK_CODE + DOT + KEYWORD + PIPE
        + FUNDINGS_SOURCE_LABELS + BOKMAAL_CODE + DOT + KEYWORD + PIPE
        + FUNDINGS_SOURCE_LABELS + SAMI_CODE + DOT + KEYWORD;

    public static final String PARENT_PUBLICATION_ID =
        ENTITY_PUBLICATION_INSTANCE_DOT + "corrigendumFor" + DOT + KEYWORD + PIPE
        + ENTITY_PUBLICATION_INSTANCE_DOT + "manifestations" + DOT + ID + DOT + KEYWORD + PIPE
        + ENTITY_PUBLICATION_INSTANCE_DOT + ID + DOT + KEYWORD;


    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        RESOURCES_AGGREGATIONS = List.of(
        nestedTermBuilder(ENTITY_DESCRIPTION, ENTITY_DESCRIPTION)
            .subAggregation(generateContributor())
            .subAggregation(generateReference()),
        nestedTermBuilder(FUNDING,FUNDINGS)
            .subAggregation(
                termBuilder(ID,null, FUNDINGS, SOURCE, IDENTIFIER, KEYWORD)
                .subAggregation(
                    generateLabelsAggregation(jsonPath(FUNDINGS, SOURCE))
                )
            ),
        nestedTermBuilder(ASSOCIATED_ARTIFACTS+1, ASSOCIATED_ARTIFACTS)
              .subAggregation(termBuilder(HAS_FILE, new IncludeExclude(PUBLISHED_FILE, EMPTY_STRING),
                  ASSOCIATED_ARTIFACTS, ADMINSTRATIVE_AGREEMENT)
            ),
        generateAssociatedArtifactsAggregation(),
//        generateAssociatedArtifactsAggregation(),
//        generateFundingSource(),
        generateObjectLabelsAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS)
    );


    public static NestedAggregationBuilder generateContributor() {
        return new NestedAggregationBuilder(CONTRIBUTOR, jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS))
            .subAggregation(
                termBuilder(ID,null, ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID, KEYWORD)
                    .subAggregation(
                        termBuilder(NAME, null, ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME, KEYWORD))
            );
    }



    private static AbstractAggregationBuilder<?> generateReference() {
        return nestedTermBuilder(REFERENCE, ENTITY_DESCRIPTION, REFERENCE)
            .subAggregation(
                nestedTermBuilder(PUBLICATION_CONTEXT, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT)
                    .subAggregation(
                        termBuilder(PUBLISHER,null,  ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, PUBLISHER, NAME,
                            KEYWORD)
                    )
                    .subAggregation(
                        termBuilder(SERIES,null,  ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, SERIES, "title", KEYWORD)
                    )
            )
            .subAggregation(
                nestedTermBuilder(PUBLICATION_INSTANCE, ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE)
                    .subAggregation(
                        termBuilder(TYPE,null,        ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE,
                            KEYWORD)
                    )
            );
    }


    private static AbstractAggregationBuilder<?> termBuilder(String name, IncludeExclude include, String ... fields) {
        var builder = AggregationBuilders
            .terms(name)
            .field(jsonPath(fields))
            .size(Defaults.DEFAULT_AGGREGATION_SIZE);
        if (nonNull(include)) {
            builder.includeExclude(include);
        }
        return builder;
    }

    private static AbstractAggregationBuilder<?> nestedTermBuilder(String name, String ... fields) {
        return new NestedAggregationBuilder(name, jsonPath(fields));
    }



    @JacocoGenerated
    public Resource() {
    }
}
