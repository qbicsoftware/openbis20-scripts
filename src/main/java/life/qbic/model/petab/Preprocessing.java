package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Preprocessing {
  @JsonProperty
  private String method;
  @JsonProperty
  private String description;
  @JsonProperty
  private Arguments arguments;

  @Override
  public String toString() {
    return "Preprocessing{" +
        "method='" + method + '\'' +
        ", description='" + description + '\'' +
        ", arguments=" + arguments +
        '}';
  }
}
