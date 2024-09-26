package life.qbic.model.isa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for ISA Samples. Contains all mandatory and some optional properties and attributes
 * that are needed to create samples in SEEK. The model and its getters (names) are structured in a
 * way to enable the easy translation to JSON to use in SEEK queries.
 * Mandatory parameters are found in the constructor, optional attributes can be set using
 * withAttribute(attribute) notation.
 */
public class ISASample extends AbstractISAObject {

  private Attributes attributes;
  private Relationships relationships;
  private final String ISA_TYPE = "samples";

  public ISASample(Map<String, Object> attributeMap, String sampleType, List<String> projectIds) {
    this.attributes = new Attributes(attributeMap);
    this.relationships = new Relationships(sampleType, projectIds);
  }

  public ISASample withOtherCreators(String otherCreators) {
    this.attributes.otherCreators = otherCreators;
    return this;
  }

  public void setCreatorIDs(List<String> creatorIDs) {
    this.relationships.setCreatorIDs(creatorIDs);
  }

  public void setAssayIDs(List<String> assayIDs) {
    this.relationships.setAssayIDs(assayIDs);
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

  public static void main(String[] args) throws JsonProcessingException {
    ISASample sample = new ISASample(new HashMap<>(), "1", Arrays.asList("1"));
    sample.setCreatorIDs(Arrays.asList("3","2"));
    System.err.println(sample.toJson());
  }

  private class Relationships {

    private String sampleType;
    private List<String> projects;
    private List<String> creators = new ArrayList<>();
    private List<String> assays = new ArrayList<>();

    public Relationships(String sampleType, List<String> projects) {
      this.projects = projects;
      this.sampleType = sampleType;
    }

    public String getSample_type() {
      return sampleType;
    }

    public List<String> getAssays() {
      return assays;
    }

    public void setAssayIDs(List<String> assays) {
      this.assays = assays;
    }

    public List<String> getProjects() {
      return projects;
    }

    public List<String> getCreators() {
      return creators;
    }

    public void setCreatorIDs(List<String> creators) {
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
      jsonGenerator.writeObjectFieldStart("sample_type");
      jsonGenerator.writeObjectFieldStart("data");
      jsonGenerator.writeStringField("id", relationships.sampleType);
      jsonGenerator.writeStringField("type", "sample_types");
      jsonGenerator.writeEndObject();
      jsonGenerator.writeEndObject();

      generateListJSON(jsonGenerator, "creators", relationships.getCreators(), "people");
      generateListJSON(jsonGenerator, "projects", relationships.projects, "projects");
      generateListJSON(jsonGenerator, "assays", relationships.assays, "assays");

      jsonGenerator.writeEndObject();
    }
  }

  private void generateListJSON(JsonGenerator generator, String name, List<String> items, String type)
      throws IOException {
    generator.writeObjectFieldStart(name);
    generator.writeArrayFieldStart("data");
    for(String item : items) {
      generator.writeStartObject();
      generator.writeStringField("id", item);
      generator.writeStringField("type", type);
      generator.writeEndObject();
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }

  private class Attributes {

    private Map<String,Object> attributeMap = new HashMap<>();
    private String otherCreators = "";

    public Attributes(Map<String, Object> attributeMap) {
      this.attributeMap = attributeMap;
    }

    public Map<String, Object> getAttribute_map() {
      return attributeMap;
    }

    public String getOther_creators() {
      return otherCreators;
    }
  }

}
