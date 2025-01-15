package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxbXmlSerializer implements XmlSerializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(JaxbXmlSerializer.class);

  private final Marshaller marshaller;

  public JaxbXmlSerializer(final Marshaller marshaller) {
    this.marshaller = marshaller;
  }

  @Override
  public String serialize(JAXBElement<OAIPMHtype> objectToSerialize) {
    try {
      return marshal(objectToSerialize);
    } catch (JAXBException e) {
      LOGGER.error("Failed to serialize object to xml!", e);
      throw new XmlSerializationException("Invalid xml content encountered!", e);
    }
  }

  private String marshal(JAXBElement<OAIPMHtype> objectToSerialize) throws JAXBException {
    var stringWriter = new StringWriter();
    marshaller.marshal(objectToSerialize, stringWriter);
    return stringWriter.toString();
  }
}
