package life.qbic.model;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.util.List;
import java.util.Map;
import life.qbic.model.isa.ISAAssay;
import life.qbic.model.isa.ISADataFile;
import life.qbic.model.isa.ISASample;

public class SeekStructure {

  private ISAAssay assay;
  private List<ISASample> samples;
  private List<ISADataFile> dataFiles;
  private Map<ISADataFile, DataSetFile> isaToOpenBISFile;
  private Map<ISADataFile, String> isaFileToAssetType;
  //ISASample isaSample = new ISASample(map, sampleTypeID, Arrays.asList(projectID));

  public ISAAssay getAssay() {
    return assay;
  }

  public List<ISASample> getSamples() {
    return samples;
  }

  public Map<ISADataFile, DataSetFile> getIsaToOpenBISFiles() {
    return isaToOpenBISFile;
  }

  public String getAssetType(ISADataFile dataFile) {
    return isaFileToAssetType.get(dataFile);
  }
}
