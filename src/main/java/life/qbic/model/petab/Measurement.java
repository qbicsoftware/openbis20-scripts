package life.qbic.model.petab;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Measurement {
  @JsonProperty
  String unit;
  @JsonProperty
  String lloq;

  @Override
  public String toString() {
    return "Measurement{" +
        "unit='" + unit + '\'' +
        ", lloq='" + lloq + '\'' +
        '}';
  }
}