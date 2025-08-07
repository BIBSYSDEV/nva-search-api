package no.sikt.nva.oai.pmh.handler.oaipmh.transformers;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTimeUtils.truncateToSeconds;

import java.util.Collections;
import java.util.List;
import no.sikt.nva.oai.pmh.handler.oaipmh.RecordTransformer;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
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
  public RecordType transform(ResourceSearchResponse resourceSearchResponse) {
    return toRecord(resourceSearchResponse);
  }

  @Override
  public List<RecordType> transform(List<ResourceSearchResponse> hits) {
    if (isNull(hits) || hits.isEmpty()) {
      return Collections.emptyList();
    }

    return hits.stream().map(this::toRecord).toList();
  }

  private RecordType toRecord(ResourceSearchResponse response) {
    var record = new RecordType();
    var metadata = populateMetadataType(response);
    var headerType = populateHeaderType(response);
    record.setHeader(headerType);
    record.setMetadata(metadata);
    return record;
  }

  private static MetadataType populateMetadataType(ResourceSearchResponse response) {
    var metadata = OBJECT_FACTORY.createMetadataType();
    var oaiDcType = OBJECT_FACTORY.createOaiDcType();
    appendIdentifier(response, oaiDcType);
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

  private static void appendIdentifier(ResourceSearchResponse response, OaiDcType oaiDcType) {
    var identifierElement = OBJECT_FACTORY.createElementType();
    identifierElement.setValue(response.id().toString());
    oaiDcType
        .getTitleOrCreatorOrSubject()
        .addLast(OBJECT_FACTORY.createIdentifier(identifierElement));
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
        publisherName = resolvePublisherFromJournal(response);
      } else if (nonNull(response.publishingDetails().publisher())) {
        publisherName = resolvePublisherFromPublisher(response);
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

  private static String resolvePublisherFromPublisher(ResourceSearchResponse response) {
    var publisher = response.publishingDetails().publisher();
    return nonNull(publisher.name())
        ? publisher.name()
        : nonNull(publisher.id()) ? publisher.id().toString() : null;
  }

  private static String resolvePublisherFromJournal(ResourceSearchResponse response) {
    var publishingDetails = response.publishingDetails();
    return nonNull(publishingDetails.name())
        ? publishingDetails.name()
        : nonNull(publishingDetails.id()) ? publishingDetails.id().toString() : null;
  }

  private static void appendDate(ResourceSearchResponse response, OaiDcType oaiDcType) {
    if (nonNull(response.publicationDate()) && nonNull(response.publicationDate().year())) {
      var dateElement = OBJECT_FACTORY.createElementType();
      dateElement.setValue(asString(response.publicationDate()));
      oaiDcType.getTitleOrCreatorOrSubject().addLast(OBJECT_FACTORY.createDate(dateElement));
    }
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

  private static HeaderType populateHeaderType(ResourceSearchResponse response) {
    var headerType = new ObjectFactory().createHeaderType();
    headerType.setIdentifier(response.id().toString());
    var datestamp = truncateToSeconds(response.recordMetadata().modifiedDate());
    headerType.setDatestamp(datestamp);
    headerType
        .getSetSpec()
        .add(new SetSpec(SetRoot.RESOURCE_TYPE_GENERAL, response.type()).getValue().orElseThrow());
    return headerType;
  }
}
