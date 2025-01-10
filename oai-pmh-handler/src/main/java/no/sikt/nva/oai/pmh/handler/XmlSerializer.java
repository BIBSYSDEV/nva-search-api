package no.sikt.nva.oai.pmh.handler;

public interface XmlSerializer<T> {
  String serialize(T objectToSerialize);
}
