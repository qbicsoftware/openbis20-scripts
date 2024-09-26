package life.qbic.model.isa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model class for ISA Assays. Contains all mandatory and some optional properties and attributes
 * that are needed to create assays in SEEK. The model and its getters (names) are structured in a
 * way to enable the easy translation to JSON to use in SEEK queries.
 * Mandatory parameters are found in the constructor, optional attributes can be set using
 * withAttribute(attribute) notation.
 */
public class ISAAssay extends AbstractISAObject {

  private final String ISA_TYPE = "assays";

  private Attributes attributes;
  private Relationships relationships;

  public ISAAssay(String title, String studyId, String assayClass, URI assayType) {
    this.attributes = new Attributes(title, assayClass, assayType);
    this.relationships = new Relationships(studyId);
  }

  public ISAAssay withOtherCreators(String otherCreators) {
    this.attributes.otherCreators = otherCreators;
    return this;
  }
  
  public ISAAssay withTags(List<String> tags) {
    this.attributes.tags = tags;
    return this;
  }

  public ISAAssay withDescription(String description) {
    this.attributes.description = description;
    return this;
  }

  public ISAAssay withTechnologyType(String technologyType) {
    this.attributes.technologyType = technologyType;
    return this;
  }

  public void setCreatorIDs(List<Integer> creators) {
    this.relationships.setCreatorIDs(creators);
  }
  public void setOrganismIDs(List<Integer> organismIDs) {
    this.relationships.setOrganismIDs(organismIDs);
  }
  public void setSampleIDs(List<Integer> sampleIDs) {
    this.relationships.setSampleIDs(sampleIDs);
  }
  public void setDataFileIDs(List<Integer> dataFileIDs) {
    this.relationships.setDataFileIDs(dataFileIDs);
  }
  public void setSOPIDs(List<Integer> sopiDs) {
    this.relationships.setSOPIDs(sopiDs);
  }
  public void setDocumentIDs(List<Integer> documentIDs) {
    this.relationships.setDocumentIDs(documentIDs);
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

  public static void main(String[] args) throws JsonProcessingException, URISyntaxException {
    ISAAssay assay = new ISAAssay("title", "1",
        "EXP",
        new URI("http://jermontology.org/ontology/JERMOntology#RNA-Seq"));
    assay.setCreatorIDs(Arrays.asList(3,2));
    assay.setOrganismIDs(Arrays.asList(123,3332));
    System.err.println(assay.toJson());
  }

  private class Relationships {

    private String studyId;
    private List<Integer> creators = new ArrayList<>();
    private List<Integer> samples = new ArrayList<>();
    private List<Integer> documents = new ArrayList<>();
    private List<Integer> dataFiles = new ArrayList<>();
    private List<Integer> sops = new ArrayList<>();
    private List<Integer> organisms = new ArrayList<>();

    public Relationships(String studyId) {
      this.studyId = studyId;
    }

    public String getStudyId() {
      return studyId;
    }

    public List<Integer> getCreators() {
      return creators;
    }

    public void setCreatorIDs(List<Integer> creators) {
      this.creators = creators;
    }
    public void setSampleIDs(List<Integer> samples) {
      this.samples = samples;
    }
    public void setDocumentIDs(List<Integer> documents) {
      this.documents = documents;
    }
    public void setDataFileIDs(List<Integer> dataFiles) {
      this.dataFiles = dataFiles;
    }
    public void setSOPIDs(List<Integer> sops) {
      this.sops = sops;
    }
    public void setOrganismIDs(List<Integer> organisms) {
      this.organisms = organisms;
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
      jsonGenerator.writeObjectFieldStart("study");
      jsonGenerator.writeObjectFieldStart("data");
      jsonGenerator.writeStringField("id", relationships.getStudyId());
      jsonGenerator.writeStringField("type", "studies");
      jsonGenerator.writeEndObject();
      jsonGenerator.writeEndObject();
      generateListJSON(jsonGenerator, "creators", relationships.getCreators(), "people");
      generateListJSON(jsonGenerator, "samples", relationships.samples, "samples");
      generateListJSON(jsonGenerator, "documents", relationships.documents, "documents");
      generateListJSON(jsonGenerator, "data_files", relationships.dataFiles, "data_files");
      generateListJSON(jsonGenerator, "sops", relationships.sops, "sops");
      generateListJSON(jsonGenerator, "organisms", relationships.organisms, "organisms");

      jsonGenerator.writeEndObject();
    }
  }

  private void generateListJSON(JsonGenerator generator, String name, List<Integer> items, String type)
      throws IOException {
    generator.writeObjectFieldStart(name);
    generator.writeArrayFieldStart("data");
    for(int item : items) {
      generator.writeStartObject();
      generator.writeStringField("id", Integer.toString(item));
      generator.writeStringField("type", type);
      generator.writeEndObject();
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }

  private class Attributes {

    public List<String> tags = new ArrayList<>();
    public String description = "";
    public String technologyType = "";
    private String title;
    private AssayClass assayClass;
    private AssayType assayType;
    private String otherCreators = "";

    public Attributes(String title, String assayClass, URI assayType) {
      this.title = title;
      this.assayClass = new AssayClass(assayClass);
      this.assayType = new AssayType(assayType.toString());
    }

    public List<String> getTags() {
      return tags;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getTechnologyType() {
      return technologyType;
    }

    public AssayClass getAssay_class() {
      return assayClass;
    }

    public AssayType getAssay_type() {
      return assayType;
    }

    public String getOther_creators() {
      return otherCreators;
    }

    private class AssayClass {

      String key;

      public AssayClass(String assayClass) {
        this.key = assayClass;
      }

      public String getKey() {
        return key;
      }
    }

    private class AssayType {

      String uri;

      public AssayType(String assayType) {
        this.uri = assayType;
      }
      public String getUri() {
        return uri;
      }
    }
  }

}
