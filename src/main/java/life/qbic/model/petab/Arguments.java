package life.qbic.model.petab;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Arguments {
  @JsonProperty
  List<String> housekeeperObservableIds;

  @Override
  public String toString() {
    return "Arguments{" +
        "housekeeperObservableIds=" + housekeeperObservableIds +
        '}';
  }
}
