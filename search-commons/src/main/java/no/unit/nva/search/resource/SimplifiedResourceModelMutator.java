package no.unit.nva.search.resource;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import no.unit.nva.search.common.records.JsonNodeMutator;
import no.unit.nva.search.resource.response.Affiliation;
import no.unit.nva.search.resource.response.Contributor;
import no.unit.nva.search.resource.response.Identity;
import no.unit.nva.search.resource.response.OtherIdentifiers;
import no.unit.nva.search.resource.response.PublicationDate;
import no.unit.nva.search.resource.response.PublishingDetails;
import no.unit.nva.search.resource.response.RecordMetadata;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import no.unit.nva.search.resource.response.ResourceSearchResponse.Builder;
import no.unit.nva.search.resource.response.Series;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimplifiedResourceModelMutator implements JsonNodeMutator {

    public static final String ID = "id";
    public static final String AFFILIATIONS = "affiliations";
    public static final String PUBLICATION_INSTANCE = "publicationInstance";
    public static final String TYPE = "type";
    public static final String ABSTRACT = "abstract";
    public static final String MAIN_TITLE = "mainTitle";
    public static final String DESCRIPTION = "description";
    public static final String ALTERNATIVE_TITLES = "alternativeTitles";
    public static final String PUBLICATION_DATE = "publicationDate";
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";
    public static final String CONTRIBUTORS_COUNT = "contributorsCount";
    public static final String ONLINE_ISSN = "onlineIssn";
    public static final String PRINT_ISSN = "printIssn";
    public static final String ISBN_LIST = "isbnList";
    public static final String ADDITIONAL_IDENTIFIERS = "additionalIdentifiers";
    public static final String SOURCE_NAME = "sourceName";
    public static final String HANDLE_IDENTIFIER = "HandleIdentifier";
    public static final String VALUE = "value";
    public static final String SCOPUS_IDENTIFIER = "ScopusIdentifier";
    public static final String CRISTIN_IDENTIFIER = "CristinIdentifier";
    public static final String STATUS = "status";
    public static final String CREATED_DATE = "createdDate";
    public static final String MODIFIED_DATE = "modifiedDate";
    public static final String PUBLISHED_DATE = "publishedDate";
    public static final String NAME = "name";
    public static final String DOI = "doi";
    public static final String CONTRIBUTORS_PREVIEW = "contributorsPreview";
    public static final String CORRESPONDING_AUTHOR = "correspondingAuthor";
    public static final String IDENTITY = "identity";
    public static final String SEQUENCE = "sequence";
    public static final String ROLE = "role";
    public static final String MANIFESTATIONS = "manifestations";
    private final ObjectMapper objectMapper = dtoObjectMapper.copy();
    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String REFERENCE = "reference";
    public static final String PUBLICATION_CONTEXT = "publicationContext";
    public static final String SERIES = "series";

    public SimplifiedResourceModelMutator() {
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String path(String... path) {
        return Arrays.stream(path).collect(Collectors.joining("."));
    }

    public static List<String> getIncludedFields() {
        return List.of(
                ID,
                STATUS,
                CREATED_DATE,
                MODIFIED_DATE,
                PUBLISHED_DATE,
                path(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE),
                path(ENTITY_DESCRIPTION, REFERENCE, DOI),
                path(
                        ENTITY_DESCRIPTION,
                        REFERENCE,
                        PUBLICATION_CONTEXT), // TODO: Narrow down further?
                path(ENTITY_DESCRIPTION, DESCRIPTION),
                path(ENTITY_DESCRIPTION, MAIN_TITLE),
                path(ENTITY_DESCRIPTION, ABSTRACT),
                path(ENTITY_DESCRIPTION, ALTERNATIVE_TITLES),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_COUNT),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, AFFILIATIONS, ID),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, AFFILIATIONS, TYPE),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, CORRESPONDING_AUTHOR, ID),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, IDENTITY, ID),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, IDENTITY, TYPE),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, ROLE, TYPE),
                path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, SEQUENCE),
                path(ENTITY_DESCRIPTION, PUBLICATION_DATE));
    }

    @Override
    public JsonNode transform(JsonNode source) {
        return (JsonNode)
                attempt(() -> objectMapper.valueToTree(transformToDto(source))).orElseThrow();
    }

    @NotNull
    private static PublicationDate mutatePublicationDate(JsonNode source) {
        return new PublicationDate(
                source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(YEAR).textValue(),
                source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(MONTH).textValue(),
                source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(DAY).textValue());
    }

    private ResourceSearchResponse transformToDto(JsonNode source) throws IOException {
        return new Builder()
                .withId(source.path(ID).textValue())
                .withType(
                        source.path(ENTITY_DESCRIPTION)
                                .path(REFERENCE)
                                .path(PUBLICATION_INSTANCE)
                                .path(TYPE)
                                .textValue())
                .withMainTitle(source.path(ENTITY_DESCRIPTION).path(MAIN_TITLE).textValue())
                .withMainLanguageAbstract(
                        source.path(ENTITY_DESCRIPTION).path(ABSTRACT).textValue())
                .withDescription(source.path(ENTITY_DESCRIPTION).path(DESCRIPTION).textValue())
                .withAlternativeTitles(mutateAlternativeTitles(source))
                .withPublicationDate(mutatePublicationDate(source))
                .withContributorsPreview(mutateContributorsPreview(source))
                .withContributorsCount(
                        source.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS_COUNT).asInt())
                .withPublishingDetails(mutatePublishingDetails(source))
                .withOtherIdentifiers(mutateOtherIdentifiers(source))
                .withRecordMetadata(mutateRecordMetadata(source))
                .build();
    }

    @Nullable
    private Map<String, String> mutateAlternativeTitles(JsonNode source) throws IOException {
        return source.path(ENTITY_DESCRIPTION).has(ALTERNATIVE_TITLES)
                ? jsonNodeMapToMap(source.path(ENTITY_DESCRIPTION).path(ALTERNATIVE_TITLES))
                : null;
    }

    private Map<String, String> jsonNodeMapToMap(JsonNode source) throws IOException {
        return objectMapper.convertValue(source, Map.class);
    }

    private OtherIdentifiers mutateOtherIdentifiers(JsonNode source) throws IOException {
        var issns =
                Stream.of(
                                source.path(ENTITY_DESCRIPTION)
                                        .path(REFERENCE)
                                        .path(PUBLICATION_CONTEXT)
                                        .path(ONLINE_ISSN)
                                        .textValue(),
                                source.path(ENTITY_DESCRIPTION)
                                        .path(REFERENCE)
                                        .path(PUBLICATION_CONTEXT)
                                        .path(PRINT_ISSN)
                                        .textValue(),
                                source.path(ENTITY_DESCRIPTION)
                                        .path(REFERENCE)
                                        .path(PUBLICATION_CONTEXT)
                                        .path(SERIES)
                                        .path(ONLINE_ISSN)
                                        .textValue(),
                                source.path(ENTITY_DESCRIPTION)
                                        .path(REFERENCE)
                                        .path(PUBLICATION_CONTEXT)
                                        .path(SERIES)
                                        .path(PRINT_ISSN)
                                        .textValue())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        var isbnsInSourceNose =
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_CONTEXT)
                        .path(ISBN_LIST);

        List<String> isbnsInManifestations = new ArrayList<>();
        var manifestations =
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_INSTANCE)
                        .path(MANIFESTATIONS);

        if (!manifestations.isMissingNode() && manifestations.isArray()) {
            manifestations
                    .iterator()
                    .forEachRemaining(
                            manifest -> {
                                if (!manifest.get(ISBN_LIST).isMissingNode()) {
                                    List<String> isbns =
                                            (List<String>)
                                                    attempt(
                                                                    () ->
                                                                            objectMapper
                                                                                    .readerForListOf(
                                                                                            String
                                                                                                    .class)
                                                                                    .readValue(
                                                                                            manifest
                                                                                                    .get(
                                                                                                            ISBN_LIST)))
                                                            .orElseThrow();
                                    isbnsInManifestations.addAll(isbns);
                                }
                            });
        }

        List<String> isbnsInSource =
                isbnsInSourceNose.isMissingNode()
                        ? Collections.emptyList()
                        : objectMapper.readerForListOf(String.class).readValue(isbnsInSourceNose);

        var isbns = Stream.concat(isbnsInSource.stream(), isbnsInManifestations.stream()).toList();

        var handleIdentifiers = new ArrayList<String>();
        var cristinIdentifiers = new ArrayList<String>();
        var scopusIdentifiers = new ArrayList<String>();
        source.path(ADDITIONAL_IDENTIFIERS)
                .iterator()
                .forEachRemaining(
                        i -> {
                            switch (i.path(SOURCE_NAME).textValue()) {
                                case HANDLE_IDENTIFIER:
                                    handleIdentifiers.add(i.path(VALUE).textValue());
                                    break;
                                case SCOPUS_IDENTIFIER:
                                    scopusIdentifiers.add(i.path(VALUE).textValue());
                                    break;
                                case CRISTIN_IDENTIFIER:
                                    cristinIdentifiers.add(i.path(VALUE).textValue());
                                    break;
                                default:
                                    break;
                            }
                        });

        return new OtherIdentifiers(
                new HashSet<>(scopusIdentifiers),
                new HashSet<>(cristinIdentifiers),
                new HashSet<>(handleIdentifiers),
                new HashSet<>(issns),
                new HashSet<>(isbns));
    }

    private RecordMetadata mutateRecordMetadata(JsonNode source) {
        return new RecordMetadata(
                source.path(STATUS).textValue(), source.path(CREATED_DATE).textValue(),
                source.path(MODIFIED_DATE).textValue(), source.path(PUBLISHED_DATE).textValue());
    }

    private PublishingDetails mutatePublishingDetails(JsonNode source) {
        return new PublishingDetails(
                uriFromText(
                        source.path(ENTITY_DESCRIPTION)
                                .path(REFERENCE)
                                .path(PUBLICATION_CONTEXT)
                                .path(ID)
                                .textValue()),
                mutatePublicationContextType(source),
                mutateSeries(source),
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_CONTEXT)
                        .path(NAME)
                        .textValue(),
                uriFromText(source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(DOI).textValue()));
    }

    private String mutatePublicationContextType(JsonNode source) {
        return source.path(ENTITY_DESCRIPTION)
                .path(REFERENCE)
                .path(PUBLICATION_CONTEXT)
                .path(TYPE)
                .textValue();
    }

    private Series mutateSeries(JsonNode source) {
        var series =
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_CONTEXT)
                        .path(SERIES);

        if (series.isMissingNode()) {
            return null;
        }
        return new Series(series.path(ID).textValue(), series.path(NAME).textValue());
    }

    private List<Contributor> mutateContributorsPreview(JsonNode source) {
        var contributors = new ArrayList();
        source.path(ENTITY_DESCRIPTION)
                .path(CONTRIBUTORS_PREVIEW)
                .iterator()
                .forEachRemaining(
                        contributorNode -> {
                            var affiliationNode = contributorNode.path(AFFILIATIONS);
                            var affiliations = new HashSet<Affiliation>();
                            if (!affiliationNode.isMissingNode()) {
                                affiliationNode
                                        .iterator()
                                        .forEachRemaining(
                                                aff -> {
                                                    affiliations.add(
                                                            new Affiliation(
                                                                    aff.path(ID).textValue(),
                                                                    aff.path(TYPE).textValue()));
                                                });
                            }

                            contributors.add(
                                    new Contributor(
                                            affiliations,
                                            contributorNode.path(CORRESPONDING_AUTHOR).asBoolean(),
                                            new Identity(
                                                    uriFromText(
                                                            contributorNode
                                                                    .path(IDENTITY)
                                                                    .path(ID)
                                                                    .textValue()),
                                                    contributorNode
                                                            .path(IDENTITY)
                                                            .path(NAME)
                                                            .textValue()),
                                            contributorNode.path(ROLE).path(TYPE).textValue(),
                                            contributorNode.path(SEQUENCE).asInt()));
                        });
        return contributors;
    }

    private URI uriFromText(String text) {
        return Objects.isNull(text) ? null : URI.create(text);
    }
}
