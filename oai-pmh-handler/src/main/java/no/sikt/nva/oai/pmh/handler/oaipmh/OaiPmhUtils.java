package no.sikt.nva.oai.pmh.handler.oaipmh;

import jakarta.xml.bind.JAXBElement;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public final class OaiPmhUtils {
  private OaiPmhUtils() {}

  public static JAXBElement<OAIPMHtype> baseResponse(final ObjectFactory objectFactory) {
    var responseDate =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()));
    var request = objectFactory.createRequestType();
    var oaiPmhType = objectFactory.createOAIPMHtype();
    oaiPmhType.setResponseDate(responseDate);
    oaiPmhType.setRequest(request);

    return objectFactory.createOAIPMH(oaiPmhType);
  }
}
