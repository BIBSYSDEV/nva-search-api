package no.sikt.nva.oai.pmh.handler.oaipmh.handlers;

import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import java.util.LinkedList;
import java.util.List;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.ListSetsRequest;
import no.sikt.nva.oai.pmh.handler.repository.ResourceRepository;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.SetType;

public class ListSetsRequestHandler implements OaiPmhRequestHandler<ListSetsRequest> {
  private final ResourceRepository resourceRepository;

  public ListSetsRequestHandler(ResourceRepository resourceRepository) {
    this.resourceRepository = resourceRepository;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(ListSetsRequest request) {
    var sets = resourceRepository.fetchSetsFromAggregations();
    var objectFactory = new ObjectFactory();
    var oaiResponse = baseResponse(objectFactory);
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(request.getVerbType());

    var listSets = objectFactory.createListSetsType();
    listSets
        .getSet()
        .addAll(generateSets(SetRoot.RESOURCE_TYPE_GENERAL, sets.instanceTypes(), objectFactory));
    listSets
        .getSet()
        .addAll(generateSets(SetRoot.INSTITUTION, sets.institutionIdentifiers(), objectFactory));
    value.setListSets(listSets);

    return oaiResponse;
  }

  private List<SetType> generateSets(
      SetRoot setRoot, java.util.Set<String> values, ObjectFactory objectFactory) {
    var setTypes = new LinkedList<SetType>();
    var setQualifier = objectFactory.createSetType();
    setQualifier.setSetSpec(setRoot.getValue());
    setQualifier.setSetName(setRoot.getValue());

    setTypes.add(setQualifier);

    var subSetTypes =
        values.stream()
            .map(instanceType -> new SetSpec(setRoot, instanceType))
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
