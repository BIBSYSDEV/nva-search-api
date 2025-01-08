package no.unit.nva.search.model.records;

import nva.commons.core.JacocoGenerated;

/**
 * UsernamePasswordWrapper is a class that wraps a username and a password.
 *
 * @author Sondre Vestad
 */
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
