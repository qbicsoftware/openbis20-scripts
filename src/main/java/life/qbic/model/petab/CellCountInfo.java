package life.qbic.model.petab;


import com.fasterxml.jackson.annotation.JsonProperty;

public class CellCountInfo {
  @JsonProperty
  double seeded;
  @JsonProperty
  String ncellsCount;
  @JsonProperty
  String unit;

  @Override
  public String toString() {
    return "CellCountInfo{" +
        "seeded=" + seeded +
        ", ncellsCount='" + ncellsCount + '\'' +
        ", unit='" + unit + '\'' +
        '}';
  }
}

