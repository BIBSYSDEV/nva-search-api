package no.unit.nva.search.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class UsernamePasswordWrapper {
    @JsonProperty("username")
    public String username;

    @JsonProperty("password")
    public String password;

    public UsernamePasswordWrapper() {
    }

    @JacocoGenerated
    public UsernamePasswordWrapper(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @JacocoGenerated
    public String getUsername() {
        return username;
    }

    @JacocoGenerated
    public String getPassword() {
        return password;
    }
}
