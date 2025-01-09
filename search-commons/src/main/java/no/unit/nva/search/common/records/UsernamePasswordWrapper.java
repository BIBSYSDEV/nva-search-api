package no.unit.nva.search.common.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

/**
 * UsernamePasswordWrapper is a class that wraps a username and a password.
 *
 * @author Sondre Vestad
 */
public class UsernamePasswordWrapper {
  @JsonProperty("username")
  public String username;

  @JsonProperty("password")
  public String password;

  @JacocoGenerated
  public UsernamePasswordWrapper() {}

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
