package no.sikt.nva.oai.pmh.handler.oaipmh;

import com.google.common.html.HtmlEscapers;

public class MetadataPrefixNotSupportedException extends RuntimeException {
  public MetadataPrefixNotSupportedException(String metadataPrefix) {
    super(
        "Metadata prefix '"
            + HtmlEscapers.htmlEscaper().escape(metadataPrefix)
            + "' is not supported");
  }
}
