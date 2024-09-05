package no.unit.nva.search.endpoint;

import java.util.HashSet;
import java.util.Objects;

public final class ParameterCollection {
    private final HashSet<Parameter> parameters;

    public ParameterCollection(HashSet<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ParameterCollection) obj;
        return Objects.equals(this.parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }

    @Override
    public String toString() {
        return "Parameters[" + "parameters=" + parameters + ']';
    }
}
