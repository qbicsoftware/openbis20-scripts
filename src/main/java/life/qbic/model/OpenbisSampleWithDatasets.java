package life.qbic.model;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.util.List;
import java.util.Map;

public class OpenbisSampleWithDatasets {

  private Sample sample;
  private List<DatasetWithProperties> datasets;
  private Map<String, List<DataSetFile>> datasetCodeToFiles;

  public OpenbisSampleWithDatasets(Sample sample,
      List<DatasetWithProperties> datasets, Map<String, List<DataSetFile>> datasetCodeToFiles) {
    this.sample = sample;
    this.datasets = datasets;
    this.datasetCodeToFiles = datasetCodeToFiles;
  }

  public Sample getSample() {
    return sample;
  }

  public List<DatasetWithProperties> getDatasets() {
    return datasets;
  }

  public List<DataSetFile> getFilesForDataset(String permID) {
    return datasetCodeToFiles.get(permID);
  }
}
