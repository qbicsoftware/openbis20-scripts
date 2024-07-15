package life.qbic.model.petab;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ConditionWithUnit {
  @JsonProperty
  String name;
  @JsonProperty
  String unit;

  @Override
  public String toString() {
    return "ConditionWithUnit{" +
        "name='" + name + '\'' +
        ", unit='" + unit + '\'' +
        '}';
  }
}