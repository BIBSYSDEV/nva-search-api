package no.unit.nva.search.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class InputClass {
    
    public static final String NAME_FIELD = "name";
    @JsonProperty(NAME_FIELD)
    private final String name;
    
    @JsonCreator
    public InputClass(@JsonProperty(NAME_FIELD) String name) {
        this.name = name;
    }
    
    public static InputClass empty() {
        return new InputClass(null);
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getName());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InputClass)) {
            return false;
        }
        InputClass that = (InputClass) o;
        return Objects.equals(getName(), that.getName());
    }
}
