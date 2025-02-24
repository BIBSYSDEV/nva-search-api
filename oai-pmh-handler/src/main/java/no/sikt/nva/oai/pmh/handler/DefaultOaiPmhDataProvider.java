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
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.scroll.ScrollClient;
import no.unit.nva.search.scroll.ScrollQuery;
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
  private static final String AGGREGATION_NONE = "none";
  private static final String PROTOCOL_VERSION = "2.0";
  private static final String REPOSITORY_NAME = "NVA-OAI-PMH";
  private static final String EARLIEST_DATESTAMP = "2016-01-01";
  private static final String CONTACT_AT_SIKT_NO = "kontakt@sikt.no";
  private static final String UNSUPPORTED_VERB = "Unsupported verb.";
  private static final String UNKNOWN_OR_NO_VERB_SUPPLIED = "Unknown or no verb supplied.";
  private static final String OAI_DC = "oai-dc";
  private static final String OAI_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
  private static final String OAI_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
  private static final String SCROLL_TTL = "10m";

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final ResourceClient resourceClient;
  private final ScrollClient scrollClient;
  private final URI endpointUri;
  private final RecordTransformer recordTransformer;

  public DefaultOaiPmhDataProvider(
      ResourceClient resourceClient, ScrollClient scrollClient, URI endpointUri) {
    this.resourceClient = resourceClient;
    this.scrollClient = scrollClient;
    this.endpointUri = endpointUri;
    this.recordTransformer = new GraphRecordTransformer();
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(
      final String verb,
      final String from,
      final String until,
      final String metadataPrefix,
      final String resumptionToken) {
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
                  case LIST_RECORDS -> listRecords(from, until, resumptionToken, metadataPrefix);
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

  private HttpResponseFormatter<ResourceParameter> doSearch(ResourceSearchQuery query) {
    try {
      return query.doSearch(resourceClient);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to search for records.", e);
      throw new ResourceSearchException("Error looking up records.", e);
    }
  }

  private JAXBElement<OAIPMHtype> listRecords(
      String from, String until, String resumptionToken, String metadataPrefix) {

    final SwsResponse response;
    if (resumptionToken != null) {
      var token = ResumptionToken.from(resumptionToken);
      var scrollQuery =
          ScrollQuery.builder().withScrollId(token.scrollId()).withTtl(SCROLL_TTL).build();
      response = scrollQuery.doSearch(scrollClient).swsResponse();
    } else {
      final ResourceSearchQuery query = buildListRecordsPageQuery(from, until);
      response = doSearch(query).swsResponse();
    }

    var oaiResponse = baseResponse();
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(VerbType.LIST_RECORDS);
    value.getRequest().setResumptionToken(resumptionToken);
    value.getRequest().setFrom(from);
    value.getRequest().setUntil(until);
    value.getRequest().setMetadataPrefix(metadataPrefix);

    var scrollId = response._scroll_id();

    var records = recordTransformer.transform(response.getSearchHits());

    var inTenMinutes =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now().plusMinutes(10)));

    var resumptionTokenType = objectFactory.createResumptionTokenType();
    var newResumptionToken = new ResumptionToken(from, until, metadataPrefix, scrollId);
    resumptionTokenType.setValue(newResumptionToken.getValue());
    resumptionTokenType.setExpirationDate(inTenMinutes);

    var listRecords = objectFactory.createListRecordsType();
    listRecords.getRecord().addAll(records);
    listRecords.setResumptionToken(resumptionTokenType);

    value.setListRecords(listRecords);
    return oaiResponse;
  }

  private Set<String> doSearchAndExtractInstanceTypesFromTypeAggregation(
      ResourceSearchQuery query) {
    final HttpResponseFormatter<ResourceParameter> response;
    try {
      response = query.doSearch(resourceClient);
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

  private static ResourceSearchQuery buildListRecordsPageQuery(String from, String until) {
    final ResourceSearchQuery query;
    try {
      query =
          ResourceSearchQuery.builder()
              .withParameter(ResourceParameter.AGGREGATION, AGGREGATION_NONE)
              .withParameter(ResourceParameter.MODIFIED_SINCE, from)
              .withParameter(ResourceParameter.MODIFIED_BEFORE, until)
              .withParameter(ResourceParameter.FROM, ZERO)
              .withParameter(ResourceParameter.SIZE, "50")
              .withParameter(ResourceParameter.SORT, ResourceSort.MODIFIED_DATE.asCamelCase())
              .withParameter(ResourceParameter.SORT, ResourceSort.IDENTIFIER.asCamelCase())
              .withParameter(ResourceParameter.SORT_ORDER, "desc")
              .build()
              .withScrollTime(SCROLL_TTL);
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
