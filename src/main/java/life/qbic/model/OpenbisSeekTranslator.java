package life.qbic.model;

import static java.util.Map.entry;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import life.qbic.model.isa.GenericSeekAsset;
import life.qbic.model.isa.ISAAssay;
import life.qbic.model.isa.ISASample;
import life.qbic.model.isa.ISASampleType;
import life.qbic.model.isa.ISASampleType.SampleAttribute;
import life.qbic.model.isa.ISASampleType.SampleAttributeType;
import life.qbic.model.isa.SeekStructure;

public class OpenbisSeekTranslator {

  private final String DEFAULT_PROJECT_ID;
  private final String DEFAULT_STUDY_ID;
  private final String DEFAULT_TRANSFERRED_SAMPLE_TITLE = "openBIS Name";
  private final String openBISBaseURL;

  public OpenbisSeekTranslator(String openBISBaseURL, String defaultProjectID, String defaultStudyID) {
    this.openBISBaseURL = openBISBaseURL;
    this.DEFAULT_PROJECT_ID = defaultProjectID;
    this.DEFAULT_STUDY_ID = defaultStudyID;
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

  Map<String, String> experimentTypeToAssayClass = Map.ofEntries(
      entry("00_MOUSE_DATABASE", "EXP"),
      entry("00_PATIENT_DATABASE", "EXP"),
      entry("00_STANDARD_OPERATING_PROTOCOLS", "EXP"),
      entry("01_BIOLOGICAL_EXPERIMENT", "EXP"),
      entry("02_MASSSPECTROMETRY_EXPERIMENT", "EXP"),//where is petab modeling attached?
      entry("03_HISTOLOGICAL_ANALYSIS", "EXP"),
      entry("04_MICRO_CT", "EXP")
  );

  Map<DataType, SampleAttributeType> dataTypeToAttributeType = Map.ofEntries(
      entry(DataType.INTEGER, new SampleAttributeType("4", "Integer", "Integer")),
      entry(DataType.VARCHAR, new SampleAttributeType("8", "String", "String")),
      entry(DataType.MULTILINE_VARCHAR, new SampleAttributeType("7", "Text", "Text")),
      entry(DataType.REAL, new SampleAttributeType("3", "Real number", "Float")),
      entry(DataType.TIMESTAMP, new SampleAttributeType("1", "Date time", "DateTime")),
      entry(DataType.BOOLEAN, new SampleAttributeType("16", "Boolean", "Boolean")),
      entry(DataType.CONTROLLEDVOCABULARY, //we use String for now
          new SampleAttributeType("8", "String", "String")),
      entry(DataType.MATERIAL, //not used anymore in this form
          new SampleAttributeType("8", "String", "String")),
      entry(DataType.HYPERLINK, new SampleAttributeType("8", "String", "String")),
      entry(DataType.XML, new SampleAttributeType("7", "Text", "Text")),
      entry(DataType.SAMPLE, //we link the sample as URL to openBIS for now
          new SampleAttributeType("5", "Web link", "String")),
      entry(DataType.DATE, new SampleAttributeType("2", "Date time", "Date"))
  );

  Map<String, String> experimentTypeToAssayType = Map.ofEntries(
      entry("00_MOUSE_DATABASE", ""),
      entry("00_PATIENT_DATABASE", ""),
      entry("00_STANDARD_OPERATING_PROTOCOLS", ""),
      entry("01_BIOLOGICAL_EXPERIMENT", "http://jermontology.org/ontology/JERMOntology#Cultivation_experiment"),
      entry("02_MASSSPECTROMETRY_EXPERIMENT", "http://jermontology.org/ontology/JERMOntology#Proteomics"),
      entry("03_HISTOLOGICAL_ANALYSIS", ""),
      entry("04_MICRO_CT", "")
  );

  Map<String, String> fileExtensionToDataFormat = Map.ofEntries(
      entry("fastq.gz", "http://edamontology.org/format_1930"),
      entry("fastq", "http://edamontology.org/format_1930"),
      entry("json", "http://edamontology.org/format_3464"),
      entry("yaml", "http://edamontology.org/format_3750"),
      entry("raw", "http://edamontology.org/format_3712"),
      entry("tsv", "http://edamontology.org/format_3475"),
      entry("csv", "http://edamontology.org/format_3752")
      );

  Map<String, String> datasetTypeToAssetType = Map.ofEntries(
      entry("ANALYSIS_NOTEBOOK", "documents"),
      entry("ANALYZED_DATA", "data_files"),
      entry("ATTACHMENT", "documents"),
      entry("ELN_PREVIEW", ""),
      entry("EXPERIMENT_PROTOCOL", "sops"),
      entry("EXPERIMENT_RESULT", "documents"),
      entry("HISTOLOGICAL_SLIDE", "data_files"),
      entry("IB_DATA", "data_files"),
      entry("LUMINEX_DATA", "data_files"),
      entry("MS_DATA_ANALYZED", "data_files"),
      entry("MS_DATA_RAW", "data_files"),
      entry("OTHER_DATA", "data_files"),
      entry("PROCESSED_DATA", "data_files"),
      entry("PUBLICATION_DATA", "publications"),
      entry("QPCR_DATA", "data_files"),
      entry("RAW_DATA", "data_files"),
      entry("SOURCE_CODE", "documents"),
      entry("TEST_CONT", ""),
      entry("TEST_DAT", ""),
      entry("UNKNOWN", "data_files")
  );

  public ISASampleType translate(SampleType sampleType) {
    SampleAttribute titleAttribute = new SampleAttribute(DEFAULT_TRANSFERRED_SAMPLE_TITLE,
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
      Map<String, String> sampleTypesToIds, Set<String> blacklist) throws URISyntaxException {

    Experiment exp = experiment.getExperiment();
    String expType = exp.getType().getCode();
    String title = exp.getCode()+" ("+exp.getPermId().getPermId()+")";
    ISAAssay assay = new ISAAssay(title, DEFAULT_STUDY_ID, experimentTypeToAssayClass.get(expType),
        new URI(experimentTypeToAssayType.get(expType)));//TODO

    List<ISASample> samples = new ArrayList<>();
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

      attributes.put(DEFAULT_TRANSFERRED_SAMPLE_TITLE, sample.getIdentifier().getIdentifier());
      String sampleTypeId = sampleTypesToIds.get(sampleType.getCode());
      ISASample isaSample = new ISASample(sample.getPermId().getPermId(), attributes, sampleTypeId,
          Collections.singletonList(DEFAULT_PROJECT_ID));
      samples.add(isaSample);
    }
    Map<GenericSeekAsset, DataSetFile> isaToOpenBISFile = new HashMap<>();

    //create ISA files for assets. If actual data is uploaded is determined later based upon flag
    for(DatasetWithProperties dataset : experiment.getDatasets()) {
      String permID = dataset.getCode();
      if(!blacklist.contains(permID)) {
        for(DataSetFile file : experiment.getFilesForDataset(permID)) {
          String datasetType = getDatasetTypeOfFile(file, experiment.getDatasets());
          datasetFileToSeekAsset(file, datasetType)
              .ifPresent(seekAsset -> isaToOpenBISFile.put(seekAsset, file));
        }
      }
    }
    return new SeekStructure(assay, samples, isaToOpenBISFile);
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
  private Optional<GenericSeekAsset> datasetFileToSeekAsset(DataSetFile file, String datasetType) {
    if (!file.getPath().isBlank() && !file.isDirectory()) {
      File f = new File(file.getPath());
      String datasetCode = file.getDataSetPermId().toString();
      String assetName = datasetCode + ": " + f.getName();
      String assetType = assetForDatasetType(datasetType);
      GenericSeekAsset isaFile = new GenericSeekAsset(assetType, assetName, file.getPath(),
          Arrays.asList(DEFAULT_PROJECT_ID));
      String fileExtension = f.getName().substring(f.getName().lastIndexOf(".") + 1);
      String annotation = dataFormatAnnotationForExtension(fileExtension);
      if (annotation != null) {
        isaFile.withDataFormatAnnotations(Arrays.asList(annotation));
      }
      return Optional.of(isaFile);
    }
    return Optional.empty();
  }

}
