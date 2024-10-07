package life.qbic.model;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.util.List;
import java.util.Map;

public class OpenbisExperimentWithDescendants {

  private Experiment experiment;
  private List<Sample> samples;
  private List<DatasetWithProperties> datasets;
  private Map<String, List<DataSetFile>> datasetCodeToFiles;

  public OpenbisExperimentWithDescendants(Experiment experiment, List<Sample> samples,
      List<DatasetWithProperties> datasets, Map<String, List<DataSetFile>> datasetCodeToFiles) {
    this.experiment = experiment;
    this.samples = samples;
    this.datasets = datasets;
    this.datasetCodeToFiles = datasetCodeToFiles;
  }

  public Experiment getExperiment() {
    return experiment;
  }

  public List<Sample> getSamples() {
    return samples;
  }

  public List<DatasetWithProperties> getDatasets() {
    return datasets;
  }

  public List<DataSetFile> getFilesForDataset(String permID) {
    return datasetCodeToFiles.get(permID);
  }
}
