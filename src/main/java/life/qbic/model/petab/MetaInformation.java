package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MetaInformation {

  @JsonProperty
  private ExperimentInformation ExperimentInformation;

  @JsonProperty
  private Units units;
  @JsonProperty
  private PreprocessingInformation PreprocessingInformation;
  @JsonProperty
  private MeasurementData measurementData;
  @JsonProperty
  private ExperimentalCondition experimentalCondition;


  @Override
  public String toString() {
    return "MetaInformation{" +
        "units=" + units +
        ", preprocessingInformation=" + PreprocessingInformation +
        ", measurementData=" + measurementData +
        ", experimentalCondition=" + experimentalCondition +
        '}';
  }

  public Units getUnits() {
    return units;
  }

  public class ExperimentInformation {

    @Override
    public String toString() {
      return "MetaInformation{}";
    }
  }

  public class Units {
    @JsonProperty
    private String measurement;
    @JsonProperty
    private String time;
    @JsonProperty
    private String treatment;
    @JsonProperty
    private String stimulus;
    @JsonProperty
    private Medium medium;
    @JsonProperty
    private CellCountInfo ncells;
    @JsonProperty
    private String measurement_technique;
    @JsonProperty
    private String openBISId;
    @JsonProperty
    private List<String> openBISParentIds;
    @JsonProperty
    private List<String> dateOfExperiment;

    @Override
    public String toString() {
      return "Units{" +
          "measurement='" + measurement + '\'' +
          ", time='" + time + '\'' +
          ", treatment='" + treatment + '\'' +
          ", stimulus='" + stimulus + '\'' +
          ", medium=" + medium +
          ", ncells=" + ncells +
          ", measurement_technique='" + measurement_technique + '\'' +
          ", openBISId='" + openBISId + '\'' +
          ", openBISParentIds=" + openBISParentIds +
          ", dateOfExperiment=" + dateOfExperiment +
          '}';
    }

    public void setOpenbisParentIds(List<String> list) {
      this.openBISParentIds = list;
    }

    public void setOpenbisId(String id) {
      this.openBISId = id;
    }
  }

}
