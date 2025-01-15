package no.unit.nva.search.resource;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.constants.Words.ABSTRACT;
import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.CREATED_DATE;
import static no.unit.nva.constants.Words.DOI;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.IDENTIFIER;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.ISBN_LIST;
import static no.unit.nva.constants.Words.MAIN_TITLE;
import static no.unit.nva.constants.Words.MODIFIED_DATE;
import static no.unit.nva.constants.Words.NAME;
import static no.unit.nva.constants.Words.ONLINE_ISSN;
import static no.unit.nva.constants.Words.ORC_ID;
import static no.unit.nva.constants.Words.PRINT_ISSN;
import static no.unit.nva.constants.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.constants.Words.PUBLICATION_DATE;
import static no.unit.nva.constants.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.constants.Words.PUBLISHED_DATE;
import static no.unit.nva.constants.Words.REFERENCE;
import static no.unit.nva.constants.Words.ROLE;
import static no.unit.nva.constants.Words.SERIES;
import static no.unit.nva.constants.Words.STATUS;
import static no.unit.nva.constants.Words.TYPE;
import static no.unit.nva.constants.Words.VALUE;
import static no.unit.nva.search.resource.Constants.ADDITIONAL_IDENTIFIERS;
import static no.unit.nva.search.resource.Constants.ALTERNATIVE_TITLES;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_COUNT;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_PREVIEW;
import static no.unit.nva.search.resource.Constants.CRISTIN_IDENTIFIER;
import static no.unit.nva.search.resource.Constants.GLOBAL_EXCLUDED_FIELDS;
import static no.unit.nva.search.resource.Constants.MANIFESTATIONS;
import static no.unit.nva.search.resource.Constants.SCOPUS_IDENTIFIER;
import static no.unit.nva.search.resource.Constants.SEQUENCE;
import static no.unit.nva.search.resource.response.ResourceSearchResponse.responseBuilder;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
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
import no.unit.nva.search.common.records.JsonNodeMutator;
import no.unit.nva.search.resource.response.Contributor;
import no.unit.nva.search.resource.response.NodeUtils;
import no.unit.nva.search.resource.response.PublicationDate;
import no.unit.nva.search.resource.response.PublishingDetails;
import no.unit.nva.search.resource.response.RecordMetadata;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import no.unit.nva.search.resource.response.ResourceSearchResponse.OtherIdentifiers;

public class SimplifiedMutator implements JsonNodeMutator {

  public static final String HANDLE_IDENTIFIER = "HandleIdentifier";
  public static final String CORRESPONDING_AUTHOR = "correspondingAuthor";
  private final ObjectMapper objectMapper = dtoObjectMapper.copy();

  public SimplifiedMutator() {
    objectMapper
        .registerModule(new JodaModule())
        .configOverride(Map.class)
        .setInclude(
            JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
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
    var path = source.path(ENTITY_DESCRIPTION).path(PUBLICATION_DATE);
    return path.isMissingNode() ? null : new PublicationDate(path);
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
        Stream.concat(isbnsInPublicationContext.stream(), isbnsInManifestations.stream())
            .collect(Collectors.toUnmodifiableSet());
    var issns = fromNodeIssns(source);

    var handleIdentifiers = new HashSet<String>();
    var cristinIdentifiers = new HashSet<String>();
    var scopusIdentifiers = new HashSet<String>();
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
        scopusIdentifiers, cristinIdentifiers, handleIdentifiers, issns, isbns);
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

  private Set<String> extractIsbnsInPublicationContext(JsonNode source) {
    var isbnsInSourceNode =
        source.path(ENTITY_DESCRIPTION).path(REFERENCE).path(PUBLICATION_CONTEXT).path(ISBN_LIST);
    return isbnsInSourceNode.isMissingNode()
        ? Collections.emptySet()
        : nodeAsListOf(isbnsInSourceNode).stream().collect(Collectors.toUnmodifiableSet());
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
    return new PublishingDetails(source.path(ENTITY_DESCRIPTION).path(REFERENCE));
  }

  private List<Contributor> fromNodeContributorPreviews(JsonNode source) {
    var contributors = new ArrayList<Contributor>();
    source
        .path(ENTITY_DESCRIPTION)
        .path(CONTRIBUTORS_PREVIEW)
        .iterator()
        .forEachRemaining(node -> contributors.add(new Contributor(node)));
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