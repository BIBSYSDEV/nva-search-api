package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeFactory;
import no.unit.nva.search.common.records.Facet;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.openarchives.oai.pmh.v2.DeletedRecordType;
import org.openarchives.oai.pmh.v2.GranularityType;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.SetType;
import org.openarchives.oai.pmh.v2.VerbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultOaiPmhDataProvider implements OaiPmhDataProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOaiPmhDataProvider.class);
  private static final String PUBLICATION_INSTANCE_TYPE_SET = "PublicationInstanceType";
  private static final String COLON = ":";
  private static final String INSTANCE_TYPE_AGGREGATION_NAME = "type";
  private static final String ZERO = "0";
  private static final String AGGREGATION_ALL = "all";
  private static final String PROTOCOL_VERSION = "2.0";
  private static final String REPOSITORY_NAME = "NVA-OAI-PMH";
  private static final String EARLIEST_DATESTAMP = "2016-01-01";
  private static final String CONTACT_AT_SIKT_NO = "kontakt@sikt.no";
  private static final String UNSUPPORTED_VERB = "Unsupported verb.";
  private static final String UNKNOWN_OR_NO_VERB_SUPPLIED = "Unknown or no verb supplied.";
  private static final String OAI_DC = "oai-dc";
  private static final String OAI_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
  private static final String OAI_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final ResourceClient opensearchClient;
  private final URI endpointUri;

  public DefaultOaiPmhDataProvider(ResourceClient opensearchClient, URI endpointUri) {
    this.opensearchClient = opensearchClient;
    this.endpointUri = endpointUri;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(final String verb) {
    Optional<VerbType> verbType;
    try {
      verbType = Optional.of(VerbType.fromValue(verb));
    } catch (IllegalArgumentException e) {
      verbType = Optional.empty();
    }

    return verbType
        .map(
            type ->
                switch (type) {
                  case LIST_SETS -> listSets();
                  case IDENTIFY -> identify();
                  case LIST_METADATA_FORMATS -> listMetadataFormats();
                  default -> badVerb(UNSUPPORTED_VERB);
                })
        .orElseGet(() -> badVerb(UNKNOWN_OR_NO_VERB_SUPPLIED));
  }

  private JAXBElement<OAIPMHtype> identify() {
    var oaiResponse = baseResponse();
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(VerbType.IDENTIFY);

    var identify = objectFactory.createIdentifyType();
    identify.setBaseURL(endpointUri.toString());
    identify.setProtocolVersion(PROTOCOL_VERSION);
    identify.setEarliestDatestamp(EARLIEST_DATESTAMP);
    identify.setRepositoryName(REPOSITORY_NAME);
    identify.setGranularity(GranularityType.YYYY_MM_DD);
    identify.setDeletedRecord(DeletedRecordType.NO);
    identify.getAdminEmail().add(CONTACT_AT_SIKT_NO);

    value.setIdentify(identify);

    return oaiResponse;
  }

  private JAXBElement<OAIPMHtype> listMetadataFormats() {
    var oaiResponse = baseResponse();
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(VerbType.LIST_METADATA_FORMATS);

    var listMetadataFormatsType = objectFactory.createListMetadataFormatsType();

    var metadataFormatType = objectFactory.createMetadataFormatType();
    metadataFormatType.setMetadataPrefix(OAI_DC);
    metadataFormatType.setSchema(OAI_DC_SCHEMA);
    metadataFormatType.setMetadataNamespace(OAI_DC_NAMESPACE);

    listMetadataFormatsType.getMetadataFormat().add(metadataFormatType);

    value.setListMetadataFormats(listMetadataFormatsType);

    return oaiResponse;
  }

  private JAXBElement<OAIPMHtype> baseResponse() {
    var responseDate =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()));
    var request = objectFactory.createRequestType();
    var oaiPmhType = objectFactory.createOAIPMHtype();
    oaiPmhType.setResponseDate(responseDate);
    oaiPmhType.setRequest(request);

    return objectFactory.createOAIPMH(oaiPmhType);
  }

  private JAXBElement<OAIPMHtype> listSets() {
    final ResourceSearchQuery query = buildAllAggregationsQuery();

    var instanceTypes = doSearchAndExtractInstanceTypesFromTypeAggregation(query);

    var oaiResponse = baseResponse();
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(VerbType.LIST_SETS);

    var listSets = objectFactory.createListSetsType();
    listSets.getSet().addAll(generateSets(objectFactory, instanceTypes));
    value.setListSets(listSets);

    return oaiResponse;
  }

  private Set<String> doSearchAndExtractInstanceTypesFromTypeAggregation(
      ResourceSearchQuery query) {
    final HttpResponseFormatter<ResourceParameter> response;
    try {
      response = query.doSearch(opensearchClient);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to search for publication instance types using 'type' aggregation.", e);
      throw new ResourceSearchException("Error looking up instance types using aggregations.", e);
    }

    return response
        .toPagedResponse()
        .aggregations()
        .getOrDefault(INSTANCE_TYPE_AGGREGATION_NAME, Collections.emptyList())
        .stream()
        .map(Facet::key)
        .collect(Collectors.toSet());
  }

  private static ResourceSearchQuery buildAllAggregationsQuery() {
    final ResourceSearchQuery query;
    try {
      query =
          ResourceSearchQuery.builder()
              .withParameter(ResourceParameter.AGGREGATION, AGGREGATION_ALL)
              .withParameter(ResourceParameter.FROM, ZERO)
              .withParameter(ResourceParameter.SIZE, ZERO)
              .build();
    } catch (BadRequestException e) {
      // should never happen unless query validation code is changed!
      LOGGER.error("Failed to search for publication instance types using 'type' aggregation.", e);
      throw new ResourceSearchException("Error looking up instance types using aggregations.", e);
    }
    return query;
  }

  private List<SetType> generateSets(ObjectFactory objectFactory, Set<String> instanceTypes) {
    var setTypes = new LinkedList<SetType>();
    var setQualifier = objectFactory.createSetType();
    setQualifier.setSetSpec(PUBLICATION_INSTANCE_TYPE_SET);
    setQualifier.setSetName(PUBLICATION_INSTANCE_TYPE_SET);

    setTypes.add(setQualifier);

    var subSetTypes = instanceTypes.stream().map(this::wrap).toList();

    setTypes.addAll(subSetTypes);
    return setTypes;
  }

  private SetType wrap(String instanceType) {
    var setType = objectFactory.createSetType();
    setType.setSetSpec(PUBLICATION_INSTANCE_TYPE_SET + COLON + instanceType);
    setType.setSetName(instanceType);

    return setType;
  }

  private JAXBElement<OAIPMHtype> badVerb(String message) {
    var response = baseResponse();
    var value = response.getValue();
    var error = objectFactory.createOAIPMHerrorType();
    error.setCode(OAIPMHerrorcodeType.BAD_VERB);
    error.setValue(message);

    value.getError().add(error);

    return response;
  }
}
