package no.sikt.nva.oai.pmh.handler.data;

import java.net.URI;
import no.sikt.nva.oai.pmh.handler.data.ResourceDocumentFactory.ResourceDocumentBuilder;

public abstract class AbstractReferenceBuilder<T extends AbstractReferenceBuilder<T>> {
  protected URI referenceDoi;

  public abstract ResourceDocumentBuilder apply();

  public T withReferenceDoi(URI referenceDoi) {
    this.referenceDoi = referenceDoi;
    //noinspection unchecked
    return (T) this;
  }
}
