package no.unit.nva.search.service.resource.response;

import static java.util.Objects.isNull;
import static no.unit.nva.search.model.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.model.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search.model.constant.Words.REFERENCE;
import static no.unit.nva.search.model.constant.Words.TYPE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ResourceSearchResponse(
    URI id,
    String identifier,
    String type,
    OtherIdentifiers otherIdentifiers,
    String mainTitle,
    @JsonProperty("abstract") String mainLanguageAbstract,
    String description,
    Map<String, String> alternativeTitles,
    PublicationDate publicationDate,
    List<Contributor> contributorsPreview,
    int contributorsCount,
    PublishingDetails publishingDetails,
    RecordMetadata recordMetadata) {

  public static Builder responseBuilder() {
    return new Builder();
  }

  public record OtherIdentifiers(
      Set<String> scopus,
      Set<String> cristin,
      Set<String> handle,
      Set<String> issn,
      Set<String> isbn) {}

  public static final class Builder {

    private URI id;
    private String identifier;
    private String type;
    private OtherIdentifiers otherIdentifiers;
    private RecordMetadata recordMetadata;
    private String mainTitle;
    private String mainLanguageAbstract;
    private String description;
    private Map<String, String> alternativeTitles;
    private PublicationDate publicationDate;
    private List<Contributor> contributorsPreview;
    private int contributorsCount;
    private PublishingDetails publishingDetails;

    private Builder() {}

    public Builder withId(URI id) {
      this.id = id;
      return this;
    }

    public Builder withIdentifier(JsonNode identifier) {
      this.identifier = isNull(identifier) ? null : identifier.textValue();
      return this;
    }

    public Builder withType(JsonNode source) {
      this.type =
          source
              .path(ENTITY_DESCRIPTION)
              .path(REFERENCE)
              .path(PUBLICATION_INSTANCE)
              .path(TYPE)
              .textValue();
      return this;
    }

    public Builder withOtherIdentifiers(OtherIdentifiers otherIdentifiers) {
      this.otherIdentifiers = otherIdentifiers;
      return this;
    }

    public Builder withRecordMetadata(RecordMetadata recordMetadata) {
      this.recordMetadata = recordMetadata;
      return this;
    }

    public Builder withMainTitle(JsonNode mainTitle) {
      this.mainTitle = mainTitle.textValue();
      return this;
    }

    public Builder withMainLanguageAbstract(JsonNode mainLanguageAbstract) {
      this.mainLanguageAbstract = mainLanguageAbstract.textValue();
      return this;
    }

    public Builder withDescription(JsonNode description) {
      this.description = description.textValue();
      return this;
    }

    public Builder withAlternativeTitles(Map<String, String> alternativeTitles) {
      this.alternativeTitles = alternativeTitles;
      return this;
    }

    public Builder withPublicationDate(PublicationDate publicationDate) {
      this.publicationDate = publicationDate;
      return this;
    }

    public Builder withContributorsPreview(List<Contributor> contributorsPreview) {
      this.contributorsPreview = contributorsPreview;
      return this;
    }

    public Builder withContributorsCount(JsonNode contributorsCount) {
      this.contributorsCount = contributorsCount.asInt();
      return this;
    }

    public Builder withPublishingDetails(PublishingDetails publishingDetails) {
      this.publishingDetails = publishingDetails;
      return this;
    }

    public ResourceSearchResponse build() {
      return new ResourceSearchResponse(
          id,
          identifier,
          type,
          otherIdentifiers,
          mainTitle,
          mainLanguageAbstract,
          description,
          alternativeTitles,
          publicationDate,
          contributorsPreview,
          contributorsCount,
          publishingDetails,
          recordMetadata);
    }
  }
}
