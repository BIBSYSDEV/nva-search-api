package no.sikt.nva.oai.pmh.handler.oaipmh;

import com.google.common.html.HtmlEscapers;

public class SetNotSupportedException extends RuntimeException {
  public SetNotSupportedException(String set) {
    super("Set '" + HtmlEscapers.htmlEscaper().escape(set) + "' is not supported");
  }
}
