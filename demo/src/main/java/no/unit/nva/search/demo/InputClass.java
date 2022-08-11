package no.unit.nva.search.demo;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class InputClass {
    
    private String name;
    
    @JacocoGenerated
    public String getName() {
        return name;
    }
    
    @JacocoGenerated
    public void setName(String name) {
        this.name = name;
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
