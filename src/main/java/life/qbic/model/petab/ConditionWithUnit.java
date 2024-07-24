package life.qbic.model.petab;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ConditionWithUnit {
  @JsonProperty
  String name;
  @JsonProperty
  String unit;

  public ConditionWithUnit() {}

  public ConditionWithUnit(String name, String unit) {
    this.name = name;
    this.unit = unit;
  }

  @Override
  public String toString() {
    return "ConditionWithUnit{" +
        "name='" + name + '\'' +
        ", unit='" + unit + '\'' +
        '}';
  }
}