package life.qbic.model;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import java.util.Set;

public class SampleTypesAndMaterials {

  Set<SampleType> sampleTypes;
  Set<SampleType> sampleTypesAsMaterials;

  public SampleTypesAndMaterials(Set<SampleType> sampleTypes, Set<SampleType> sampleTypesAsMaterials) {
    this.sampleTypes = sampleTypes;
    this.sampleTypesAsMaterials = sampleTypesAsMaterials;
  }

  public Set<SampleType> getSamplesAsMaterials() {
    return sampleTypesAsMaterials;
  }

  public Set<SampleType> getSampleTypes() {
    return sampleTypes;
  }
}
