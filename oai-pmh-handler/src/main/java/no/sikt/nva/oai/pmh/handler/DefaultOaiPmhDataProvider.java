package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class DefaultOaiPmhDataProvider implements OaiPmhDataProvider {
  @Override
  public JAXBElement<OAIPMHtype> handleRequest() {
    return notImplemented();
  }

  private JAXBElement<OAIPMHtype> notImplemented() {
    var objectFactory = new ObjectFactory();
    var responseDate =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()));
    var request = objectFactory.createRequestType();
    var error = objectFactory.createOAIPMHerrorType();
    error.setCode(OAIPMHerrorcodeType.BAD_VERB);
    error.setValue("Not implemented yet!");

    var oaiPmhType = objectFactory.createOAIPMHtype();
    oaiPmhType.setResponseDate(responseDate);
    oaiPmhType.setRequest(request);
    oaiPmhType.getError().add(error);

    return objectFactory.createOAIPMH(oaiPmhType);
  }
}
