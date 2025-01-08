package no.unit.nva.indexingclient;

import static no.unit.nva.constants.Words.SLASH;
import static no.unit.nva.indexingclient.Constants.BATCH_INDEX_EVENT_TOPIC;
import static no.unit.nva.indexingclient.Constants.S3_LOCATION_FIELD;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.events.models.EventBody;
import nva.commons.core.JacocoGenerated;

public class ImportDataRequestEvent implements EventBody, JsonSerializable {

  private static final String START_OF_LISTING_INDEX = "startMarker";

  @JsonProperty(S3_LOCATION_FIELD)
  private final URI s3Location;

  @JsonProperty(START_OF_LISTING_INDEX)
  private final String startMarker;

  @JsonCreator
  public ImportDataRequestEvent(
      @JsonProperty(S3_LOCATION_FIELD) String s3Location,
      @JsonProperty(START_OF_LISTING_INDEX) String startMarker) {
    this.s3Location =
        Optional.ofNullable(s3Location).map(URI::create).orElseThrow(this::reportMissingValue);
    this.startMarker = startMarker;
  }

  public ImportDataRequestEvent(String s3Location) {
    this(s3Location, null);
  }

  @Override
  public String getTopic() {
    return BATCH_INDEX_EVENT_TOPIC;
  }

  public String getStartMarker() {
    return startMarker;
  }

  public String getS3Location() {
    return s3Location.toString();
  }

  @JsonIgnore
  @JacocoGenerated
  public String getBucket() {
    return s3Location.getHost();
  }

  @JsonIgnore
  @JacocoGenerated
  public String getS3Path() {
    return Optional.ofNullable(s3Location).map(URI::getPath).map(this::removeRoot).orElseThrow();
  }

  @JacocoGenerated
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ImportDataRequestEvent that)) {
      return false;
    }
    return Objects.equals(getS3Location(), that.getS3Location())
        && Objects.equals(getStartMarker(), that.getStartMarker());
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return Objects.hash(getS3Location(), getStartMarker());
  }

  private IllegalArgumentException reportMissingValue() {
    return new IllegalArgumentException("Missing input:" + S3_LOCATION_FIELD);
  }

  private String removeRoot(String path) {
    return path.startsWith(SLASH) ? path.substring(1) : path;
  }
}
