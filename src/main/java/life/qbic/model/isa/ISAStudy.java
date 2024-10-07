package life.qbic.model.isa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model class for ISA Studies. Contains all mandatory and some optional properties and attributes
 * that are needed to create studies in SEEK. The model and its getters (names) are structured in a
 * way to enable the easy translation to JSON to use in SEEK queries.
 * Mandatory parameters are found in the constructor, optional attributes can be set using
 * withAttribute(attribute) notation.
 */
public class ISAStudy extends AbstractISAObject {

  private Attributes attributes;
  private Relationships relationships;
  private final String ISA_TYPE = "studies";

  public ISAStudy(String title, String investigationId) {
    this.attributes = new Attributes(title);
    this.relationships = new Relationships(investigationId);
  }

  public ISAStudy withDescription(String description) {
    this.attributes.description = description;
    return this;
  }

  public ISAStudy withExperimentalists(String experimentalists) {
    this.attributes.experimentalists = experimentalists;
    return this;
  }

  public ISAStudy withOtherCreators(String otherCreators) {
    this.attributes.otherCreators = otherCreators;
    return this;
  }

  public void setCreatorIDs(List<Integer> creators) {
    this.relationships.setCreatorIDs(creators);
  }

  public String toJson() throws JsonProcessingException {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Relationships.class, new RelationshipsSerializer());
    return super.toJson(module);
  }

  public String getType() {
    return ISA_TYPE;
  }

  public Relationships getRelationships() {
    return relationships;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  private class Relationships {

    private String investigationId;
    private List<Integer> creators = new ArrayList<>();

    public Relationships(String investigationId) {
      this.investigationId = investigationId;
    }

    public String getInvestigationId() {
      return investigationId;
    }

    public List<Integer> getCreators() {
      return creators;
    }

    public void setCreatorIDs(List<Integer> creators) {
      this.creators = creators;
    }
  }

  public class RelationshipsSerializer extends StdSerializer<Relationships> {

    public RelationshipsSerializer(Class<Relationships> t) {
      super(t);
    }

    public RelationshipsSerializer() {
      this(null);
    }

    @Override
    public void serialize(Relationships relationships, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
      jsonGenerator.writeStartObject();
      jsonGenerator.writeObjectFieldStart("investigation");
      jsonGenerator.writeObjectFieldStart("data");
      jsonGenerator.writeStringField("id", relationships.getInvestigationId());
      jsonGenerator.writeStringField("type", "investigations");
      jsonGenerator.writeEndObject();
      jsonGenerator.writeEndObject();
      jsonGenerator.writeObjectFieldStart("creators");
      jsonGenerator.writeArrayFieldStart("data");
      for(int personID : relationships.getCreators()) {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", personID);
        jsonGenerator.writeStringField("type", "people");
        jsonGenerator.writeEndObject();
      }
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();
      jsonGenerator.writeEndObject();
    }
  }

  private class Attributes {

    private String title;
    private String description = "";
    private String experimentalists = "";
    private String otherCreators = "";

    public Attributes(String title) {
      this.title = title;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getExperimentalists() {
      return experimentalists;
    }

    public String getOther_creators() {
      return otherCreators;
    }
  }

}
