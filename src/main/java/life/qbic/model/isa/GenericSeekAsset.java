package life.qbic.model.isa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for Seek assets. Contains all mandatory and some optional properties and attributes
 * that are needed to create an asset in SEEK. The model and its getters (names) are structured in a
 * way to enable the easy translation to JSON to use in SEEK queries.
 * Mandatory parameters are found in the constructor, optional attributes can be set using
 * withAttribute(attribute) notation. Since there are different types of assets, the isaType is a
 * parameter here.
 */
public class GenericSeekAsset extends AbstractISAObject {

  private Attributes attributes;
  private Relationships relationships;
  private String assetType;

  public GenericSeekAsset(String assetType, String title, String fileName, List<String> projectIds) {
    this.assetType = assetType;
    this.attributes = new Attributes(title, fileName);
    this.relationships = new Relationships(projectIds);
  }

  public GenericSeekAsset withOtherCreators(String otherCreators) {
    this.attributes.otherCreators = otherCreators;
    return this;
  }

  public GenericSeekAsset withAssays(List<String> assays) {
    this.relationships.assays = assays;
    return this;
  }

  public GenericSeekAsset withDataFormatAnnotations(List<String> identifiers) {
    this.attributes.withDataFormatAnnotations(identifiers);
    return this;
  }

  public String toJson() throws JsonProcessingException {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Relationships.class, new RelationshipsSerializer());
    return super.toJson(module);
  }

  public String getType() {
    return assetType;
  }

  public Relationships getRelationships() {
    return relationships;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public String getFileName() {
    return attributes.getContent_blobs().get(0).getOriginal_filename();
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
    private List<ContentBlob> contentBlobs = new ArrayList<>();
    private String otherCreators = "";
    private List<DataFormatAnnotation> dataFormatAnnotations = new ArrayList<>();


    public Attributes(String title, String fileName) {
      this.title = title;
      this.contentBlobs.add(new ContentBlob(fileName));
    }

    public String getTitle() {
      return title;
    }

    public List<ContentBlob> getContent_blobs() {
      return contentBlobs;
    }

    public String getOther_creators() {
      return otherCreators;
    }

    public List<DataFormatAnnotation> getData_format_annotations() {
      return dataFormatAnnotations;
    }

    public void withDataFormatAnnotations(List<String> identifiers) {
      List<DataFormatAnnotation> annotations = new ArrayList<>();
      for(String id : identifiers) {
        annotations.add(new DataFormatAnnotation(id));
      }
      this.dataFormatAnnotations = annotations;
    }

    private class DataFormatAnnotation {

      private String label;
      private String identifier;

      public DataFormatAnnotation(String identifier) {
        this.identifier = identifier;
      }

      public String getIdentifier() {
        return identifier;
      }

    }

    private class ContentBlob {

      private String originalFilename;
      private String contentType;

      public ContentBlob(String fileName) {
        this.originalFilename = fileName;
        String suffix = fileName.substring(fileName.indexOf('.') + 1);

        this.contentType = "application/" + suffix;
      }

      public String getContent_type() {
        return contentType;
      }

      public String getOriginal_filename() {
        return originalFilename;
      }
    }
  }

}
