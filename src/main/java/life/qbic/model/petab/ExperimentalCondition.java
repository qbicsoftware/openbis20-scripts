package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public class ExperimentalCondition {
  @JsonProperty
  IdWithPattern conditionId;
  @JsonProperty
  List<ConditionWithUnit> conditions;

  public ExperimentalCondition() {}

  public ExperimentalCondition(IdWithPattern pattern, List<ConditionWithUnit> conditions) {
    this.conditionId = pattern;
    this.conditions = conditions;
  }

  public void setConditions(List<ConditionWithUnit> conditions) {
    this.conditions = conditions;
  }

  public void setConditionId(IdWithPattern id) {
    this.conditionId = id;
  }

  @Override
  public String toString() {
    return "ExperimentalCondition{" +
        "conditionId=" + conditionId +
        ", conditions=" + conditions +
        '}';
  }
}