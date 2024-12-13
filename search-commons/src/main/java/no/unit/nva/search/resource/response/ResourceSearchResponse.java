package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record ResourceSearchResponse(
        URI id,
        String identifier,
        String type,
        OtherIdentifiers otherIdentifiers,
        RecordMetadata recordMetadata,
        String mainTitle,
        @JsonProperty("abstract") String mainLanguageAbstract,
        String description,
        Map<String, String> alternativeTitles,
        PublicationDate publicationDate,
        List<Contributor> contributorsPreview,
        int contributorsCount,
        PublishingDetails publishingDetails) {

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

        public Builder() {}

        public static Builder aResourceSearchResponse() {
            return new Builder();
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
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

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withMainLanguageAbstract(String mainLanguageAbstract) {
            this.mainLanguageAbstract = mainLanguageAbstract;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
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

        public Builder withContributorsCount(int contributorsCount) {
            this.contributorsCount = contributorsCount;
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
                    recordMetadata,
                    mainTitle,
                    mainLanguageAbstract,
                    description,
                    alternativeTitles,
                    publicationDate,
                    contributorsPreview,
                    contributorsCount,
                    publishingDetails);
        }
    }
}
