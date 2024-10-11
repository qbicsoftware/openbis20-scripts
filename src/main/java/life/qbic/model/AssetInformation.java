package life.qbic.model;

public class AssetInformation {

  private String seekID;
  private String title;
  private String description;
  private String assetType;
  private String openbisPermId;

  public AssetInformation(String assetID, String assetType, String title, String description) {
    this.seekID = assetID;
    this.title = title;
    this.description = description;
    this.assetType = assetType;
  }

  public String getAssetType() {
    return assetType;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getSeekID() {
    return seekID;
  }

  public void setOpenbisPermId(String id) {
    this.openbisPermId = id;
  }

  public String getOpenbisPermId() {
    return openbisPermId;
  }
}
