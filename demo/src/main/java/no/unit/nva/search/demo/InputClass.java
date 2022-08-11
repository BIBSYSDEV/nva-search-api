package no.unit.nva.search.demo;

import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class InputClass implements JsonSerializable {
    
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
    
    @Override
    public String toString(){
        return toJsonString();
    }
}
