package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.Optional;
import java.util.function.Consumer;

public interface NullableWrapper<T> {
  default void ifPresent(Consumer<T> consumer) {
    getValue().ifPresent(consumer);
  }

  default T orElse(T other) {
    return getValue().orElse(other);
  }

  boolean isPresent();

  Optional<T> getValue();
}
