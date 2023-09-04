package no.unit.nva.search2.common;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Predicate;

public interface IParameterKey {

    Operator operator();

    String key();

    Collection<String> swsKey();

    String pattern();

    String errorMessage();

    KeyEncoding encoding();

    static Predicate<IParameterKey> hasValidValue(String value) {
        return f -> {
            var encoded = f.encoding() == KeyEncoding.ENCODE_DECODE
                ? URLDecoder.decode(value, StandardCharsets.UTF_8)
                : value;
            return encoded.matches(f.pattern());
        };
    }

    static Predicate<IParameterKey> equalTo(String name) {
        return key -> name.equals(key.key());
    }

    enum KeyEncoding {
        NONE,DECODE,ENCODE_DECODE
    }

    enum ParamKind {
        DATE, NUMBER, STRING, CUSTOM
    }

    enum Operator {
        EQUALS("%s:%s"),
        GREATER_THAN("%s:>%s"),
        GREATER_THAN_OR_EQUAL_TO("%s:>=%s"),
        LESS_THAN("%s:<%s"),
        LESS_THAN_OR_EQUAL_TO("%s:<=%s"),
        ;

        private final String format;

        Operator(String format) {
            this.format = format;
        }

        public String format() {
            return format;
        }
    }

}