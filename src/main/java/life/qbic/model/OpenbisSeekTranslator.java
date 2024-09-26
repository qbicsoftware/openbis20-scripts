package life.qbic.model;

import static java.util.Map.entry;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.model.isa.ISADataFile;
import life.qbic.model.isa.ISASample;

public class OpenbisSeekTranslator {

  private final String DEFAULT_PROJECT_ID;
  private final String DEFAULT_STUDY_ID;

  public OpenbisSeekTranslator(String defaultProjectID, String defaultStudyID) {
    this.DEFAULT_PROJECT_ID = defaultProjectID;
    this.DEFAULT_STUDY_ID = defaultStudyID;
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

  Map<String, String> experimentTypeToAssayType = Map.ofEntries(
      entry("00_MOUSE_DATABASE", ""),
      entry("00_PATIENT_DATABASE", ""),//if this is related to measured data, attach it as sample to the assay
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
      entry("ANALYSIS_NOTEBOOK", "Document"),
      entry("ANALYZED_DATA", "Data_file"),
      entry("ATTACHMENT", "Document"),
      entry("ELN_PREVIEW", ""),
      entry("EXPERIMENT_PROTOCOL", "SOP"),
      entry("EXPERIMENT_RESULT", "Document"),
      entry("HISTOLOGICAL_SLIDE", "Data_file"),
      entry("IB_DATA", "Data_file"),
      entry("LUMINEX_DATA", "Data_file"),
      entry("MS_DATA_ANALYZED", "Data_file"),
      entry("MS_DATA_RAW", "Data_file"),
      entry("OTHER_DATA", "Document"),
      entry("PROCESSED_DATA", "Data_file"),
      entry("PUBLICATION_DATA", "Publication"),
      entry("QPCR_DATA", "Data_file"),
      entry("RAW_DATA", "Data_file"),
      entry("SOURCE_CODE", "Document"),
      entry("TEST_CONT", ""),
      entry("TEST_DAT", ""),
      entry("UNKNOWN", "")
  );

  public SeekStructure translate(String seekNode, Experiment experimentWithSamplesAndDatasets) {
    Experiment exp = experimentWithSamplesAndDatasets;
    exp.getType();
    return null;
    //new ISAAssay(exp.getCode(), )
  }

  public ISADataFile translate(String seekNode, DataSet dataset) {
    return null; //new ISADataFile();
  }

  public String assetForDatasetType(String datasetType) {
    return "Data_file";//TODO
  }

  public String dataFormatAnnotationForExtension(String fileExtension) {
    return fileExtensionToDataFormat.get(fileExtension);
  }

  public SeekStructure translate(Experiment experiment, List<String> blacklist) {
    System.err.println(experiment.getCode());

    for(Sample sample : experiment.getSamples()) {
      String sampleType = getSampleType(sample.getType());
      Map<String, Object> attributes = new HashMap<>();
      ISASample isaSample = new ISASample(attributes, sampleType,
          Collections.singletonList(DEFAULT_PROJECT_ID));
      System.err.println(sample.getCode());
    }
    for(DataSet dataset : experiment.getDataSets()) {
      System.err.println(dataset.getCode());
    }
    System.err.println("blacklisted:");
    for(String dataset : blacklist) {
      System.err.println(dataset);
    }
    return null;//TODO
  }

  private String getSampleType(SampleType type) {
  }
}
