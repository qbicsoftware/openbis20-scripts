package life.qbic.model;

import java.util.List;
import life.qbic.model.download.SEEKConnector.AssetToUpload;

public class AssayWithQueuedAssets {

  private String assayEndpoint;
  private List<AssetToUpload> assetsToUpload;

  public AssayWithQueuedAssets(String assayEndpoint, List<AssetToUpload> assetsToUpload) {
    this.assayEndpoint = assayEndpoint;
    this.assetsToUpload = assetsToUpload;
  }

  public String getAssayEndpoint() {
    return assayEndpoint;
  }

  public List<AssetToUpload> getAssets() {
    return assetsToUpload;
  }
}
