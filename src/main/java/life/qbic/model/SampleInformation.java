package life.qbic.model;

import java.util.Map;

public class SampleInformation {

  private String seekID;
  private String openBisIdentifier;
  private Map<String, Object> attributes;

  public SampleInformation(String sampleID, String title, Map<String, Object> attributesMap) {
    this.seekID = sampleID;
    this.openBisIdentifier = title;
    this.attributes = attributesMap;
  }

  public String getSeekID() {
    return seekID;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public String getOpenBisIdentifier() {
    return openBisIdentifier;
  }
}
