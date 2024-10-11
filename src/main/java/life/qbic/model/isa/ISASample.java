package life.qbic.model.isa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
  private String id;

  public ISASample(String title, Map<String, Object> attributeMap, String sampleTypeId,
      List<String> projectIds) {
    this.attributes = new Attributes(title, attributeMap);
    this.relationships = new Relationships(sampleTypeId, projectIds);
  }

  public ISASample withOtherCreators(String otherCreators) {
    this.attributes.otherCreators = otherCreators;
    return this;
  }

  public void setSampleID(String seekID) {
    this.id = seekID;
  }

  public void setCreatorIDs(List<String> creatorIDs) {
    this.relationships.setCreatorIDs(creatorIDs);
  }

  public void setAssayIDs(List<String> assayIDs) {
    this.relationships.setAssayIDs(assayIDs);
  }

  public String getId() {
    return id;
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

  public Map<String, Object> fetchCopyOfAttributeMap() {
    return new HashMap<>(attributes.getAttribute_map());
  }

  private class Relationships {

    private String sampleTypeId;
    private List<String> projects;
    private List<String> creators = new ArrayList<>();
    private List<String> assays = new ArrayList<>();

    public Relationships(String sampleTypeId, List<String> projects) {
      this.projects = projects;
      this.sampleTypeId = sampleTypeId;
    }

    public String getSample_type() {
      return sampleTypeId;
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
      jsonGenerator.writeStringField("id", relationships.sampleTypeId);
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
    private String title;

    public Attributes(String title, Map<String, Object> attributeMap) {
      this.attributeMap = attributeMap;
      this.title = title;
    }

    public String getTitle() {
      return title;
    }

    public Map<String, Object> getAttribute_map() {
      return attributeMap;
    }

    public String getOther_creators() {
      return otherCreators;
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ISASample)) {
      return false;
    }

    ISASample isaSample = (ISASample) o;
    return Objects.equals(attributes, isaSample.attributes) && Objects.equals(
        relationships, isaSample.relationships);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(attributes);
    result = 31 * result + Objects.hashCode(relationships);
    result = 31 * result + ISA_TYPE.hashCode();
    return result;
  }
}
