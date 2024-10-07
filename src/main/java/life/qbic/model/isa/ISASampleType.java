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
 * Model class for SampleType. Contains all mandatory and some optional properties and attributes
 * that are needed to create a sample type in SEEK. The model and its getters (names) are structured
 * in a way to enable the easy translation to JSON to use in SEEK queries.
 * Mandatory parameters are found in the constructor, optional attributes can be set using
 * withAttribute(attribute) notation.
 * Can be used to populate a SEEK installation with sample types taken from another system's API.
 */
public class ISASampleType extends AbstractISAObject {

  private Attributes attributes;
  private Relationships relationships;
  private final String ISA_TYPE = "sample_types";

  public ISASampleType(String title, SampleAttribute titleAttribute, String projectID) {
    this.attributes = new Attributes(title, titleAttribute);
    this.relationships = new Relationships(Arrays.asList(projectID));
  }

  public void addSampleAttribute(String title, SampleAttributeType sampleAttributeType,
      boolean required, String linkedSampleTypeIdOrNull) {
    attributes.addSampleAttribute(title, sampleAttributeType, required, linkedSampleTypeIdOrNull);
  }

  public ISASampleType withAssays(List<String> assays) {
    this.relationships.assays = assays;
    return this;
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

    private List<String> projects;
    private List<String> assays;

    public Relationships(List<String> projects) {
      this.projects = projects;
      this.assays = new ArrayList<>();
    }

    public List<String> getProjects() {
      return projects;
    }

    public List<String> getAssays() {
      return assays;
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

      generateListJSON(jsonGenerator, "projects", relationships.projects, "projects");
      generateListJSON(jsonGenerator, "assays", relationships.assays, "assays");

      jsonGenerator.writeEndObject();
    }
  }

  private void generateListJSON(JsonGenerator generator, String name, List<String> items,
      String type)
      throws IOException {
    generator.writeObjectFieldStart(name);
    generator.writeArrayFieldStart("data");
    for (String item : items) {
      generator.writeStartObject();
      generator.writeStringField("id", item);
      generator.writeStringField("type", type);
      generator.writeEndObject();
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }

  private class Attributes {

    private String title;
    private List<SampleAttribute> sampleAttributes = new ArrayList<>();;

    public Attributes(String title, SampleAttribute titleAttribute) {
      this.title = title;
      if(!titleAttribute.isTitle) {
        throw new IllegalArgumentException("The first sample attribute must be the title attribute.");
      }
      this.sampleAttributes.add(titleAttribute);
    }

    public void addSampleAttribute(String title, SampleAttributeType sampleAttributeType,
        boolean required, String linkedSampleTypeIdOrNull) {
      SampleAttribute sampleAttribute = new SampleAttribute(title, sampleAttributeType, false,
          required).withLinkedSampleTypeId(linkedSampleTypeIdOrNull);
      sampleAttributes.add(sampleAttribute);
    }

    public String getTitle() {
      return title;
    }

    public List<SampleAttribute> getSample_attributes() {
      return sampleAttributes;
    }
  }

  public static class SampleAttribute {

    private String title;
    private String description;
    private SampleAttributeType sampleAttributeType;
    private boolean isTitle;
    private boolean required;
    private String linkedSampleTypeId;

    public SampleAttribute(String title, SampleAttributeType sampleAttributeType, boolean isTitle,
        boolean required) {
      this.title = title;
      this.isTitle = isTitle;
      this.required = required;
      this.sampleAttributeType = sampleAttributeType;
    }

    public SampleAttribute withDescription(String description) {
      this.description = description;
      return this;
    }

    public SampleAttribute withLinkedSampleTypeId(String linkedSampleTypeId) {
      this.linkedSampleTypeId = linkedSampleTypeId;
      return this;
    }

    public SampleAttributeType getSample_attribute_type() {
      return sampleAttributeType;
    }

    public String getLinked_sample_type_id() {
      return linkedSampleTypeId;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public boolean getRequired() {
      return required;
    }

    public boolean getIs_title() {
      return isTitle;
    }
  }

  public static class SampleAttributeType {
    private String id;
    private String title;
    private String baseType;

    public SampleAttributeType(String id, String title, String baseType) {
      this.id = id;
      this.title = title;
      this.baseType = baseType;
    }

    public String getBase_type() {
      return baseType;
    }

    public String getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }
  }
}
