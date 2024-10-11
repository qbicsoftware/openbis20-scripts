package life.qbic.model.isa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Used to create the outer "data" node of all SEEK json objects.
 */
public abstract class AbstractISAObject {

  public String toJson(SimpleModule module) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(this);
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\"data\":");
    jsonBuilder.append(json);
    jsonBuilder.append("}");
    return jsonBuilder.toString();
  }

}
