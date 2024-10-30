package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreprocessingInformation {
  @JsonProperty
  private String normalizationStatus;
  @JsonProperty
  private Preprocessing preprocessing;

  @Override
  public String toString() {
    return "PreprocessingInformation{" +
        "normalizationStatus='" + normalizationStatus + '\'' +
        ", preprocessing=" + preprocessing +
        '}';
  }
}
