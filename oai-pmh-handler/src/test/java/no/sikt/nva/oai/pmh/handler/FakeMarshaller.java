package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

public class FakeMarshaller implements Marshaller {

  @Override
  public void marshal(Object o, Result result) throws JAXBException {}

  @Override
  public void marshal(Object o, OutputStream outputStream) throws JAXBException {}

  @Override
  public void marshal(Object o, File file) throws JAXBException {}

  @Override
  public void marshal(Object o, Writer writer) throws JAXBException {
    throw new JAXBException("Ha ha!!!");
  }

  @Override
  public void marshal(Object o, ContentHandler contentHandler) throws JAXBException {}

  @Override
  public void marshal(Object o, Node node) throws JAXBException {}

  @Override
  public void marshal(Object o, XMLStreamWriter xmlStreamWriter) throws JAXBException {}

  @Override
  public void marshal(Object o, XMLEventWriter xmlEventWriter) throws JAXBException {}

  @Override
  public Node getNode(Object o) throws JAXBException {
    return null;
  }

  @Override
  public void setProperty(String s, Object o) throws PropertyException {}

  @Override
  public Object getProperty(String s) throws PropertyException {
    return null;
  }

  @Override
  public void setEventHandler(ValidationEventHandler validationEventHandler) throws JAXBException {}

  @Override
  public ValidationEventHandler getEventHandler() throws JAXBException {
    return null;
  }

  @Override
  public <A extends XmlAdapter<?, ?>> void setAdapter(A a) {}

  @Override
  public <A extends XmlAdapter<?, ?>> void setAdapter(Class<A> aClass, A a) {}

  @Override
  public <A extends XmlAdapter<?, ?>> A getAdapter(Class<A> aClass) {
    return null;
  }

  @Override
  public void setAttachmentMarshaller(AttachmentMarshaller attachmentMarshaller) {}

  @Override
  public AttachmentMarshaller getAttachmentMarshaller() {
    return null;
  }

  @Override
  public void setSchema(Schema schema) {}

  @Override
  public Schema getSchema() {
    return null;
  }

  @Override
  public void setListener(Listener listener) {}

  @Override
  public Listener getListener() {
    return null;
  }
}
