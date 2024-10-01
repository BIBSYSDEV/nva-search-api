package no.unit.nva.search.common.jwt;

/**
 * Abstract class for providing a cached value.
 *
 * @author Sondre Vestad
 * @param <T> the type of the cached value
 */
public abstract class CachedValueProvider<T> {

    protected T cachedValue;

    public T getValue() {
        if (cachedValue == null || isExpired()) {
            cachedValue = getNewValue();
        }
        return cachedValue;
    }

    protected abstract boolean isExpired();

    protected abstract T getNewValue();
}
