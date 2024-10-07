package life.qbic.model.isa;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.util.List;
import java.util.Map;

public class SeekStructure {

  private ISAAssay assay;
  private List<ISASample> samples;
  private Map<GenericSeekAsset, DataSetFile> isaToOpenBISFile;

  public SeekStructure(ISAAssay assay, List<ISASample> samples,
      Map<GenericSeekAsset, DataSetFile> isaToOpenBISFile) {
    this.assay = assay;
    this.samples = samples;
    this.isaToOpenBISFile = isaToOpenBISFile;
  }

  public ISAAssay getAssay() {
    return assay;
  }

  public List<ISASample> getSamples() {
    return samples;
  }

  public Map<GenericSeekAsset, DataSetFile> getISAFileToDatasetFiles() {
    return isaToOpenBISFile;
  }

}
