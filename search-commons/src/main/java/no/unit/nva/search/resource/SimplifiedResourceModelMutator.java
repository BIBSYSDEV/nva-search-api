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

import java.io.IOException;
import java.util.ArrayList;
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
    public static final String JOURNAL = "Journal";
    public static final String PUBLISHER = "Publisher";
    public static final String CONTRIBUTORS_PREVIEW = "contributorsPreview";
    public static final String CORRESPONDING_AUTHOR = "correspondingAuthor";
    public static final String IDENTITY = "identity";
    public static final String SEQUENCE = "sequence";
    public static final String ROLE = "role";
    private final ObjectMapper objectMapper = dtoObjectMapper.copy();
    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String REFERENCE = "reference";
    public static final String PUBLICATION_CONTEXT = "publicationContext";
    public static final String SERIES = "series";

    public SimplifiedResourceModelMutator() {
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static List<String> getIncludedFields() {
        return List.of(ID, REFERENCE, ENTITY_DESCRIPTION);
    }

    @Override
    public JsonNode transform(JsonNode source) {
        return (JsonNode)
                attempt(() -> objectMapper.valueToTree(transformToDto(source))).orElseThrow();
    }

    private ResourceSearchResponse transformToDto(JsonNode source) throws IOException {
        return new Builder()
                .withId(source.path(ID).textValue())
                .withType(source.path(REFERENCE).path(PUBLICATION_INSTANCE).path(TYPE).textValue())
                .withMainTitle(source.path(ENTITY_DESCRIPTION).path(MAIN_TITLE).textValue())
                .withMainLanguageAbstract(
                        source.path(ENTITY_DESCRIPTION).path(ABSTRACT).textValue())
                .withDescription(source.path(DESCRIPTION).textValue())
                .withAlternativeTitles(
                        source.path(ENTITY_DESCRIPTION).has(ALTERNATIVE_TITLES)
                                ? jsonNodeMapToValueList(
                                        source.path(ENTITY_DESCRIPTION).path(ALTERNATIVE_TITLES))
                                : null)
                .withPublicationDate(
                        new PublicationDate(
                                source.path(ENTITY_DESCRIPTION)
                                        .path(PUBLICATION_DATE)
                                        .path(YEAR)
                                        .textValue(),
                                source.path(ENTITY_DESCRIPTION)
                                        .path(PUBLICATION_DATE)
                                        .path(MONTH)
                                        .textValue(),
                                source.path(ENTITY_DESCRIPTION)
                                        .path(PUBLICATION_DATE)
                                        .path(DAY)
                                        .textValue()))
                .withContributorsPreview(mutateContributorsPreview(source))
                .withContributorsCount(
                        source.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS_COUNT).asInt())
                .withPublishingDetails(mutatePublishingDetails(source))
                .withOtherIdentifiers(mutateOtherIdentifiers(source))
                .withRecordMetadata(mutateRecordMetadata(source))
                .build();
    }

    private List<String> jsonNodeMapToValueList(JsonNode source) throws IOException {
        Map<String, String> map = objectMapper.convertValue(source, Map.class);
        return map.values().stream().collect(Collectors.toList());
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

        var isbnsInSource =
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_CONTEXT)
                        .path(ISBN_LIST);
        List<String> isbns =
                isbnsInSource.isMissingNode()
                        ? Collections.emptyList()
                        : objectMapper.readerForListOf(String.class).readValue(isbnsInSource);

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
                new HashSet<>(isbns),
                new HashSet<>(issns));
    }

    private RecordMetadata mutateRecordMetadata(JsonNode source) {
        return new RecordMetadata(
                source.path(STATUS).textValue(), source.path(CREATED_DATE).textValue(),
                source.path(MODIFIED_DATE).textValue(), source.path(PUBLISHED_DATE).textValue());
    }

    private PublishingDetails mutatePublishingDetails(JsonNode source) {
        return new PublishingDetails(
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_CONTEXT)
                        .path(ID)
                        .textValue(),
                mutateJournalOrPublisher(source),
                mutateSeries(source),
                source.path(ENTITY_DESCRIPTION)
                        .path(REFERENCE)
                        .path(PUBLICATION_CONTEXT)
                        .path(NAME)
                        .textValue(),
                source.path(REFERENCE).path(DOI).textValue());
    }

    private String mutateJournalOrPublisher(JsonNode source) {
        return JOURNAL.equals(
                        source.path(ENTITY_DESCRIPTION)
                                .path(REFERENCE)
                                .path(PUBLICATION_CONTEXT)
                                .path(TYPE)
                                .textValue())
                ? JOURNAL
                : PUBLISHER;
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
                        c -> {
                            var affiliationNode = c.path(AFFILIATIONS);
                            var affiliation =
                                    affiliationNode.isMissingNode()
                                            ? null
                                            : new Affiliation(
                                                    affiliationNode.get(0).path(ID).textValue(),
                                                    affiliationNode.get(0).path(TYPE).textValue());
                            contributors.add(
                                    new Contributor(
                                            affiliation,
                                            c.path(CORRESPONDING_AUTHOR).asBoolean(),
                                            new Identity(
                                                    c.path(IDENTITY).path(ID).textValue(),
                                                    c.path(IDENTITY).path(TYPE).textValue(),
                                                    c.path(IDENTITY).path(NAME).textValue()),
                                            c.path(ROLE).textValue(),
                                            c.path(SEQUENCE).asInt(),
                                            c.path(TYPE).textValue()));
                        });
        return contributors;
    }
}
