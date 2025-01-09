package no.unit.nva.search.resource;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.constants.Words.ABSTRACT;
import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.CREATED_DATE;
import static no.unit.nva.constants.Words.DAY;
import static no.unit.nva.constants.Words.DOI;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.IDENTIFIER;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.ISBN_LIST;
import static no.unit.nva.constants.Words.MAIN_TITLE;
import static no.unit.nva.constants.Words.MODIFIED_DATE;
import static no.unit.nva.constants.Words.MONTH;
import static no.unit.nva.constants.Words.NAME;
import static no.unit.nva.constants.Words.ONLINE_ISSN;
import static no.unit.nva.constants.Words.ORC_ID;
import static no.unit.nva.constants.Words.PRINT_ISSN;
import static no.unit.nva.constants.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.constants.Words.PUBLICATION_DATE;
import static no.unit.nva.constants.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.constants.Words.PUBLISHED_DATE;
import static no.unit.nva.constants.Words.PUBLISHER;
import static no.unit.nva.constants.Words.REFERENCE;
import static no.unit.nva.constants.Words.ROLE;
import static no.unit.nva.constants.Words.SCIENTIFIC_VALUE;
import static no.unit.nva.constants.Words.SERIES;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.constants.Words.TYPE;
import static no.unit.nva.constants.Words.VALUE;
import static no.unit.nva.constants.Words.YEAR;
import static no.unit.nva.search.resource.Constants.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.resource.Constants.ALTERNATIVE_TITLES;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_COUNT;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_PREVIEW;
import static no.unit.nva.search.resource.Constants.CRISTIN_IDENTIFIER;
import static no.unit.nva.search.resource.Constants.GLOBAL_EXCLUDED_FIELDS;
import static no.unit.nva.search.resource.Constants.MANIFESTATIONS;
import static no.unit.nva.search.resource.Constants.SCOPUS_IDENTIFIER;
import static no.unit.nva.search.resource.Constants.SEQUENCE;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.common.records.JsonNodeMutator;
import no.unit.nva.search.resource.response.Affiliation;
import no.unit.nva.search.resource.response.Contributor;
import no.unit.nva.search.resource.response.Identity;
import no.unit.nva.search.resource.response.OtherIdentifiers;
import no.unit.nva.search.resource.response.PublicationDate;
import no.unit.nva.search.resource.response.Publisher;
import no.unit.nva.search.resource.response.PublishingDetails;
import no.unit.nva.search.resource.response.RecordMetadata;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import no.unit.nva.search.resource.response.ResourceSearchResponse.Builder;
import no.unit.nva.search.resource.response.Series;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimplifiedMutator implements JsonNodeMutator {

  public static final String HANDLE_IDENTIFIER = "HandleIdentifier";
  public static final String CORRESPONDING_AUTHOR = "correspondingAuthor";
  private final ObjectMapper objectMapper = dtoObjectMapper.copy();

  public SimplifiedMutator() {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static String path(String... path) {
    return String.join(DOT, path);
  }

  public static List<String> getExcludedFields() {
    return GLOBAL_EXCLUDED_FIELDS;
  }

  public static List<String> getIncludedFields() {
    return List.of(
        ID,
        IDENTIFIER,
        STATUS,
        CREATED_DATE,
        MODIFIED_DATE,
        PUBLISHED_DATE,
        path(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE),
        path(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, MANIFESTATIONS, ISBN_LIST),
        path(ENTITY_DESCRIPTION, REFERENCE, DOI),
        path(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT), // TODO: Narrow down further?
        path(ENTITY_DESCRIPTION, Constants.DESCRIPTION),
        path(ENTITY_DESCRIPTION, MAIN_TITLE),
        path(ENTITY_DESCRIPTION, ABSTRACT),
        path(ENTITY_DESCRIPTION, ALTERNATIVE_TITLES),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_COUNT),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, AFFILIATIONS, ID),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, AFFILIATIONS, TYPE),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, CORRESPONDING_AUTHOR),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, IDENTITY, ID),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, IDENTITY, NAME),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, IDENTITY, ORC_ID),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, ROLE, TYPE),
        path(ENTITY_DESCRIPTION, CONTRIBUTORS_PREVIEW, SEQUENCE),
        path(ENTITY_DESCRIPTION, PUBLICATION_DATE),
        path(ADDITIONAL_IDENTIFIERS, TYPE),
        path(ADDITIONAL_IDENTIFIERS, VALUE));
  }

  @NotNull
  private static PublicationDate mutatePublicationDate(JsonNode source) {
    return new PublicationDate(
        source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(YEAR).textValue(),
        source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(MONTH).textValue(),
        source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(DAY).textValue());
  }

  @Override
  public JsonNode transform(JsonNode source) {
    return (JsonNode) attempt(() -> objectMapper.valueToTree(transformToDto(source))).orElseThrow();
  }

  private ResourceSearchResponse transformToDto(JsonNode source) throws IOException {
    return new Builder()
        .withId(uriFromText(source.path(ID).textValue()))
        .withIdentifier(source.path(IDENTIFIER).textValue())
        .withType(
            source
                .path(ENTITY_DESCRIPTION)
                .path(REFERENCE)
                .path(PUBLICATION_INSTANCE)
                .path(TYPE)
                .textValue())
        .withMainTitle(source.path(ENTITY_DESCRIPTION).path(MAIN_TITLE).textValue())
        .withMainLanguageAbstract(source.path(ENTITY_DESCRIPTION).path(ABSTRACT).textValue())
        .withDescription(source.path(ENTITY_DESCRIPTION).path(Constants.DESCRIPTION).textValue())
        .withAlternativeTitles(mutateAlternativeTitles(source))
        .withPublicationDate(mutatePublicationDate(source))
        .withContributorsPreview(mutateContributorsPreview(source))
        .withContributorsCount(source.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS_COUNT).asInt())
        .withPublishingDetails(mutatePublishingDetails(source))
        .withOtherIdentifiers(mutateOtherIdentifiers(source))
        .withRecordMetadata(mutateRecordMetadata(source))
        .build();
  }

  @Nullable
  private Map<String, String> mutateAlternativeTitles(JsonNode source) {
    return source.path(ENTITY_DESCRIPTION).has(ALTERNATIVE_TITLES)
        ? jsonNodeMapToMap(source.path(ENTITY_DESCRIPTION).path(ALTERNATIVE_TITLES))
        : null;
  }

  private Map<String, String> jsonNodeMapToMap(JsonNode source) {
    return objectMapper.convertValue(source, Map.class);
  }

  private OtherIdentifiers mutateOtherIdentifiers(JsonNode source) throws IOException {
    var issns =
        Stream.of(
                source
                    .path(ENTITY_DESCRIPTION)
                    .path(REFERENCE)
                    .path(PUBLICATION_CONTEXT)
                    .path(ONLINE_ISSN)
                    .textValue(),
                source
                    .path(ENTITY_DESCRIPTION)
                    .path(REFERENCE)
                    .path(PUBLICATION_CONTEXT)
                    .path(PRINT_ISSN)
                    .textValue(),
                source
                    .path(ENTITY_DESCRIPTION)
                    .path(REFERENCE)
                    .path(PUBLICATION_CONTEXT)
                    .path(SERIES)
                    .path(ONLINE_ISSN)
                    .textValue(),
                source
                    .path(ENTITY_DESCRIPTION)
                    .path(REFERENCE)
                    .path(PUBLICATION_CONTEXT)
                    .path(SERIES)
                    .path(PRINT_ISSN)
                    .textValue())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    var isbnsInSourceNode =
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(ISBN_LIST);

    List<String> isbnsInManifestations = new ArrayList<>();
    var manifestations =
        source
            .path(ENTITY_DESCRIPTION)
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
                                          .readerForListOf(String.class)
                                          .readValue(manifest.get(ISBN_LIST)))
                              .orElseThrow();
                  isbnsInManifestations.addAll(isbns);
                }
              });
    }

    List<String> isbnsInSource =
        isbnsInSourceNode.isMissingNode()
            ? Collections.emptyList()
            : objectMapper.readerForListOf(String.class).readValue(isbnsInSourceNode);

    var isbns = Stream.concat(isbnsInSource.stream(), isbnsInManifestations.stream()).toList();

    var handleIdentifiers = new ArrayList<String>();
    var cristinIdentifiers = new ArrayList<String>();
    var scopusIdentifiers = new ArrayList<String>();
    source
        .path(ADDITIONAL_IDENTIFIERS)
        .iterator()
        .forEachRemaining(
            i -> {
              switch (i.path(TYPE).textValue()) {
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
            source
                .path(ENTITY_DESCRIPTION)
                .path(REFERENCE)
                .path(PUBLICATION_CONTEXT)
                .path(ID)
                .textValue()),
        mutatePublicationContextType(source),
        mutateSeries(source),
        source
            .path(ENTITY_DESCRIPTION)
            .path(REFERENCE)
            .path(PUBLICATION_CONTEXT)
            .path(NAME)
            .textValue(),
        uriFromText(source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(DOI).textValue()),
        mutatePublisher(source));
  }

  private String mutatePublicationContextType(JsonNode source) {
    return source
        .path(ENTITY_DESCRIPTION)
        .path(REFERENCE)
        .path(PUBLICATION_CONTEXT)
        .path(TYPE)
        .textValue();
  }

  private Series mutateSeries(JsonNode source) {
    var series =
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(SERIES);

    if (series.isMissingNode()) {
      return null;
    }
    return new Series(
        uriFromText(series.path(ID).textValue()),
        series.path(NAME).textValue(),
        series.path(SCIENTIFIC_VALUE).textValue());
  }

  private Publisher mutatePublisher(JsonNode source) {
    var publisher =
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(PUBLISHER);

    if (publisher.isMissingNode()) {
      return null;
    }
    return new Publisher(
        uriFromText(publisher.path(ID).textValue()),
        publisher.path(NAME).textValue(),
        publisher.path(SCIENTIFIC_VALUE).textValue());
  }

  private List<Contributor> mutateContributorsPreview(JsonNode source) {
    var contributors = new ArrayList<Contributor>();
    source
        .path(ENTITY_DESCRIPTION)
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
                        aff ->
                            affiliations.add(
                                new Affiliation(
                                    aff.path(ID).textValue(), aff.path(TYPE).textValue())));
              }

              contributors.add(
                  new Contributor(
                      affiliations,
                      contributorNode.path(CORRESPONDING_AUTHOR).asBoolean(),
                      new Identity(
                          uriFromText(contributorNode.path(IDENTITY).path(ID).textValue()),
                          contributorNode.path(IDENTITY).path(NAME).textValue(),
                          uriFromText(contributorNode.path(IDENTITY).path(ORC_ID).textValue())),
                      contributorNode.path(ROLE).path(TYPE).textValue(),
                      contributorNode.path(SEQUENCE).asInt()));
            });
    return contributors;
  }

  private URI uriFromText(String text) {
    return Objects.isNull(text) ? null : URI.create(text);
  }
}
