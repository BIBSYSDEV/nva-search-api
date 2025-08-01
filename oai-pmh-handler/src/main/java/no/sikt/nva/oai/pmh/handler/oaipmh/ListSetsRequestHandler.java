package no.sikt.nva.oai.pmh.handler.oaipmh;

import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;
import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.ZERO;

import jakarta.xml.bind.JAXBElement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.records.Facet;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.SetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListSetsRequestHandler implements OaiPmhRequestHandler<ListSetsRequest> {
  private static final Logger logger = LoggerFactory.getLogger(ListSetsRequestHandler.class);
  private static final String INSTANCE_TYPE_AGGREGATION_NAME = "type";
  private final ResourceClient resourceClient;

  public ListSetsRequestHandler(ResourceClient resourceClient) {
    this.resourceClient = resourceClient;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(ListSetsRequest request) {
    final ResourceSearchQuery query = buildAllAggregationsQuery();

    var instanceTypes = doSearchAndExtractInstanceTypesFromTypeAggregation(resourceClient, query);
    var objectFactory = new ObjectFactory();
    var oaiResponse = baseResponse(objectFactory);
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(request.getVerbType());

    var listSets = objectFactory.createListSetsType();
    listSets.getSet().addAll(generateSets(instanceTypes, objectFactory));
    value.setListSets(listSets);

    return oaiResponse;
  }

  private java.util.Set<String> doSearchAndExtractInstanceTypesFromTypeAggregation(
      ResourceClient resourceClient, ResourceSearchQuery query) {
    final HttpResponseFormatter<ResourceParameter> response;
    try {
      response = query.doSearch(resourceClient, Words.RESOURCES);
    } catch (RuntimeException e) {
      logger.error("Failed to search for publication instance types using 'type' aggregation.", e);
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
              .withParameter(ResourceParameter.AGGREGATION, ALL)
              .withParameter(ResourceParameter.FROM, ZERO)
              .withParameter(ResourceParameter.SIZE, ZERO)
              .build();
    } catch (BadRequestException e) {
      // should never happen unless query validation code is changed!
      logger.error("Failed to search for publication instance types using 'type' aggregation.", e);
      throw new ResourceSearchException("Error looking up instance types using aggregations.", e);
    }
    return query;
  }

  private List<SetType> generateSets(
      java.util.Set<String> instanceTypes, ObjectFactory objectFactory) {
    var setTypes = new LinkedList<SetType>();
    var setQualifier = objectFactory.createSetType();
    setQualifier.setSetSpec(SetRoot.RESOURCE_TYPE_GENERAL.getValue());
    setQualifier.setSetName(SetRoot.RESOURCE_TYPE_GENERAL.getValue());

    setTypes.add(setQualifier);

    var subSetTypes =
        instanceTypes.stream()
            .map(instanceType -> new SetSpec(SetRoot.RESOURCE_TYPE_GENERAL, instanceType))
            .map(setSpec -> wrap(setSpec, objectFactory))
            .toList();

    setTypes.addAll(subSetTypes);
    return setTypes;
  }

  private SetType wrap(SetSpec setSpec, ObjectFactory objectFactory) {
    var setType = objectFactory.createSetType();
    setType.setSetSpec(setSpec.getValue().orElseThrow());
    setType.setSetName(setSpec.children()[0]);
    return setType;
  }
}
