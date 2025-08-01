package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import java.util.Optional;
import no.sikt.nva.oai.pmh.handler.FormUrlencodedBodyParser;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.StringUtils;

public final class RequestUtils {
  private static final String NULL_STRING = "null";

  private RequestUtils() {}

  public static Optional<String> extractParameter(
      OaiPmhParameterName parameterName, RequestInfo requestInfo, String body) {
    if (StringUtils.isEmpty(body) || NULL_STRING.equals(body)) {
      return requestInfo.getQueryParameterOpt(parameterName.getName());
    } else {
      var bodyParser = FormUrlencodedBodyParser.from(body);
      return bodyParser.getValue(parameterName.getName());
    }
  }
}
