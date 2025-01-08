package no.unit.nva.search.service.resource;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.search.model.constant.Words.ABSTRACT;
import static no.unit.nva.search.model.constant.Words.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.model.constant.Words.AFFILIATIONS;
import static no.unit.nva.search.model.constant.Words.CONTRIBUTORS_PREVIEW;
import static no.unit.nva.search.model.constant.Words.CREATED_DATE;
import static no.unit.nva.search.model.constant.Words.DAY;
import static no.unit.nva.search.model.constant.Words.DOI;
import static no.unit.nva.search.model.constant.Words.DOT;
import static no.unit.nva.search.model.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.model.constant.Words.ID;
import static no.unit.nva.search.model.constant.Words.IDENTIFIER;
import static no.unit.nva.search.model.constant.Words.IDENTITY;
import static no.unit.nva.search.model.constant.Words.ISBN_LIST;
import static no.unit.nva.search.model.constant.Words.MAIN_TITLE;
import static no.unit.nva.search.model.constant.Words.MODIFIED_DATE;
import static no.unit.nva.search.model.constant.Words.MONTH;
import static no.unit.nva.search.model.constant.Words.NAME;
import static no.unit.nva.search.model.constant.Words.ONLINE_ISSN;
import static no.unit.nva.search.model.constant.Words.ORC_ID;
import static no.unit.nva.search.model.constant.Words.PRINT_ISSN;
import static no.unit.nva.search.model.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search.model.constant.Words.PUBLICATION_DATE;
import static no.unit.nva.search.model.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search.model.constant.Words.PUBLISHED_DATE;
import static no.unit.nva.search.model.constant.Words.PUBLISHER;
import static no.unit.nva.search.model.constant.Words.REFERENCE;
import static no.unit.nva.search.model.constant.Words.ROLE;
import static no.unit.nva.search.model.constant.Words.SCIENTIFIC_VALUE;
import static no.unit.nva.search.model.constant.Words.SERIES;
import static no.unit.nva.search.model.constant.Words.STATUS;
import static no.unit.nva.search.model.constant.Words.TYPE;
import static no.unit.nva.search.model.constant.Words.YEAR;
import static no.unit.nva.search.service.resource.Constants.SEQUENCE;
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
import no.unit.nva.search.model.records.JsonNodeMutator;
import no.unit.nva.search.service.resource.response.Contributor;
import no.unit.nva.search.service.resource.response.PublicationDate;
import no.unit.nva.search.service.resource.response.PublishingDetails;
import no.unit.nva.search.service.resource.response.RecordMetadata;
import no.unit.nva.search.service.resource.response.ResourceSearchResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimplifiedResourceModelMutator implements JsonNodeMutator {

  public static final String ALTERNATIVE_TITLES = "alternativeTitles";
  public static final String CONTRIBUTORS_COUNT = "contributorsCount";
  public static final String CORRESPONDING_AUTHOR = "correspondingAuthor";
  public static final String CRISTIN_IDENTIFIER = "CristinIdentifier";
  public static final String DESCRIPTION = "description";
  public static final String HANDLE_IDENTIFIER = "HandleIdentifier";
  public static final String MANIFESTATIONS = "manifestations";

  public static final String SCOPUS_IDENTIFIER = "ScopusIdentifier";
  public static final String VALUE = "value";

  private final ObjectMapper objectMapper = dtoObjectMapper.copy();

  public SimplifiedResourceModelMutator() {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public static String path(String... path) {
    return String.join(DOT, path);
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
        path(ENTITY_DESCRIPTION, DESCRIPTION),
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
    return ResourceSearchResponse.builder()
        .withId(uriFromText(source.path(ID)))
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
        .withDescription(source.path(ENTITY_DESCRIPTION).path(DESCRIPTION).textValue())
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

  private ResourceSearchResponse.OtherIdentifiers mutateOtherIdentifiers(JsonNode source)
      throws IOException {
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
                  var isbns =
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

    return new ResourceSearchResponse.OtherIdentifiers(
        new HashSet<>(scopusIdentifiers),
        new HashSet<>(cristinIdentifiers),
        new HashSet<>(handleIdentifiers),
        new HashSet<>(issns),
        new HashSet<>(isbns));
  }

  private RecordMetadata mutateRecordMetadata(JsonNode source) {
    return new RecordMetadata(
        source.path(STATUS), source.path(CREATED_DATE),
        source.path(MODIFIED_DATE), source.path(PUBLISHED_DATE));
  }

  private PublishingDetails mutatePublishingDetails(JsonNode source) {
    return new PublishingDetails(
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(ID),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(TYPE),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(NAME),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(DOI),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(SERIES),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(PUBLISHER));
  }

  private List<Contributor> mutateContributorsPreview(JsonNode source) {
    var contributors = new ArrayList<Contributor>();
    source
        .path(ENTITY_DESCRIPTION)
        .path(CONTRIBUTORS_PREVIEW)
        .iterator()
        .forEachRemaining(
            contributorNode ->
                contributors.add(
                    new Contributor(
                        contributorNode.path(IDENTITY),
                        contributorNode.path(ROLE).path(TYPE),
                        contributorNode.path(AFFILIATIONS),
                        contributorNode.path(CORRESPONDING_AUTHOR),
                        contributorNode.path(SEQUENCE))));
    return contributors;
  }

  private URI uriFromText(JsonNode text) {
    return Objects.isNull(text) ? null : URI.create(text.asText());
  }
}
