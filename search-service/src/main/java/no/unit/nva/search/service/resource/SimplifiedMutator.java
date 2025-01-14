package no.unit.nva.search.service.resource;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.search.model.constant.Words.ABSTRACT;
import static no.unit.nva.search.model.constant.Words.AFFILIATIONS;
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
import static no.unit.nva.search.model.constant.Words.SERIES;
import static no.unit.nva.search.model.constant.Words.STATUS;
import static no.unit.nva.search.model.constant.Words.TYPE;
import static no.unit.nva.search.model.constant.Words.VALUE;
import static no.unit.nva.search.model.constant.Words.YEAR;
import static no.unit.nva.search.service.resource.Constants.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.service.resource.Constants.ALTERNATIVE_TITLES;
import static no.unit.nva.search.service.resource.Constants.CONTRIBUTORS_COUNT;
import static no.unit.nva.search.service.resource.Constants.CONTRIBUTORS_PREVIEW;
import static no.unit.nva.search.service.resource.Constants.CRISTIN_IDENTIFIER;
import static no.unit.nva.search.service.resource.Constants.GLOBAL_EXCLUDED_FIELDS;
import static no.unit.nva.search.service.resource.Constants.MANIFESTATIONS;
import static no.unit.nva.search.service.resource.Constants.SCOPUS_IDENTIFIER;
import static no.unit.nva.search.service.resource.Constants.SEQUENCE;
import static no.unit.nva.search.service.resource.response.ResourceSearchResponse.responseBuilder;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.model.records.JsonNodeMutator;
import no.unit.nva.search.service.resource.response.Contributor;
import no.unit.nva.search.service.resource.response.NodeUtils;
import no.unit.nva.search.service.resource.response.PublicationDate;
import no.unit.nva.search.service.resource.response.PublishingDetails;
import no.unit.nva.search.service.resource.response.RecordMetadata;
import no.unit.nva.search.service.resource.response.ResourceSearchResponse;
import no.unit.nva.search.service.resource.response.ResourceSearchResponse.OtherIdentifiers;

public class SimplifiedMutator implements JsonNodeMutator {

  public static final String HANDLE_IDENTIFIER = "HandleIdentifier";
  public static final String CORRESPONDING_AUTHOR = "correspondingAuthor";
  private final ObjectMapper objectMapper = dtoObjectMapper.copy();

  public SimplifiedMutator() {
    objectMapper
        .configOverride(Map.class)
        .setInclude(
            JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

    //        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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

  @Override
  public JsonNode transform(JsonNode source) {
    return (JsonNode) attempt(() -> objectMapper.valueToTree(transformToDto(source))).orElseThrow();
  }

  private PublicationDate fromNodePublicationDate(JsonNode source) {
    return new PublicationDate(
        source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(YEAR).textValue(),
        source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(MONTH).textValue(),
        source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE).path(DAY).textValue());
  }

  private ResourceSearchResponse transformToDto(JsonNode source) {
    return responseBuilder()
        .withId(NodeUtils.toUri(source.path(ID)))
        .withIdentifier(source.path(IDENTIFIER))
        .withMainTitle(source.path(ENTITY_DESCRIPTION).path(MAIN_TITLE))
        .withMainLanguageAbstract(source.path(ENTITY_DESCRIPTION).path(ABSTRACT))
        .withDescription(source.path(ENTITY_DESCRIPTION).path(Constants.DESCRIPTION))
        .withContributorsCount(source.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS_COUNT))
        .withType(source)
        .withAlternativeTitles(fromNodeAlternativeTitles(source))
        .withPublicationDate(fromNodePublicationDate(source))
        .withContributorsPreview(fromNodeContributorPreviews(source))
        .withPublishingDetails(fromNodePublishingDetails(source))
        .withOtherIdentifiers(fromNodeOtherIdentifiers(source))
        .withRecordMetadata(fromNodeRecordMetadata(source))
        .build();
  }

  private Map<String, String> fromNodeAlternativeTitles(JsonNode source) {
    var path = source.path(ENTITY_DESCRIPTION).path(ALTERNATIVE_TITLES);
    return path.isMissingNode() ? Map.of() : jsonNodeMapToMap(path);
  }

  private Map<String, String> jsonNodeMapToMap(JsonNode source) {
    return objectMapper.convertValue(source, new TypeReference<>() {});
  }

  private OtherIdentifiers fromNodeOtherIdentifiers(JsonNode source) {
    var isbnsInManifestations = isbnsInManifestations(source);
    var isbnsInPublicationContext = extractIsbnsInPublicationContext(source);
    var isbns =
        Stream.concat(isbnsInPublicationContext.stream(), isbnsInManifestations.stream()).toList();
    var issns = fromNodeIssns(source);

    var handleIdentifiers = new ArrayList<String>();
    var cristinIdentifiers = new ArrayList<String>();
    var scopusIdentifiers = new ArrayList<String>();
    source
        .path(ADDITIONAL_IDENTIFIERS)
        .iterator()
        .forEachRemaining(
            i -> {
              switch (i.path(TYPE).textValue()) {
                case HANDLE_IDENTIFIER -> handleIdentifiers.add(i.path(VALUE).textValue());
                case SCOPUS_IDENTIFIER -> scopusIdentifiers.add(i.path(VALUE).textValue());
                case CRISTIN_IDENTIFIER -> cristinIdentifiers.add(i.path(VALUE).textValue());
                default -> {}
              }
            });

    return new OtherIdentifiers(
        new HashSet<>(scopusIdentifiers),
        new HashSet<>(cristinIdentifiers),
        new HashSet<>(handleIdentifiers),
        new HashSet<>(issns),
        new HashSet<>(isbns));
  }

  private List<String> isbnsInManifestations(JsonNode source) {
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
                if (!manifest.path(ISBN_LIST).isMissingNode()) {
                  isbnsInManifestations.addAll(nodeAsListOf(manifest.path(ISBN_LIST)));
                }
              });
    }
    return isbnsInManifestations;
  }

  private List<String> extractIsbnsInPublicationContext(JsonNode source) {
    var isbnsInSourceNode =
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(ISBN_LIST);
    return isbnsInSourceNode.isMissingNode()
        ? Collections.emptyList()
        : nodeAsListOf(isbnsInSourceNode);
  }

  private Set<String> fromNodeIssns(JsonNode source) {
    var context = source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT);
    return Stream.of(
            context.path(ONLINE_ISSN).textValue(),
            context.path(PRINT_ISSN).textValue(),
            context.path(SERIES).path(ONLINE_ISSN).textValue(),
            context.path(SERIES).path(PRINT_ISSN).textValue())
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private RecordMetadata fromNodeRecordMetadata(JsonNode source) {
    return new RecordMetadata(
        source.path(STATUS),
        source.path(CREATED_DATE),
        source.path(MODIFIED_DATE),
        source.path(PUBLISHED_DATE));
  }

  private PublishingDetails fromNodePublishingDetails(JsonNode source) {
    return new PublishingDetails(
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(ID),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(TYPE),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(NAME),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(DOI),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(SERIES),
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(PUBLISHER));
  }

  private String fromNodePublicationContextType(JsonNode source) {
    return source
        .path(ENTITY_DESCRIPTION)
        .path(REFERENCE)
        .path(PUBLICATION_CONTEXT)
        .path(TYPE)
        .textValue();
  }

  private List<Contributor> fromNodeContributorPreviews(JsonNode source) {
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

  private List<String> nodeAsListOf(JsonNode jsonNode) {
    try {
      return objectMapper.readerForListOf(String.class).readValue(jsonNode);
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }
}
