package life.qbic.model.petab;


import com.fasterxml.jackson.annotation.JsonProperty;

public class IdWithPattern {
  @JsonProperty
  String pattern;

  public IdWithPattern() {}

  public IdWithPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public String toString() {
    return "IdWithPattern{" +
        "pattern='" + pattern + '\'' +
        '}';
  }
}