package no.unit.nva.search.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OutputClass extends InputClass{
    
    @JsonCreator
    public OutputClass(@JsonProperty(NAME_FIELD) String name) {
        super(name);
    }
}
