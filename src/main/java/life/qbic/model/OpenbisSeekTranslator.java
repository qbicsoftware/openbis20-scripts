package life.qbic.model;

import static java.util.Map.entry;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import life.qbic.App;
import life.qbic.io.PropertyReader;
import life.qbic.model.isa.GenericSeekAsset;
import life.qbic.model.isa.ISAAssay;
import life.qbic.model.isa.ISASample;
import life.qbic.model.isa.ISASampleType;
import life.qbic.model.isa.ISASampleType.SampleAttribute;
import life.qbic.model.isa.ISASampleType.SampleAttributeType;
import life.qbic.model.isa.SeekStructure;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class OpenbisSeekTranslator {

  private final String DEFAULT_PROJECT_ID;
  private String INVESTIGATION_ID;
  private String STUDY_ID;
  private final String openBISBaseURL;
  private Map<String, String> experimentTypeToAssayClass;
  private Map<DataType, SampleAttributeType> dataTypeToAttributeType;
  private Map<String, String> datasetTypeToAssetType;
  private Map<String, String> experimentTypeToAssayType;

  public OpenbisSeekTranslator(String openBISBaseURL, String defaultProjectID)
      throws IOException, ParserConfigurationException, SAXException {
    this.openBISBaseURL = openBISBaseURL;
    this.DEFAULT_PROJECT_ID = defaultProjectID;
    parseConfigs();
    if(!App.configProperties.containsKey("seek_openbis_sample_title")) {
      throw new RuntimeException("Script can not be run, since 'seek_openbis_sample_title' was not "
          + "provided.");
    }
  }

  /**
   * Parses mandatory mapping information from mandatory config files. Other files may be added.
   */
  private void parseConfigs() throws IOException, ParserConfigurationException, SAXException {
    final String dataTypeToAttributeType = "openbis_datatype_to_seek_attributetype.xml";
    final String datasetToAssaytype = "dataset_type_to_assaytype.properties";
    final String experimentTypeToAssayClass = "experiment_type_to_assay_class.properties";
    final String experimentTypeToAssayType = "openbis_experiment_type_to_seek_assaytype.properties";
    this.experimentTypeToAssayType = PropertyReader.getProperties(experimentTypeToAssayType);
    this.datasetTypeToAssetType = PropertyReader.getProperties(datasetToAssaytype);
    this.experimentTypeToAssayClass = PropertyReader.getProperties(experimentTypeToAssayClass);
    this.dataTypeToAttributeType = parseAttributeXML(dataTypeToAttributeType);
  }

  private Map<DataType, SampleAttributeType> parseAttributeXML(String dataTypeToAttributeType)
      throws ParserConfigurationException, IOException, SAXException {
    Map<DataType, SampleAttributeType> result = new HashMap<>();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(dataTypeToAttributeType);
    NodeList elements = document.getElementsByTagName("entry");
    for (int i = 0; i < elements.getLength(); i++) {
      Node node = elements.item(i);
      DataType openbisType = DataType.valueOf(node.getAttributes()
          .getNamedItem("type")
          .getNodeValue());
      NodeList nodes = node.getChildNodes();
      String seekId = "", seekTitle = "", seekBaseType = "";
      for (int j = 0; j < nodes.getLength(); j++) {
        Node n = nodes.item(j);
        if (n.getNodeName().equals("seek_id")) {
          seekId = n.getTextContent();
        }
        if (n.getNodeName().equals("seek_type")) {
          seekBaseType = n.getTextContent();
        }
        if (n.getNodeName().equals("seek_title")) {
          seekTitle = n.getTextContent();
        }
      }
      result.put(openbisType, new SampleAttributeType(seekId, seekTitle, seekBaseType));
    }
    return result;
  }

  private String generateOpenBISLinkFromPermID(String entityType, String permID) {
    StringBuilder builder = new StringBuilder();
    builder.append(openBISBaseURL);
    builder.append("#entity=");
    builder.append(entityType);
    builder.append("&permId=");
    builder.append(permID);
    return builder.toString();
  }

  /**
   * not mandatory, but nice to have?
   */
  Map<String, String> fileExtensionToDataFormat = Map.ofEntries(
      entry("fastq.gz", "http://edamontology.org/format_1930"),
      entry("fastq", "http://edamontology.org/format_1930"),
      entry("json", "http://edamontology.org/format_3464"),
      entry("yaml", "http://edamontology.org/format_3750"),
      entry("raw", "http://edamontology.org/format_3712"),
      entry("tsv", "http://edamontology.org/format_3475"),
      entry("csv", "http://edamontology.org/format_3752"),
      entry("txt", "http://edamontology.org/format_2330")
      );

  public ISASampleType translate(SampleType sampleType) {
    SampleAttribute titleAttribute = new SampleAttribute(
        App.configProperties.get("seek_openbis_sample_title"),
        dataTypeToAttributeType.get(DataType.VARCHAR), true, false);
    ISASampleType type = new ISASampleType(sampleType.getCode(), titleAttribute,
        DEFAULT_PROJECT_ID);
    for (PropertyAssignment a : sampleType.getPropertyAssignments()) {
      DataType dataType = a.getPropertyType().getDataType();
      type.addSampleAttribute(a.getPropertyType().getLabel(), dataTypeToAttributeType.get(dataType),
          false, null);
    }
    return type;
  }

  public String assetForDatasetType(String datasetType) {
    if(datasetTypeToAssetType.get(datasetType) == null || datasetTypeToAssetType.get(datasetType).isBlank()) {
      throw new RuntimeException("Dataset type " + datasetType + " could not be mapped to SEEK type.");
    }
    return datasetTypeToAssetType.get(datasetType);
  }

  public String dataFormatAnnotationForExtension(String fileExtension) {
    return fileExtensionToDataFormat.get(fileExtension);
  }

  public SeekStructure translate(OpenbisExperimentWithDescendants experiment,
      Map<String, String> sampleTypesToIds, Set<String> blacklist, boolean transferData)
      throws URISyntaxException {

    Experiment exp = experiment.getExperiment();
    String expType = exp.getType().getCode();
    String title = exp.getCode()+" ("+exp.getPermId().getPermId()+")";
    String assayType = experimentTypeToAssayType.get(expType);

    if(assayType ==null || assayType.isBlank()) {
      throw new RuntimeException("Could not find assay type for " + expType+". A mapping needs to "
          + "be added to the respective properties file.");
    }
      ISAAssay assay = new ISAAssay(title, STUDY_ID, experimentTypeToAssayClass.get(expType),
          new URI(assayType));

    SeekStructure result = new SeekStructure(assay, exp.getIdentifier().getIdentifier());

    for(Sample sample : experiment.getSamples()) {
      SampleType sampleType = sample.getType();

      //try to put all attributes into sample properties, as they should be a 1:1 mapping
      Map<String, String> typeCodesToNames = new HashMap<>();
      Set<String> propertiesLinkingSamples = new HashSet<>();
      for (PropertyAssignment a : sampleType.getPropertyAssignments()) {
        String code = a.getPropertyType().getCode();
        String label = a.getPropertyType().getLabel();
        DataType type = a.getPropertyType().getDataType();
        typeCodesToNames.put(code, label);
        if(type.equals(DataType.SAMPLE)) {
          propertiesLinkingSamples.add(code);
        }
      }
      Map<String, Object> attributes = new HashMap<>();
      for(String code : sample.getProperties().keySet()) {
        String value = sample.getProperty(code);
        if(propertiesLinkingSamples.contains(code)) {
          value = generateOpenBISLinkFromPermID("SAMPLE", value);
        }
        attributes.put(typeCodesToNames.get(code), value);
      }

      String sampleID = sample.getIdentifier().getIdentifier();
      attributes.put(App.configProperties.get("seek_openbis_sample_title"), sampleID);
      String sampleTypeId = sampleTypesToIds.get(sampleType.getCode());
      ISASample isaSample = new ISASample(sample.getIdentifier().getIdentifier(), attributes,
          sampleTypeId, Collections.singletonList(DEFAULT_PROJECT_ID));
      result.addSample(isaSample, sampleID);
    }

    //create ISA files for assets. If actual data is to be uploaded is determined later based on flag
    for(DatasetWithProperties dataset : experiment.getDatasets()) {
      String permID = dataset.getCode();
      if(!blacklist.contains(permID)) {
        for(DataSetFile file : experiment.getFilesForDataset(permID)) {
          String datasetType = getDatasetTypeOfFile(file, experiment.getDatasets());
          datasetFileToSeekAsset(file, datasetType, transferData)
              .ifPresent(seekAsset -> result.addAsset(seekAsset, file));
        }
      }
    }
    return result;
  }

  public SeekStructure translate(OpenbisSampleWithDatasets sampleWithDatasets,
      Map<String, String> sampleTypesToIds, Set<String> blacklist, boolean transferData) {
    Sample sample = sampleWithDatasets.getSample();
    SampleType sampleType = sample.getType();
    //try to put all attributes into sample properties, as they should be a 1:1 mapping
    Map<String, String> typeCodesToNames = new HashMap<>();
    Set<String> propertiesLinkingSamples = new HashSet<>();
    for (PropertyAssignment a : sampleType.getPropertyAssignments()) {
      String code = a.getPropertyType().getCode();
      String label = a.getPropertyType().getLabel();
      DataType type = a.getPropertyType().getDataType();
      typeCodesToNames.put(code, label);
      if (type.equals(DataType.SAMPLE)) {
        propertiesLinkingSamples.add(code);
      }
    }
    Map<String, Object> attributes = new HashMap<>();
    for (String code : sample.getProperties().keySet()) {
      String value = sample.getProperty(code);
      if (propertiesLinkingSamples.contains(code)) {
        value = generateOpenBISLinkFromPermID("SAMPLE", value);
      }
      attributes.put(typeCodesToNames.get(code), value);
    }

    String sampleID = sample.getIdentifier().getIdentifier();
    attributes.put(App.configProperties.get("seek_openbis_sample_title"), sampleID);
    String sampleTypeId = sampleTypesToIds.get(sampleType.getCode());
    ISASample isaSample = new ISASample(sample.getIdentifier().getIdentifier(), attributes,
        sampleTypeId, Collections.singletonList(DEFAULT_PROJECT_ID));
    SeekStructure result = new SeekStructure(isaSample, sampleID);

    //create ISA files for assets. If actual data is to be uploaded is determined later based on flag
    List<DatasetWithProperties> datasets = sampleWithDatasets.getDatasets();
    for (DatasetWithProperties dataset : datasets) {
      String permID = dataset.getCode();
      if (!blacklist.contains(permID)) {
        for (DataSetFile file : sampleWithDatasets.getFilesForDataset(permID)) {
          String datasetType = getDatasetTypeOfFile(file, datasets);
          datasetFileToSeekAsset(file, datasetType, transferData)
              .ifPresent(seekAsset -> result.addAsset(seekAsset, file));
        }
      }
    }
    return result;
  }

  public SeekStructure translate(Pair<DatasetWithProperties, List<DataSetFile>> datasetWithFiles,
      Set<String> blacklist, boolean transferData) {
    Map<GenericSeekAsset, DataSetFile> assetToDatasetFiles = new HashMap<>();
    DatasetWithProperties dataset = datasetWithFiles.getLeft();
    String permID = dataset.getCode();
    String datasetType = dataset.getType().getCode();

    if (!blacklist.contains(permID)) {
      for (DataSetFile file : datasetWithFiles.getRight()) {
        datasetFileToSeekAsset(file, datasetType, transferData)
            .ifPresent(seekAsset -> assetToDatasetFiles.put(seekAsset, file));
      }
    }
    return new SeekStructure(assetToDatasetFiles);
  }

  private String getDatasetTypeOfFile(DataSetFile file, List<DatasetWithProperties> dataSets) {
    String permId = file.getDataSetPermId().getPermId();
    for(DatasetWithProperties dataset : dataSets) {
      if(dataset.getCode().equals(permId)) {
        return dataset.getType().getCode();
      }
    }
    return "";
  }

  /**
   * Creates a SEEK asset from an openBIS DataSetFile, if it describes a file (not a folder).
   * @param file the openBIS DataSetFile
   * @return an optional SEEK asset
   */
  private Optional<GenericSeekAsset> datasetFileToSeekAsset(DataSetFile file, String datasetType,
      boolean transferData) {
    if (!file.getPath().isBlank() && !file.isDirectory()) {
      File f = new File(file.getPath());
      String datasetPermID = file.getDataSetPermId().toString();
      String assetName = datasetPermID + ": " + f.getName();
      String assetType = assetForDatasetType(datasetType);
      GenericSeekAsset isaFile = new GenericSeekAsset(assetType, assetName, file.getPath(),
          Arrays.asList(DEFAULT_PROJECT_ID), file.getFileLength());
      //reference the openbis dataset in the description - if transferData flag is false, this will
      //also add a second link instead of the (non-functional) download link to a non-existent blob.
      //it seems that directly linking to files needs an open session, so we only set the dataset for now
      String datasetLink = generateOpenBISLinkFromPermID("DATA_SET", datasetPermID);
      isaFile.setDatasetLink(datasetLink, transferData);
      String fileExtension = f.getName().substring(f.getName().lastIndexOf(".") + 1);
      String annotation = dataFormatAnnotationForExtension(fileExtension);
      if (annotation != null) {
        isaFile.withDataFormatAnnotations(Arrays.asList(annotation));
      }
      return Optional.of(isaFile);
    }
    return Optional.empty();
  }

  public void setDefaultStudy(String studyID) {
    this.STUDY_ID = studyID;
  }

  public void setDefaultInvestigation(String investigationID) {
    this.INVESTIGATION_ID = investigationID;
  }

}
