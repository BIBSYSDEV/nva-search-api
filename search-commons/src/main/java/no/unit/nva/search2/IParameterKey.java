package no.unit.nva.search2;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

public interface IParameterKey {

    String getKey();

    String getPattern();

    String getErrorMessage();

    KeyEncoding encoding();

    static Predicate<IParameterKey> hasValidValue(String value) {
        return f -> {
            var encoded = f.encoding() == KeyEncoding.ENCODE_DECODE
                ? URLDecoder.decode(value, StandardCharsets.UTF_8)
                : value;
            return encoded.matches(f.getPattern());
        };
    }

    static Predicate<IParameterKey> equalTo(String name) {
        return key -> name.equals(key.getKey());
    }

    enum KeyEncoding {
        NONE,DECODE,ENCODE_DECODE
    }

}