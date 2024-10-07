package life.qbic.model.download;

import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import life.qbic.model.AssayWithQueuedAssets;
import life.qbic.model.OpenbisSeekTranslator;
import life.qbic.model.isa.SeekStructure;
import life.qbic.model.isa.GenericSeekAsset;
import life.qbic.model.isa.ISAAssay;
import life.qbic.model.isa.ISASample;
import life.qbic.model.isa.ISASampleType;
import life.qbic.model.isa.ISAStudy;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SEEKConnector {

  private static final Logger LOG = LogManager.getLogger(SEEKConnector.class);
  private String apiURL;
  private byte[] credentials;
  private OpenbisSeekTranslator translator;
  private final String DEFAULT_PROJECT_ID;

  public SEEKConnector(String apiURL, byte[] httpCredentials, String openBISBaseURL,
      String defaultProjectTitle, String defaultStudyTitle) throws URISyntaxException, IOException,
      InterruptedException {
    this.apiURL = apiURL;
    this.credentials = httpCredentials;
    Optional<String> projectID = getProjectWithTitle(defaultProjectTitle);
    if(projectID.isEmpty()) {
      throw new RuntimeException("Failed to find project with title: " + defaultProjectTitle+". "
          + "Please provide an existing default project.");
    }
    DEFAULT_PROJECT_ID = projectID.get();

    translator = new OpenbisSeekTranslator(openBISBaseURL, DEFAULT_PROJECT_ID,
        searchNodeWithTitle("studies", defaultStudyTitle));
  }

  /**
   * Lists projects and returns the optional identifier of the one matching the provided ID.
   * Necessary because project search does not seem to work.
   * @param projectTitle the title to search for
   * @return
   */
  private Optional<String> getProjectWithTitle(String projectTitle)
      throws IOException, InterruptedException, URISyntaxException {
    String endpoint = apiURL+"/projects/";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      JsonNode rootNode = new ObjectMapper().readTree(response.body());
      JsonNode hits = rootNode.path("data");
      for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
        JsonNode hit = it.next();
        String id = hit.get("id").asText();
        String title = hit.get("attributes").get("title").asText();
        if(title.equals(projectTitle)) {
          return Optional.of(id);
        }
      }
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
    return Optional.empty();
  }

  public String addAssay(ISAAssay assay)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/assays";

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPOSTRequest(endpoint, assay.toJson()),
            BodyHandlers.ofString());

    if(response.statusCode()!=200) {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
    JsonNode rootNode = new ObjectMapper().readTree(response.body());
    JsonNode idNode = rootNode.path("data").path("id");

    return idNode.asText();
  }

  public String createStudy(ISAStudy study)
      throws IOException, URISyntaxException, InterruptedException, IOException {
    String endpoint = apiURL+"/studies";

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPOSTRequest(endpoint, study.toJson()),
            BodyHandlers.ofString());

    if(response.statusCode()!=200) {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
    JsonNode rootNode = new ObjectMapper().readTree(response.body());
    JsonNode idNode = rootNode.path("data").path("id");

    return idNode.asText();
  }

  private HttpRequest buildAuthorizedPOSTRequest(String endpoint, String body) throws URISyntaxException {
    return HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
  }

  public boolean studyExists(String id) throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/studies/"+id;
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    return response.statusCode() == 200;
  }

  public void printAttributeTypes() throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/sample_attribute_types";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    System.err.println(response.body());
  }
  /*
  -datatype by extension
-assay equals experiment
-investigation: pre-created in SEEK?
-study: project
-patient id should be linked somehow, maybe gender?
-flexible object type to sample type?
   */

  public void deleteSampleType(String id) throws URISyntaxException, IOException,
      InterruptedException {
    String endpoint = apiURL+"/sample_types";
    URIBuilder builder = new URIBuilder(endpoint);
    builder.setParameter("id", id);

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(HttpRequest.newBuilder().uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .DELETE().build(), BodyHandlers.ofString());

    if(response.statusCode()!=201) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  public String createSampleType(ISASampleType sampleType) throws URISyntaxException, IOException,
      InterruptedException {
    String endpoint = apiURL+"/sample_types";

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPOSTRequest(endpoint, sampleType.toJson()),
            BodyHandlers.ofString());

    if(response.statusCode()!=201) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
    JsonNode rootNode = new ObjectMapper().readTree(response.body());
    JsonNode idNode = rootNode.path("data").path("id");

    return idNode.asText();
  }

  public String createSample(ISASample isaSample) throws URISyntaxException, IOException,
      InterruptedException {
    String endpoint = apiURL+"/samples";

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPOSTRequest(endpoint, isaSample.toJson()),
            BodyHandlers.ofString());

    if(response.statusCode()!=200) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
    JsonNode rootNode = new ObjectMapper().readTree(response.body());
    JsonNode idNode = rootNode.path("data").path("id");

    return idNode.asText();
  }

  private AssetToUpload createDataFileAsset(String datasetCode, GenericSeekAsset data)
      throws IOException, URISyntaxException, InterruptedException {
    String endpoint = apiURL+"/"+data.getType();

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPOSTRequest(endpoint, data.toJson()),
            BodyHandlers.ofString());

    if(response.statusCode()!=201 && response.statusCode()!=200) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }

    JsonNode rootNode = new ObjectMapper().readTree(response.body());
    JsonNode idNode = rootNode.path("data")
        .path("attributes")
        .path("content_blobs")
        .path(0).path("link");
    return new AssetToUpload(idNode.asText(), data.getFileName(), datasetCode);
  }

  @Deprecated
  private void uploadFileContent(String assetType, String assetID, String blobID, String file)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/"+assetType+"/"+assetID+"/content_blobs/"+blobID;

    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/octet-stream")
        .headers("Accept", "application/octet-stream")
        .headers("Authorization", "Basic " + new String(credentials))
        .PUT(BodyPublishers.ofFile(new File(file).toPath())).build();

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());

    if(response.statusCode()!=200) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  public String uploadStreamContent(String blobEndpoint,
      Supplier<InputStream> streamSupplier)
      throws URISyntaxException, IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(blobEndpoint))
        .headers("Content-Type", "application/octet-stream")
        .headers("Accept", "*/*")
        .headers("Authorization", "Basic " + new String(credentials))
        .PUT(BodyPublishers.ofInputStream(streamSupplier)).build();

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());

    System.err.println("response was: "+response);
    System.err.println("response body: "+response.body());

    if(response.statusCode()!=200) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    } else {
      String fileURL = blobEndpoint.split("content_blobs")[0];
      return fileURL;
    }
  }

  public boolean endPointExists(String endpoint)
      throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    return response.statusCode() == 200;
  }

  /**
   * Creates an optional asset for a file from an openBIS dataset. Folders are ignored.
   * @param file
   * @param assetType
   * @param assays
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  public Optional<AssetToUpload> createAssetForFile(DataSetFile file, String assetType,
      List<String> assays)
      throws IOException, URISyntaxException, InterruptedException {
      if(!file.getPath().isBlank() && !file.isDirectory()) {
        File f = new File(file.getPath());
        String datasetCode = file.getDataSetPermId().toString();
        String assetName = datasetCode+": "+f.getName();//TODO what do we want to call the asset?
        GenericSeekAsset isaFile = new GenericSeekAsset(assetType, assetName, file.getPath(),
            Arrays.asList(DEFAULT_PROJECT_ID));
        isaFile.withAssays(assays);
        String fileExtension = f.getName().substring(f.getName().lastIndexOf(".")+1);
        String annotation = translator.dataFormatAnnotationForExtension(fileExtension);
        if(annotation!=null) {
          isaFile.withDataFormatAnnotations(Arrays.asList(annotation));
        }
        return Optional.of(createDataFileAsset(datasetCode, isaFile));
      }
      return Optional.empty();
  }

  public List<AssetToUpload> createAssets(List<DataSetFile> filesInDataset,
      List<String> assays)
      throws IOException, URISyntaxException, InterruptedException {
    List<AssetToUpload> result = new ArrayList<>();
    for(DataSetFile file : filesInDataset) {
      if(!file.getPath().isBlank() && !file.isDirectory()) {
        File f = new File(file.getPath());
        String datasetCode = file.getDataSetPermId().toString();
        String assetName = datasetCode+": "+f.getName();//TODO what do we want to call the asset?
        GenericSeekAsset isaFile = new GenericSeekAsset("data_files", assetName, file.getPath(),
            Arrays.asList(DEFAULT_PROJECT_ID));
        isaFile.withAssays(assays);
        String fileExtension = f.getName().substring(f.getName().lastIndexOf(".")+1);
        String annotation = translator.dataFormatAnnotationForExtension(fileExtension);
        if(annotation!=null) {
          isaFile.withDataFormatAnnotations(Arrays.asList(annotation));
        }
        result.add(createDataFileAsset(datasetCode, isaFile));
      }
    }
    return result;
  }

  /**
   * Creates
   * @param isaToOpenBISFile
   * @param assays
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  public List<AssetToUpload> createAssetsForAssays(Map<GenericSeekAsset, DataSetFile> isaToOpenBISFile,
      List<String> assays)
      throws IOException, URISyntaxException, InterruptedException {
    List<AssetToUpload> result = new ArrayList<>();
    for (GenericSeekAsset isaFile : isaToOpenBISFile.keySet()) {
      isaFile.withAssays(assays);
      result.add(createDataFileAsset(isaToOpenBISFile.get(isaFile).getDataSetPermId().getPermId(),
          isaFile));
    }
    return result;
  }

  public String listAssays() throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/assays/";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      return response.body();
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  public Map<String, String> getSampleTypeNamesToIDs()
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/sample_types/";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      return parseSampleTypesJSON(response.body());
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  private Map<String, String> parseSampleTypesJSON(String json) throws JsonProcessingException {
    Map<String, String> typesToIDs = new HashMap<>();
    JsonNode rootNode = new ObjectMapper().readTree(json);
    JsonNode hits = rootNode.path("data");
    for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
      JsonNode hit = it.next();
      String id = hit.get("id").asText();
      String title = hit.get("attributes").get("title").asText();
      typesToIDs.put(title, id);
    }
    return typesToIDs;
  }

  private String searchNodeWithTitle(String nodeType, String title)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/search";
    URIBuilder builder = new URIBuilder(endpoint);
    builder.setParameter("q", title).setParameter("search_type", nodeType);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    System.err.println("searching for: "+title+" ("+nodeType+")");
    if(response.statusCode() == 200) {
      JsonNode rootNode = new ObjectMapper().readTree(response.body());
      JsonNode hits = rootNode.path("data");
      for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
        JsonNode hit = it.next();
        if(hit.get("attributes").get("title").asText().equals(title)) {
          return hit.get("id").asText();
        }
      }
      throw new RuntimeException("Matching "+nodeType+" title was not found : " + title);
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  /**
   * Searches for assays containing a search term and returns a list of found assay ids
   * @param searchTerm the search term that should be in the assay properties - e.g. an openBIS id
   * @return
   * @throws URISyntaxException
   * @throws IOException
   * @throws InterruptedException
   */
  public List<String> searchAssaysContainingKeyword(String searchTerm)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/search/";
    URIBuilder builder = new URIBuilder(endpoint);
    builder.setParameter("q", searchTerm).setParameter("type", "assays");

    HttpRequest request = HttpRequest.newBuilder()
        .uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      JsonNode rootNode = new ObjectMapper().readTree(response.body());
      JsonNode hits = rootNode.path("data");
      List<String> assayIDs = new ArrayList<>();
      for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
        JsonNode hit = it.next();
        assayIDs.add(hit.get("id").asText());
      }
      return assayIDs;
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  public String updateNode(SeekStructure nodeWithChildren, String assayID, boolean transferData) {
    //updateAssay(nodeWithChildren.getAssay());
    return apiURL+"/assays/"+assayID;
  }

  public AssayWithQueuedAssets createNode(SeekStructure nodeWithChildren, boolean transferData)
      throws URISyntaxException, IOException, InterruptedException {
    String assayID = addAssay(nodeWithChildren.getAssay());
    for(ISASample sample : nodeWithChildren.getSamples()) {
      sample.setAssayIDs(Arrays.asList(assayID));
      createSample(sample);
    }

    Map<GenericSeekAsset, DataSetFile> isaToFileMap = nodeWithChildren.getISAFileToDatasetFiles();

    return new AssayWithQueuedAssets(apiURL+"/assays/"+assayID,
        createAssetsForAssays(isaToFileMap, Arrays.asList(assayID)));
  }

  public OpenbisSeekTranslator getTranslator() {
    return translator;
  }

  public static class AssetToUpload {

    private final String blobEndpoint;
    private final String filePath;
    private final String openBISDataSetCode;

    public AssetToUpload(String blobEndpoint, String filePath, String openBISDataSetCode) {
      this.blobEndpoint = blobEndpoint;
      this.filePath = filePath;
      this.openBISDataSetCode = openBISDataSetCode;
    }

    public String getFilePath() {
      return filePath;
    }

    public String getBlobEndpoint() {
      return blobEndpoint;
    }

    public String getDataSetCode() {
      return openBISDataSetCode;
    }
  }
}
