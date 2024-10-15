package life.qbic.model.isa;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Stores newly created ISA objects for SEEK, as well as their respective openBIS reference. It is
 * assumed that these references are Sample and Experiment Identifiers. PermIds of datasets are taken
 * from stored DataSetFiles
 */
public class SeekStructure {

  private final Optional<Pair<ISAAssay, String>> assayAndOpenBISReference;
  private final Map<ISASample, String> samplesWithOpenBISReference;
  private final Map<GenericSeekAsset, DataSetFile> isaToOpenBISFile;

  public SeekStructure(ISAAssay assay, String openBISReference) {
    this.assayAndOpenBISReference = Optional.of(new ImmutablePair<>(assay, openBISReference));
    this.samplesWithOpenBISReference = new HashMap<>();
    this.isaToOpenBISFile = new HashMap<>();
  }

  public SeekStructure(ISASample isaSample, String sampleID) {
    this.samplesWithOpenBISReference = new HashMap<>();
    this.isaToOpenBISFile = new HashMap<>();
    this.assayAndOpenBISReference = Optional.empty();
  }

  public SeekStructure(Map<GenericSeekAsset, DataSetFile> isaToOpenBISFile) {
    this.isaToOpenBISFile = isaToOpenBISFile;
    this.samplesWithOpenBISReference = new HashMap<>();
    this.assayAndOpenBISReference = Optional.empty();
  }

  public void addSample(ISASample sample, String openBISReference) {
    samplesWithOpenBISReference.put(sample, openBISReference);
  }

  public void addAsset(GenericSeekAsset asset, DataSetFile file) {
    isaToOpenBISFile.put(asset, file);
  }

  public Optional<Pair<ISAAssay, String>> getAssayWithOpenBISReference() {
    return assayAndOpenBISReference;
  }

  public Map<ISASample, String> getSamplesWithOpenBISReference() {
    return samplesWithOpenBISReference;
  }

  public Map<GenericSeekAsset, DataSetFile> getISAFileToDatasetFiles() {
    return isaToOpenBISFile;
  }

}
