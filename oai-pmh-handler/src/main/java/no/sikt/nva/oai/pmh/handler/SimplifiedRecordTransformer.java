package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import no.unit.nva.search.resource.response.Contributor;
import no.unit.nva.search.resource.response.PublicationDate;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import org.openarchives.oai.pmh.v2.ElementType;
import org.openarchives.oai.pmh.v2.HeaderType;
import org.openarchives.oai.pmh.v2.MetadataType;
import org.openarchives.oai.pmh.v2.OaiDcType;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;

public class SimplifiedRecordTransformer implements RecordTransformer {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
  private static final String JOURNAL = "Journal";
  private static final char DASH = '-';

  @Override
  public List<RecordType> transform(List<JsonNode> hits) {
    if (isNull(hits) || hits.isEmpty()) {
      return Collections.emptyList();
    }

    return hits.stream()
        .map(hit -> dtoObjectMapper.convertValue(hit, ResourceSearchResponse.class))
        .map(this::toRecord)
        .toList();
  }

  private RecordType toRecord(ResourceSearchResponse response) {
    var record = new RecordType();
    var metadata = populateMetadataType(response);
    var headerType = populateHeaderType(response.id(), response.recordMetadata().modifiedDate());
    record.setHeader(headerType);
    record.setMetadata(metadata);
    return record;
  }

  private static MetadataType populateMetadataType(ResourceSearchResponse response) {
    var metadata = OBJECT_FACTORY.createMetadataType();
    var oaiDcType = OBJECT_FACTORY.createOaiDcType();
    appendTitle(response, oaiDcType);
    appendContributors(response, oaiDcType);
    appendDate(response, oaiDcType);
    appendType(response, oaiDcType);
    appendPublisher(response, oaiDcType);
    metadata.setAny(OBJECT_FACTORY.createDc(oaiDcType));
    return metadata;
  }

  private static void appendContributors(ResourceSearchResponse response, OaiDcType oaiDcType) {
    response.contributorsPreview().stream()
        .map(SimplifiedRecordTransformer::toCreatorElement)
        .forEach(
            e -> oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createCreator(e)));
  }

  private static ElementType toCreatorElement(Contributor contributor) {
    var creatorElement = OBJECT_FACTORY.createElementType();
    creatorElement.setValue(contributor.identity().name());
    return creatorElement;
  }

  private static void appendTitle(ResourceSearchResponse response, OaiDcType oaiDcType) {
    var titleElement = OBJECT_FACTORY.createElementType();
    titleElement.setValue(response.mainTitle());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createTitle(titleElement));
  }

  private static void appendPublisher(ResourceSearchResponse response, OaiDcType oaiDcType) {
    if (nonNull(response.publishingDetails())) {
      final String publisherName;
      if (JOURNAL.equals(response.publishingDetails().type())) {
        var publishingDetails = response.publishingDetails();
        publisherName =
            nonNull(publishingDetails.name())
                ? publishingDetails.name()
                : publishingDetails.id().toString();
      } else if (nonNull(response.publishingDetails().publisher())) {
        var publisher = response.publishingDetails().publisher();
        publisherName = nonNull(publisher.name()) ? publisher.name() : publisher.id().toString();
      } else {
        publisherName = null;
      }

      if (nonNull(publisherName)) {
        var publisherElement = OBJECT_FACTORY.createElementType();
        publisherElement.setValue(publisherName);
        oaiDcType
            .getTitleOrCreatorOrSubject()
            .addLast(OBJECT_FACTORY.createPublisher(publisherElement));
      }
    }
  }

  private static void appendDate(ResourceSearchResponse response, OaiDcType oaiDcType) {
    var dateElement = OBJECT_FACTORY.createElementType();
    dateElement.setValue(asString(response.publicationDate()));
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createDate(dateElement));
  }

  private static String asString(PublicationDate publicationDate) {
    StringBuilder builder = new StringBuilder(publicationDate.year());
    if (nonNull(publicationDate.month())) {
      builder.append(DASH).append(publicationDate.month());
    }
    if (nonNull(publicationDate.day())) {
      builder.append(DASH).append(publicationDate.day());
    }
    return builder.toString();
  }

  private static void appendType(ResourceSearchResponse response, OaiDcType oaiDcType) {
    var typeElement = OBJECT_FACTORY.createElementType();
    typeElement.setValue(response.type());
    oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createType(typeElement));
  }

  private static HeaderType populateHeaderType(URI id, String datestamp) {
    var headerType = new ObjectFactory().createHeaderType();
    headerType.setIdentifier(id.toString());
    headerType.setDatestamp(datestamp);
    return headerType;
  }
}
