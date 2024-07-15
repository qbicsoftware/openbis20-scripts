package life.qbic.model.petab;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Medium {
  @JsonProperty
  String type;
  @JsonProperty
  double volume;
  @JsonProperty
  String unit;

  @Override
  public String toString() {
    return "Medium{" +
        "type='" + type + '\'' +
        ", volume=" + volume +
        ", unit='" + unit + '\'' +
        '}';
  }
}