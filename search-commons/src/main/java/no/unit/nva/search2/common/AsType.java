package no.unit.nva.search2.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ParameterKind;
import org.joda.time.DateTime;

/**
 * AutoConvert value to Date, Number (or String).
 * <p>Also holds key and can return value as <samp>optional stream</samp></p>
 */
@SuppressWarnings({"PMD.ShortMethodName"})
public class AsType<K extends Enum<K> & ParameterKey> {

    private final String value;
    private final K key;

    public AsType(String value, K key) {
        this.value = value;
        this.key = key;
    }

    public <T> T as() {
        if (isNull(value)) {
            return null;
        }
        if (getKey().fieldType().equals(ParameterKind.CUSTOM)) {
            Query.logger.debug("CUSTOM lacks TypeInfo, use explicit casting if 'String' doesn't cut it.");
        }
        return (T) switch (getKey().fieldType()) {
            case DATE -> castDateTime();
            case NUMBER -> castNumber();
            default -> value;
        };
    }

    public K getKey() {
        return key;
    }

    private <T> T castDateTime() {
        return ((Class<T>) DateTime.class).cast(asDateTime());
    }

    public DateTime asDateTime() {
        return DateTime.parse(value);
    }

    private <T extends Number> T castNumber() {
        return (T) attempt(this::asNumber).orElseThrow();
    }

    public Number asNumber() {
        return Integer.parseInt(value);
    }

    public boolean isEmpty() {
        return isNull(value) || value.isEmpty();
    }

    /**
     * Split.
     * @param delimiter regex to split on
     * @return The value split, or null.
     */
    public String[] split(String delimiter) {
        return nonNull(value)
            ? value.split(delimiter)
            : null;
    }

    /**
     * AsSplitStream.
     * @param delimiter regex to split on
     * @return The value as an optional Stream, split by delimiter.
     */
    public Stream<String> asSplitStream(String delimiter) {
        return asStream()
            .flatMap(value -> Arrays.stream(value.split(delimiter)).sequential());
    }

    /**
     * AsStream.
     * @return The value as an optional Stream.
     */
    public Stream<String> asStream() {
        return Optional.ofNullable(value).stream();
    }

    /**
     * AsBoolean.
     * @return False if value is null or FALSE, otherwise True
     */
    public Boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    public String asLowerCase() {
        return value.toLowerCase(Locale.getDefault());
    }

    @Override
    public String toString() {
        return value;
    }
}
