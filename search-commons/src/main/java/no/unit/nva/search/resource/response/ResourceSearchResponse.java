package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResourceSearchResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("otherIdentifiers")
    private OtherIdentifiers otherIdentifiers;

    @JsonProperty("recordMetadata")
    private RecordMetadata recordMetadata;

    @JsonProperty("mainTitle")
    private String mainTitle;

    @JsonProperty("abstract")
    private String mainLanguageAbstract;

    @JsonProperty("description")
    private String description;

    @JsonProperty("alternativeTitles")
    private List<String> alternativeTitles;

    @JsonProperty("publicationDate")
    private String publicationDate;

    @JsonProperty("contributorsPreview")
    private List<Contributor> contributorsPreview;

    @JsonProperty("contributorsCount")
    private int contributorsCount;

    @JsonProperty("publishingDetails")
    private PublishingDetails publishingDetails;
}
