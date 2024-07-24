package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Time {
  @JsonProperty
  String unit;

  @Override
  public String toString() {
    return "Time{" +
        "unit='" + unit + '\'' +
        '}';
  }
}
