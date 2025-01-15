package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public interface XmlSerializer {
  String serialize(JAXBElement<OAIPMHtype> objectToSerialize);
}
