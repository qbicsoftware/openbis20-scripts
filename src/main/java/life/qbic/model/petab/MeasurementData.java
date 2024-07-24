package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MeasurementData {
  @JsonProperty
  private Measurement measurement;
  @JsonProperty
  private Time time;
  @JsonProperty
  private IdWithPattern replicateId;

  @Override
  public String toString() {
    return "MeasurementData{" +
        "measurement=" + measurement +
        ", time=" + time +
        ", replicateId=" + replicateId +
        '}';
  }
}
