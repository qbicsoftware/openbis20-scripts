package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public class ExperimentalCondition {
  @JsonProperty
  IdWithPattern conditionId;
  @JsonProperty
  List<ConditionWithUnit> conditions;

  @Override
  public String toString() {
    return "ExperimentalCondition{" +
        "conditionId=" + conditionId +
        ", conditions=" + conditions +
        '}';
  }
}