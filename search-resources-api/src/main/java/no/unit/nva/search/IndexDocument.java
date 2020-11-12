package no.unit.nva.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.nonNull;

public class IndexDocument {

    private static final ObjectMapper mapper = JsonUtils.objectMapper;

    private final String publicationType;
    private final UUID id;
    private final URI doi;
    private final List<IndexContributor> contributors;
    private final String title;
    private final String publicationAbstract;
    private final String description;
    private final String owner;
    private final IndexDate publicationDate;
    private final IndexPublisher publisher;
    private final Instant modifiedDate;

    /**
     * Creates and IndexDocument with given properties.
     */
    @JacocoGenerated
    @JsonCreator
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public IndexDocument(@JsonProperty("publicationType") String publicationType,
                         @JsonProperty("id") UUID id,
                         @JsonProperty("doi") URI doi,
                         @JsonProperty("contributors") List<IndexContributor> contributors,
                         @JsonProperty("title") String mainTitle,
                         @JsonProperty("abstract") String publicationAbstract,
                         @JsonProperty("description") String description,
                         @JsonProperty("owner") String owner,
                         @JsonProperty("publicationDate") IndexDate publicationDate,
                         @JsonProperty("publisher") IndexPublisher publisher,
                         @JsonProperty("modifiedDate") Instant modifiedDate) {
        this.publicationType = publicationType;
        this.id = id;
        this.doi = doi;
        this.contributors = contributors;
        this.title = mainTitle;
        this.publicationDate = publicationDate;
        this.description = description;
        this.publicationAbstract = publicationAbstract;
        this.owner = owner;
        this.publisher = publisher;
        this.modifiedDate = modifiedDate;
    }

    protected IndexDocument(Builder builder) {
        publicationType = builder.publicationType;
        id = builder.id;
        doi = builder.doi;
        contributors = builder.contributors;
        title = builder.title;
        description = builder.description;
        owner = builder.owner;
        publicationDate = builder.publicationDate;
        publicationAbstract = builder.publicationAbstract;
        publisher = builder.publisher;
        modifiedDate = builder.modifiedDate;

    }

    public String getPublicationType() {
        return publicationType;
    }

    public UUID getId() {
        return id;
    }

    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    public List<IndexContributor> getContributors() {
        return contributors;
    }

    @JacocoGenerated
    public String getTitle() {
        return title;
    }

    @JacocoGenerated
    public IndexDate getPublicationDate() {
        return publicationDate;
    }

    @JacocoGenerated
    public String getAbstract() {
        return publicationAbstract;
    }

    @JacocoGenerated
    public String getDescription() {
        return description;
    }

    @JacocoGenerated
    public String getOwner() {
        return owner;
    }

    @JacocoGenerated
    public IndexPublisher getPublisher() {
        return publisher;
    }

    @JacocoGenerated
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public String toJsonString() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexDocument)) {
            return false;
        }
        IndexDocument that = (IndexDocument) o;
        return Objects.equals(publicationType, that.publicationType)
            && Objects.equals(id, that.id)
            && Objects.equals(doi, that.doi)
            && Objects.equals(contributors, that.contributors)
            && Objects.equals(title, that.title)
            && Objects.equals(owner, that.owner)
            && Objects.equals(description, that.description)
            && Objects.equals(publicationAbstract, that.publicationAbstract)
            && Objects.equals(publicationDate, that.publicationDate)
            && Objects.equals(publisher, that.publisher)
            && Objects.equals(modifiedDate, that.modifiedDate);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(publicationType,
                id,
                doi,
                contributors,
                title,
                publicationDate,
                owner,
                description,
                publicationAbstract,
                publisher,
                modifiedDate);
    }

    public static final class Builder {

        private String publicationType;
        private UUID id;
        private URI doi;
        private List<IndexContributor> contributors;
        private IndexDate publicationDate;
        private String title;
        private String publicationAbstract;
        private String description;
        private String owner;
        private IndexPublisher publisher;
        private Instant modifiedDate;

        public Builder() {
        }

        public Builder withType(String type) {
            this.publicationType = type;
            return this;
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withContributors(List<IndexContributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAbstract(String publicationAbstract) {
            this.publicationAbstract = publicationAbstract;
            return this;
        }

        @SuppressWarnings("PMD.NullAssignment")
        public Builder withPublicationDate(IndexDate date) {
            this.publicationDate = isNonNullDate(date) ? date : null;
            return this;
        }

        public Builder withPublisher(IndexPublisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public IndexDocument build() {
            return new IndexDocument(this);
        }

        private boolean isNonNullDate(IndexDate date) {
            return nonNull(date) && date.isPopulated();
        }
    }
}
