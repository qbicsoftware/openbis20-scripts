package life.qbic.model.petab;

import java.util.List;

public class PetabMetadata {

  List<String> sourceDatasetIdentifiers;

  public PetabMetadata(List<String> sourceDatasetIdentifiers) {
    this.sourceDatasetIdentifiers = sourceDatasetIdentifiers;
  }

  public List<String> getSourcePetabReferences() {
    return sourceDatasetIdentifiers;
  }
}
