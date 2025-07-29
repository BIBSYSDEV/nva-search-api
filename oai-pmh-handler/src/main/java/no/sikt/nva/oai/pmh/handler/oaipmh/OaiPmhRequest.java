package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.nonNull;

import java.util.Optional;
import no.sikt.nva.oai.pmh.handler.FormUrlencodedBodyParser;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.StringUtils;
import org.openarchives.oai.pmh.v2.VerbType;

public abstract class OaiPmhRequest {
  private static final String PARAMETER_NAME_VERB = "verb";
  private static final String PARAMETER_NAME_FROM = "from";
  private static final String PARAMETER_NAME_UNTIL = "until";
  private static final String PARAMETER_NAME_SET = "set";
  private static final String PARAMETER_NAME_METADATA_PREFIX = "metadataPrefix";
  private static final String PARAMETER_NAME_RESUMPTION_TOKEN = "resumptionToken";
  private static final String NULL_STRING = "null";

  private static final String VERB_MISSING_MESSAGE = "Parameter 'verb' is missing.";
  private static final String NOT_SUPPORTED_VERB_MESSAGE =
      "Parameter 'verb' has a value that is not supported.";

  public static OaiPmhRequest from(RequestInfo requestInfo, String body) {
    final var verb =
        extractParameter(PARAMETER_NAME_VERB, requestInfo, body)
            .orElseThrow(() -> new BadVerbException(VERB_MISSING_MESSAGE));
    if (VerbType.IDENTIFY.value().equals(verb)) {
      return identifyRequest();
    } else if (VerbType.LIST_METADATA_FORMATS.value().equals(verb)) {
      return listMetadataFormatsRequest(requestInfo);
    } else if (VerbType.LIST_SETS.value().equals(verb)) {
      return listSetsRequest(requestInfo, body);
    } else if (VerbType.LIST_RECORDS.value().equals(verb)) {
      return listRecordsRequest(requestInfo, body);
    } else {
      throw new BadVerbException(NOT_SUPPORTED_VERB_MESSAGE);
    }
  }

  private static ListRecordsRequest listRecordsRequest(RequestInfo requestInfo, String body) {
    var resumptionToken =
        extractParameter(PARAMETER_NAME_RESUMPTION_TOKEN, requestInfo, body).orElse(null);
    if (nonNull(resumptionToken)) {
      return new ListRecordsRequest(ResumptionToken.from(resumptionToken).orElseThrow());
    }

    var from = extractParameter(PARAMETER_NAME_FROM, requestInfo, body).orElse(null);
    var until = extractParameter(PARAMETER_NAME_UNTIL, requestInfo, body).orElse(null);
    var metadataPrefix =
        extractParameter(PARAMETER_NAME_METADATA_PREFIX, requestInfo, body).orElse(null);
    var set = extractParameter(PARAMETER_NAME_SET, requestInfo, body).orElse(null);
    return new ListRecordsRequest(from, until, set, metadataPrefix);
  }

  private static ListSetsRequest listSetsRequest(RequestInfo requestInfo, String body) {
    var resumptionToken =
        extractParameter(PARAMETER_NAME_RESUMPTION_TOKEN, requestInfo, body).orElse(null);
    if (nonNull(resumptionToken)) {
      throw new BadArgumentException(
          "Resumption token not supported for method '%s'".formatted(VerbType.LIST_SETS.value()));
    }
    return new ListSetsRequest();
  }

  private static ListMetadataFormatsRequest listMetadataFormatsRequest(RequestInfo requestInfo) {
    var identifier = requestInfo.getQueryParameterOpt("identifier").orElse(null);
    return new ListMetadataFormatsRequest(identifier);
  }

  private static IdentifyRequest identifyRequest() {
    return new IdentifyRequest();
  }

  private static Optional<String> extractParameter(
      String parameterName, RequestInfo requestInfo, String body) {
    if (StringUtils.isEmpty(body) || NULL_STRING.equals(body)) {
      return requestInfo.getQueryParameterOpt(parameterName);
    } else {
      var bodyParser = FormUrlencodedBodyParser.from(body);
      return bodyParser.getValue(parameterName);
    }
  }

  public abstract VerbType getVerbType();
}
