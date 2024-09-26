package no.unit.nva.search.common.jwt;

/**
 * Abstract class for providing a cached value.
 *
 * @author Sondre Vestad
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
