package no.sikt.nva.oai.pmh.handler;

import static nva.commons.core.attempt.Try.attempt;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public class JaxbXmlSerializer implements XmlSerializer<JAXBElement<OAIPMHtype>> {
  private final Marshaller marshaller;

  public JaxbXmlSerializer() {
    marshaller = attempt(JaxbXmlSerializer::createMarshaller).orElseThrow();
  }

  private static Marshaller createMarshaller() throws JAXBException {
    var context = JAXBContext.newInstance(OAIPMHtype.class);
    return context.createMarshaller();
  }

  @Override
  public String serialize(JAXBElement<OAIPMHtype> objectToSerialize) {
    return attempt(() -> marshal(objectToSerialize)).orElseThrow();
  }

  private String marshal(JAXBElement<OAIPMHtype> objectToSerialize) throws JAXBException {
    var stringWriter = new StringWriter();
    marshaller.marshal(objectToSerialize, stringWriter);
    return stringWriter.toString();
  }
}
