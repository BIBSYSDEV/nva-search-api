package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import javax.xml.stream.XMLStreamException;
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
    } catch (JAXBException | XMLStreamException e) {
      LOGGER.error("Failed to serialize object to xml!", e);
      throw new XmlSerializationException("Invalid xml content or streaming issue encountered!", e);
    }
  }

  private String marshal(final JAXBElement<OAIPMHtype> objectToSerialize)
      throws JAXBException, XMLStreamException {
    var writer = new StringWriter();
    marshaller.marshal(objectToSerialize, writer);
    return writer.toString();
  }
}
