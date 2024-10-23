package life.qbic.model;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for openBIS DataSets that collects additional information, e.g. from samples,
 * experiments etc. further up in the hierarchy.
 */
public class DatasetWithProperties {

  private final DataSet dataset;
  private final Map<String, String> properties;

  public DatasetWithProperties(DataSet dataset) {
    this.dataset = dataset;
    this.properties = new HashMap<>();
  }

  public void addProperty(String key, String value) {
    this.properties.put(key, value);
  }

  public String getProperty(String key) {
    return properties.get(key);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public DataSet getDataset() {
    return dataset;
  }

  public String getCode() {
    return dataset.getCode();
  }

  public Experiment getExperiment() {
    return dataset.getExperiment();
  }

  public DataSetType getType() {
    return dataset.getType();
  }

  public Person getRegistrator() {
    return dataset.getRegistrator();
  }

  public Date getRegistrationDate() {
    return dataset.getRegistrationDate();
  }

  /**
   * Returns sample ID or experiment ID, if Dataset has no sample.
   */
  public String getClosestSourceID() {
    if(dataset.getSample()!=null) {
      return dataset.getSample().getIdentifier().getIdentifier();
    } else {
      return getExperiment().getIdentifier().getIdentifier();
    }
  }
}
