package no.unit.nva.search.models;

import nva.commons.core.JacocoGenerated;

public record UsernamePasswordWrapper(String username, String password) {

    @JacocoGenerated
    public String getUsername() {
        return username;
    }

    @JacocoGenerated
    public String getPassword() {
        return password;
    }
}
