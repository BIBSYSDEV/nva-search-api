package no.sikt.nva.oai.pmh.handler.oaipmh.transformers;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTimeUtils.truncateToSeconds;
import static no.sikt.nva.oai.pmh.handler.oaipmh.transformers.XmlUtils.createSafeElementType;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import no.sikt.nva.oai.pmh.handler.oaipmh.RecordTransformer;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
import no.unit.nva.search.resource.response.Organization;
import no.unit.nva.search.resource.response.PublicationDate;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
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
  private static final String LANGUAGE_EN = "en";
  private static final String LANGUAGE_NB = "nb";
  private static final String LANGUAGE_NN = "nn";
  private static final String[] LANGUAGE_KEYS_IN_ORDER_OF_PREFERENCE = {
    LANGUAGE_EN, LANGUAGE_NB, LANGUAGE_NN
  };

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
    record.setHeader(populateHeaderType(response));
    record.setMetadata(populateMetadataType(response));
    return record;
  }

  private static MetadataType populateMetadataType(ResourceSearchResponse response) {
    var metadata = OBJECT_FACTORY.createMetadataType();
    var oaiDcType = OBJECT_FACTORY.createOaiDcType();

    appendDcElement(oaiDcType, OBJECT_FACTORY::createIdentifier, response.id().toString());
    appendAdditionalIdentifiers(response, oaiDcType);
    appendDcElement(oaiDcType, OBJECT_FACTORY::createTitle, response.mainTitle());
    appendDcElement(oaiDcType, OBJECT_FACTORY::createDescription, response.mainLanguageAbstract());
    appendDcElement(oaiDcType, OBJECT_FACTORY::createDescription, response.description());
    appendDcElement(oaiDcType, OBJECT_FACTORY::createLanguage, extractLanguage(response));
    appendContributors(response, oaiDcType);
    appendDcElement(oaiDcType, OBJECT_FACTORY::createDate, formatPublicationDate(response));
    appendDcElement(oaiDcType, OBJECT_FACTORY::createType, response.type());
    appendDcElement(oaiDcType, OBJECT_FACTORY::createPublisher, resolvePublisherName(response));
    response.tags().forEach(tag -> appendDcElement(oaiDcType, OBJECT_FACTORY::createSubject, tag));

    metadata.setAny(OBJECT_FACTORY.createDc(oaiDcType));
    return metadata;
  }

  private static void appendDcElement(
      OaiDcType oaiDcType, Function<ElementType, JAXBElement<ElementType>> wrapper, String value) {
    var element = createSafeElementType(value);
    if (StringUtils.isNotEmpty(element.getValue())) {
      oaiDcType.getTitleOrCreatorOrSubject().addLast(wrapper.apply(element));
    }
  }

  private static void appendContributors(ResourceSearchResponse response, OaiDcType oaiDcType) {
    response.contributorsPreview().stream()
        .map(contributor -> contributor.identity().name())
        .forEach(name -> appendDcElement(oaiDcType, OBJECT_FACTORY::createContributor, name));
    response.participatingOrganizations().stream()
        .map(SimplifiedRecordTransformer::extractOrganizationName)
        .forEach(name -> appendDcElement(oaiDcType, OBJECT_FACTORY::createContributor, name));
  }

  private static void appendAdditionalIdentifiers(
      ResourceSearchResponse response, OaiDcType oaiDcType) {
    var otherIdentifiers = response.otherIdentifiers();
    appendPrefixedIdentifiers(oaiDcType, otherIdentifiers.cristin(), "CRISTIN:");
    appendPrefixedIdentifiers(oaiDcType, otherIdentifiers.scopus(), "SCOPUS:");
    otherIdentifiers
        .handle()
        .forEach(handle -> appendDcElement(oaiDcType, OBJECT_FACTORY::createIdentifier, handle));
    appendPrefixedIdentifiers(oaiDcType, otherIdentifiers.isbn(), "ISBN:");
    appendPrefixedIdentifiers(oaiDcType, otherIdentifiers.issn(), "ISSN:");
    if (nonNull(response.publishingDetails().doi())) {
      appendDcElement(
          oaiDcType,
          OBJECT_FACTORY::createIdentifier,
          response.publishingDetails().doi().toString());
    }
  }

  private static void appendPrefixedIdentifiers(
      OaiDcType oaiDcType, Set<String> identifiers, String prefix) {
    identifiers.forEach(
        identifier ->
            appendDcElement(oaiDcType, OBJECT_FACTORY::createIdentifier, prefix + identifier));
  }

  private static String extractOrganizationName(Organization organization) {
    return Arrays.stream(LANGUAGE_KEYS_IN_ORDER_OF_PREFERENCE)
        .map(languageKey -> organization.labels().get(languageKey))
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .orElse(UriWrapper.fromUri(organization.id()).getLastPathElement());
  }

  private static String extractLanguage(ResourceSearchResponse response) {
    return nonNull(response.language())
        ? UriWrapper.fromUri(response.language()).getLastPathElement()
        : null;
  }

  private static String formatPublicationDate(ResourceSearchResponse response) {
    var publicationDate = response.publicationDate();
    if (isNull(publicationDate) || isNull(publicationDate.year())) {
      return null;
    }
    return formatPublicationDate(publicationDate);
  }

  private static String formatPublicationDate(PublicationDate publicationDate) {
    var builder = new StringBuilder(publicationDate.year());
    if (nonNull(publicationDate.month())) {
      builder.append(DASH).append(publicationDate.month());
    }
    if (nonNull(publicationDate.day())) {
      builder.append(DASH).append(publicationDate.day());
    }
    return builder.toString();
  }

  private static String resolvePublisherName(ResourceSearchResponse response) {
    var details = response.publishingDetails();
    if (isNull(details)) {
      return null;
    }
    if (JOURNAL.equals(details.type())) {
      return nameOrId(details.name(), details.id());
    }
    var publisher = details.publisher();
    return isNull(publisher) ? null : nameOrId(publisher.name(), publisher.id());
  }

  private static String nameOrId(String name, URI id) {
    return Optional.ofNullable(name)
        .or(() -> Optional.ofNullable(id).map(URI::toString))
        .orElse(null);
  }

  private static HeaderType populateHeaderType(ResourceSearchResponse response) {
    var headerType = OBJECT_FACTORY.createHeaderType();
    headerType.setIdentifier(response.id().toString());
    headerType.setDatestamp(truncateToSeconds(response.recordMetadata().modifiedDate()));
    headerType
        .getSetSpec()
        .add(new SetSpec(SetRoot.RESOURCE_TYPE_GENERAL, response.type()).getValue().orElseThrow());
    response.participatingOrganizations().stream()
        .map(Organization::id)
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .forEach(
            identifier ->
                headerType
                    .getSetSpec()
                    .add(new SetSpec(SetRoot.INSTITUTION, identifier).getValue().orElseThrow()));
    return headerType;
  }
}
