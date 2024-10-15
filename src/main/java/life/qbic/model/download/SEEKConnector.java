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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import javax.xml.parsers.ParserConfigurationException;
import life.qbic.model.AssetInformation;
import life.qbic.model.OpenbisSeekTranslator;
import life.qbic.model.SampleInformation;
import life.qbic.model.isa.SeekStructure;
import life.qbic.model.isa.GenericSeekAsset;
import life.qbic.model.isa.ISAAssay;
import life.qbic.model.isa.ISASample;
import life.qbic.model.isa.ISASampleType;
import life.qbic.model.isa.ISAStudy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

public class SEEKConnector {

  private static final Logger LOG = LogManager.getLogger(SEEKConnector.class);
  private String apiURL;
  private byte[] credentials;
  private OpenbisSeekTranslator translator;
  private final String DEFAULT_PROJECT_ID;
  private final List<String> ASSET_TYPES = new ArrayList<>(Arrays.asList("data_files", "models",
      "sops", "documents", "publications"));

  public SEEKConnector(String seekURL, byte[] httpCredentials, String openBISBaseURL,
      String defaultProjectTitle) throws URISyntaxException, IOException,
      InterruptedException, ParserConfigurationException, SAXException {
    this.apiURL = seekURL;
    this.credentials = httpCredentials;
    Optional<String> projectID = getProjectWithTitle(defaultProjectTitle);
    if (projectID.isEmpty()) {
      throw new RuntimeException("Failed to find project with title: " + defaultProjectTitle + ". "
          + "Please provide an existing default project.");
    }
    DEFAULT_PROJECT_ID = projectID.get();
    translator = new OpenbisSeekTranslator(openBISBaseURL, DEFAULT_PROJECT_ID);
  }

  public void setDefaultInvestigation(String investigationTitle)
      throws URISyntaxException, IOException, InterruptedException {
    translator.setDefaultInvestigation(searchNodeWithTitle("investigations",
        investigationTitle));
  }

  public void setDefaultStudy(String studyTitle)
      throws URISyntaxException, IOException, InterruptedException {
    translator.setDefaultStudy(searchNodeWithTitle("studies", studyTitle));
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

  public String addStudy(ISAStudy assay)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/studies";

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

  public String addAssay(ISAAssay assay)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/assays";

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPOSTRequest(endpoint, assay.toJson()),
            BodyHandlers.ofString());

    System.err.println(assay.toJson());

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

  private HttpRequest buildAuthorizedPATCHRequest(String endpoint, String body) throws URISyntaxException {
    return HttpRequest.newBuilder()
        .uri(new URI(endpoint))
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
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
-patient id should be linked somehow, maybe gender?
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

  public String createSampleType(ISASampleType sampleType)
      throws URISyntaxException, IOException, InterruptedException {
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

  public String updateSample(ISASample isaSample, String sampleID) throws URISyntaxException, IOException,
      InterruptedException {
    String endpoint = apiURL+"/samples/"+sampleID;
    isaSample.setSampleID(sampleID);

    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(buildAuthorizedPATCHRequest(endpoint, isaSample.toJson()),
            BodyHandlers.ofString());

    if(response.statusCode()!=200) {
      System.err.println(response.body());
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
    JsonNode rootNode = new ObjectMapper().readTree(response.body());
    JsonNode idNode = rootNode.path("data").path("id");

    return endpoint+"/"+idNode.asText();
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

    return endpoint+"/"+idNode.asText();
  }

  private AssetToUpload createAsset(String datasetCode, GenericSeekAsset data)
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
    return new AssetToUpload(idNode.asText(), data.getFileName(), datasetCode, data.fileSizeInBytes());
  }

  public String uploadFileContent(String blobEndpoint, String file)
      throws URISyntaxException, IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(blobEndpoint))
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
    return blobEndpointToAssetURL(blobEndpoint);
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
      return blobEndpointToAssetURL(blobEndpoint);
    }
  }

  private String blobEndpointToAssetURL(String blobEndpoint) {
    return blobEndpoint.split("content_blobs")[0];
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
   * Creates
   * @param isaToOpenBISFile
   * @param assays
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  public List<AssetToUpload> createAssetsForAssays(Map<GenericSeekAsset,
      DataSetFile> isaToOpenBISFile, List<String> assays)
      throws IOException, URISyntaxException, InterruptedException {
    List<AssetToUpload> result = new ArrayList<>();
    for (GenericSeekAsset isaFile : isaToOpenBISFile.keySet()) {
      if(!assays.isEmpty()) {
        isaFile.withAssays(assays);
      }
      result.add(createAsset(isaToOpenBISFile.get(isaFile).getDataSetPermId().getPermId(),
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

  public boolean sampleTypeExists(String typeCode)
      throws URISyntaxException, IOException, InterruptedException {
    JsonNode result = genericSearch("sample_types", typeCode);
    JsonNode hits = result.path("data");
    for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
      JsonNode hit = it.next();
      if (hit.get("attributes").get("title").asText().equals(typeCode)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Performs a generic search and returns the response in JSON format
   * @param nodeType the type of SEEK node to search for
   * @param searchTerm the term to search for
   * @return JsonNode of the server's response
   */
  private JsonNode genericSearch(String nodeType, String searchTerm)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/search";
    URIBuilder builder = new URIBuilder(endpoint);
    builder.setParameter("q", searchTerm).setParameter("search_type", nodeType);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      return new ObjectMapper().readTree(response.body());
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  private String searchNodeWithTitle(String nodeType, String title)
      throws URISyntaxException, IOException, InterruptedException {
    JsonNode result = genericSearch(nodeType, title);
    JsonNode hits = result.path("data");
    for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
      JsonNode hit = it.next();
      if (hit.get("attributes").get("title").asText().equals(title)) {
        return hit.get("id").asText();
      }
    }
    throw new RuntimeException("Matching " + nodeType + " title was not found : " + title);
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

    JsonNode result = genericSearch("assays", "*"+searchTerm+"*");

    JsonNode hits = result.path("data");
    List<String> assayIDs = new ArrayList<>();
    for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
      JsonNode hit = it.next();
      assayIDs.add(hit.get("id").asText());
    }
    return assayIDs;
  }

  /**
   * Searches for samples containing a search term and returns a list of found sample ids
   * @param searchTerm the search term that should be in the assay properties - e.g. an openBIS id
   * @return
   * @throws URISyntaxException
   * @throws IOException
   * @throws InterruptedException
   */
  public List<String> searchSamplesContainingKeyword(String searchTerm)
      throws URISyntaxException, IOException, InterruptedException {

    JsonNode result = genericSearch("samples", "*"+searchTerm+"*");

    JsonNode hits = result.path("data");
    List<String> assayIDs = new ArrayList<>();
    for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
      JsonNode hit = it.next();
      assayIDs.add(hit.get("id").asText());
    }
    return assayIDs;
  }


  public List<String> searchAssetsContainingKeyword(String searchTerm)
      throws URISyntaxException, IOException, InterruptedException {
    List<String> assetIDs = new ArrayList<>();
    for(String type : ASSET_TYPES) {
      JsonNode result = genericSearch(type, "*"+searchTerm+"*");

      JsonNode hits = result.path("data");
      for (Iterator<JsonNode> it = hits.elements(); it.hasNext(); ) {
        JsonNode hit = it.next();
        assetIDs.add(hit.get("id").asText());
      }
    }
    return assetIDs;
  }


  /**
   * Updates information of an existing assay, its samples and attached assets. Missing samples and
   * assets are created, but nothing missing from the new structure is deleted from SEEK.
   * @param nodeWithChildren the translated Seek structure as it should be once the update is done
   * @param assayID the assay id of the existing assay, that should be compared to the new structure
   * @return information necessary to make post registration updates in openBIS and upload missing
   * data to newly created assets. In the case of the update use case, only newly created objects
   * will be contained in the return object.
   */
  public SeekStructurePostRegistrationInformation updateAssayNode(SeekStructure nodeWithChildren,
      String assayID) throws URISyntaxException, IOException, InterruptedException {
    JsonNode assayData = fetchAssayData(assayID).get("data");
    Map<String, SampleInformation> sampleInfos = collectSampleInformation(assayData);

    // compare samples
    Map<ISASample, String> newSamplesWithReferences = nodeWithChildren.getSamplesWithOpenBISReference();

    List<ISASample> samplesToCreate = new ArrayList<>();
    for (ISASample newSample : newSamplesWithReferences.keySet()) {
      String openBisID = newSamplesWithReferences.get(newSample);
      SampleInformation existingSample = sampleInfos.get(openBisID);
      if (existingSample == null) {
        samplesToCreate.add(newSample);
        System.out.printf("%s not found in SEEK. It will be created.%n", openBisID);
      } else {
        Map<String, Object> newAttributes = newSample.fetchCopyOfAttributeMap();
        for (String key : newAttributes.keySet()) {
          Object newValue = newAttributes.get(key);
          Object oldValue = existingSample.getAttributes().get(key);

          boolean oldEmpty = oldValue == null || oldValue.toString().isEmpty();
          boolean newEmpty = newValue == null || newValue.toString().isEmpty();
          if ((!oldEmpty && !newEmpty) && !newValue.equals(oldValue)) {
            System.out.printf("Mismatch found in attributes of %s. Sample will be updated.%n",
                openBisID);
            newSample.setAssayIDs(List.of(assayID));
            updateSample(newSample, existingSample.getSeekID());
          }
        }
      }
    }

    // compare assets
    Map<String, AssetInformation> assetInfos = collectAssetInformation(assayData);
    Map<GenericSeekAsset, DataSetFile> newAssetsToFiles = nodeWithChildren.getISAFileToDatasetFiles();

    List<GenericSeekAsset> assetsToCreate = new ArrayList<>();
    for (GenericSeekAsset newAsset : newAssetsToFiles.keySet()) {
      DataSetFile file = newAssetsToFiles.get(newAsset);
      String newPermId = file.getDataSetPermId().getPermId();
      if (!assetInfos.containsKey(newPermId)) {
        assetsToCreate.add(newAsset);
        System.out.printf("Assets with Dataset PermId %s not found in SEEK. File %s from this "
            + "Dataset will be created.%n", newPermId, newAsset.getFileName());
      }
    }
    Map<String, String> sampleIDsWithEndpoints = new HashMap<>();
    for (ISASample sample : samplesToCreate) {
      sample.setAssayIDs(Collections.singletonList(assayID));
      String sampleEndpoint = createSample(sample);
      sampleIDsWithEndpoints.put(newSamplesWithReferences.get(sample), sampleEndpoint);
    }
    List<AssetToUpload> assetsToUpload = new ArrayList<>();
    for (GenericSeekAsset asset : assetsToCreate) {
      asset.withAssays(Collections.singletonList(assayID));
      assetsToUpload.add(createAsset(newAssetsToFiles.get(asset).getDataSetPermId().getPermId(),
          asset));
    }
    Map<String, Set<String>> datasetIDsWithEndpoints = new HashMap<>();

    for (AssetToUpload asset : assetsToUpload) {
      String endpointWithoutBlob = blobEndpointToAssetURL(asset.getBlobEndpoint());
      String dsCode = asset.getDataSetCode();
      if (datasetIDsWithEndpoints.containsKey(dsCode)) {
        datasetIDsWithEndpoints.get(dsCode).add(endpointWithoutBlob);
      } else {
        datasetIDsWithEndpoints.put(dsCode, new HashSet<>(
            List.of(endpointWithoutBlob)));
      }
    }

    String assayEndpoint = apiURL + "/assays/" + assayID;
    if(nodeWithChildren.getAssayWithOpenBISReference().isEmpty()) {
        throw new RuntimeException("No assay and openBIS reference found. Object has not been "
            + "initialized using an assay object and openBIS experiment reference.");
    }
    String expID = nodeWithChildren.getAssayWithOpenBISReference().get().getRight();
    Pair<String, String> experimentIDWithEndpoint = new ImmutablePair<>(expID, assayEndpoint);

    SeekStructurePostRegistrationInformation postRegInfo =
        new SeekStructurePostRegistrationInformation(assetsToUpload, sampleIDsWithEndpoints,
            datasetIDsWithEndpoints);
    postRegInfo.setExperimentIDWithEndpoint(experimentIDWithEndpoint);
        return postRegInfo;
  }

  private Map<String, AssetInformation> collectAssetInformation(JsonNode assayData)
      throws URISyntaxException, IOException, InterruptedException {
    Map<String, AssetInformation> assets = new HashMap<>();
    JsonNode relationships = assayData.get("relationships");
    for(String type : ASSET_TYPES) {
      for (Iterator<JsonNode> it = relationships.get(type).get("data").elements(); it.hasNext(); ) {
          String assetID = it.next().get("id").asText();
          AssetInformation assetInfo = fetchAssetInformation(assetID, type);
          if(assetInfo.getOpenbisPermId()!=null) {
            assets.put(assetInfo.getOpenbisPermId(), assetInfo);
          } else {
            System.out.printf("No Dataset permID found for existing %s %s (id: %s)%n"
                    + "This asset will be treated as if it would not exist in the update.%n",
                type, assetInfo.getTitle(), assetID);
          }
      }
    }
    return assets;
  }

  private Map<String, SampleInformation> collectSampleInformation(JsonNode assayData)
      throws URISyntaxException, IOException, InterruptedException {
    Map<String, SampleInformation> samples = new HashMap<>();
    JsonNode relationships = assayData.get("relationships");
      for (Iterator<JsonNode> it = relationships.get("samples").get("data").elements(); it.hasNext(); ) {
        String sampleID = it.next().get("id").asText();
        SampleInformation info = fetchSampleInformation(sampleID);
        samples.put(info.getOpenBisIdentifier(), info);
      }
    return samples;
  }

  private AssetInformation fetchAssetInformation(String assetID, String assetType)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/"+assetType+"/"+assetID;
    URIBuilder builder = new URIBuilder(endpoint);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      JsonNode attributes = new ObjectMapper().readTree(response.body()).get("data").get("attributes");
      String title = attributes.get("title").asText();
      String description = attributes.get("description").asText();
      AssetInformation result = new AssetInformation(assetID, assetType, title, description);
      Optional<String> permID = tryParseDatasetPermID(title);
      if(permID.isPresent()) {
        result.setOpenbisPermId(permID.get());
      } else {
        tryParseDatasetPermID(description).ifPresent(result::setOpenbisPermId);
      }
      return result;
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  private Optional<String> tryParseDatasetPermID(String input) {
    Matcher titleMatcher = OpenbisConnector.datasetCodePattern.matcher(input);
    if(titleMatcher.find()) {
      return Optional.of(titleMatcher.group());
    }
    return Optional.empty();
  }

  public SeekStructurePostRegistrationInformation updateSampleNode(SeekStructure nodeWithChildren,
      String sampleID) throws URISyntaxException, IOException, InterruptedException {
    SampleInformation existingSampleInfo = fetchSampleInformation(sampleID);
    //TODO to be able to connect samples with assets, we need to create a new assay, here

    // compare samples
    Map<ISASample, String> newSamplesWithReferences = nodeWithChildren.getSamplesWithOpenBISReference();

    List<ISASample> samplesToCreate = new ArrayList<>();
    for (ISASample newSample : newSamplesWithReferences.keySet()) {
      String openBisID = newSamplesWithReferences.get(newSample);
      if (!existingSampleInfo.getOpenBisIdentifier().equals(openBisID)) {
        samplesToCreate.add(newSample);
        System.out.printf("%s not found in SEEK. It will be created.%n", openBisID);
      } else {
        Map<String, Object> newAttributes = newSample.fetchCopyOfAttributeMap();
        for (String key : newAttributes.keySet()) {
          Object newValue = newAttributes.get(key);
          Object oldValue = existingSampleInfo.getAttributes().get(key);

          boolean oldEmpty = oldValue == null || oldValue.toString().isEmpty();
          boolean newEmpty = newValue == null || newValue.toString().isEmpty();
          if ((!oldEmpty && !newEmpty) && !newValue.equals(oldValue)) {
            System.out.printf("Mismatch found in attributes of %s. Sample will be updated.%n",
                openBisID);
            updateSample(newSample, sampleID);
          }
        }
      }
    }

    // compare assets
    Map<GenericSeekAsset, DataSetFile> newAssetsToFiles = nodeWithChildren.getISAFileToDatasetFiles();

    //TODO follow creation of assets for assay, no way to be sure these are attached to similar samples
    List<GenericSeekAsset> assetsToCreate = new ArrayList<>();

    Map<String, String> sampleIDsWithEndpoints = new HashMap<>();
    for (ISASample sample : samplesToCreate) {
      String sampleEndpoint = createSample(sample);
      sampleIDsWithEndpoints.put(newSamplesWithReferences.get(sample), sampleEndpoint);
    }
    List<AssetToUpload> assetsToUpload = new ArrayList<>();

    for (GenericSeekAsset asset : assetsToCreate) {
      assetsToUpload.add(createAsset(newAssetsToFiles.get(asset).getDataSetPermId().getPermId(),
          asset));
    }
    Map<String, Set<String>> datasetIDsWithEndpoints = new HashMap<>();

    for (AssetToUpload asset : assetsToUpload) {
      String endpointWithoutBlob = blobEndpointToAssetURL(asset.getBlobEndpoint());
      String dsCode = asset.getDataSetCode();
      if (datasetIDsWithEndpoints.containsKey(dsCode)) {
        datasetIDsWithEndpoints.get(dsCode).add(endpointWithoutBlob);
      } else {
        datasetIDsWithEndpoints.put(dsCode, new HashSet<>(
            List.of(endpointWithoutBlob)));
      }
    }

    return new SeekStructurePostRegistrationInformation(assetsToUpload, sampleIDsWithEndpoints,
        datasetIDsWithEndpoints);
  }

  private SampleInformation fetchSampleInformation(String sampleID) throws URISyntaxException,
      IOException, InterruptedException {
    String endpoint = apiURL+"/samples/"+sampleID;
    URIBuilder builder = new URIBuilder(endpoint);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      JsonNode attributeNode = new ObjectMapper().readTree(response.body()).get("data").get("attributes");
      //title is openbis identifier - this is also added to attribute_map under the name:
      //App.configProperties.get("seek_openbis_sample_title");
      String openBisId = attributeNode.get("title").asText();
      Map<String, Object> attributesMap = new ObjectMapper()
          .convertValue(attributeNode.get("attribute_map"), Map.class);
      return new SampleInformation(sampleID, openBisId, attributesMap);
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  private JsonNode fetchAssayData(String assayID)
      throws URISyntaxException, IOException, InterruptedException {
    String endpoint = apiURL+"/assays/"+assayID;
    URIBuilder builder = new URIBuilder(endpoint);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(builder.build())
        .headers("Content-Type", "application/json")
        .headers("Accept", "application/json")
        .headers("Authorization", "Basic " + new String(credentials))
        .GET().build();
    HttpResponse<String> response = HttpClient.newBuilder().build()
        .send(request, BodyHandlers.ofString());
    if(response.statusCode() == 200) {
      return new ObjectMapper().readTree(response.body());
    } else {
      throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
    }
  }

  public SeekStructurePostRegistrationInformation createNode(SeekStructure nodeWithChildren)
      throws URISyntaxException, IOException, InterruptedException {

    if(nodeWithChildren.getAssayWithOpenBISReference().isEmpty()) {
        throw new RuntimeException("No assay and openBIS reference found. Object has not been "
            + "initialized using an assay object and openBIS experiment reference.");
    }

    Pair<ISAAssay, String> assayIDPair = nodeWithChildren.getAssayWithOpenBISReference().get();

    String assayID = addAssay(assayIDPair.getKey());
    String assayEndpoint = apiURL+"/assays/"+assayID;
    Pair<String, String> experimentIDWithEndpoint =
        new ImmutablePair<>(assayIDPair.getValue(), assayEndpoint);

    //wait for a bit, so we can be sure the assay that will be referenced by the samples has been created
    Thread.sleep(3000);

    Map<String, String> sampleIDsWithEndpoints = new HashMap<>();
    Map<ISASample, String> samplesWithReferences = nodeWithChildren.getSamplesWithOpenBISReference();
    for(ISASample sample : samplesWithReferences.keySet()) {
      sample.setAssayIDs(Collections.singletonList(assayID));
      String sampleEndpoint = createSample(sample);
      sampleIDsWithEndpoints.put(samplesWithReferences.get(sample), sampleEndpoint);
    }

    Map<GenericSeekAsset, DataSetFile> isaToFileMap = nodeWithChildren.getISAFileToDatasetFiles();

    List<AssetToUpload> assetsToUpload = createAssetsForAssays(isaToFileMap,
        Collections.singletonList(assayID));

    Map<String, Set<String>> datasetIDsWithEndpoints = new HashMap<>();

    for(AssetToUpload asset : assetsToUpload) {
      String endpointWithoutBlob = blobEndpointToAssetURL(asset.getBlobEndpoint());
      String dsCode = asset.getDataSetCode();
      if(datasetIDsWithEndpoints.containsKey(dsCode)) {
        datasetIDsWithEndpoints.get(dsCode).add(endpointWithoutBlob);
      } else {
        datasetIDsWithEndpoints.put(dsCode, new HashSet<>(
            List.of(endpointWithoutBlob)));
      }
    }
    SeekStructurePostRegistrationInformation postRegInfo =
        new SeekStructurePostRegistrationInformation(assetsToUpload, sampleIDsWithEndpoints,
            datasetIDsWithEndpoints);
    postRegInfo.setExperimentIDWithEndpoint(experimentIDWithEndpoint);
    return postRegInfo;
  }

  public SeekStructurePostRegistrationInformation createSampleWithAssets(SeekStructure nodeWithChildren)
      throws URISyntaxException, IOException, InterruptedException {
   Map<String, String> sampleIDsWithEndpoints = new HashMap<>();
    Map<ISASample, String> samplesWithReferences = nodeWithChildren.getSamplesWithOpenBISReference();
    for(ISASample sample : samplesWithReferences.keySet()) {
      String sampleEndpoint = createSample(sample);
      sampleIDsWithEndpoints.put(samplesWithReferences.get(sample), sampleEndpoint);
    }

    Map<GenericSeekAsset, DataSetFile> isaToFileMap = nodeWithChildren.getISAFileToDatasetFiles();

    List<AssetToUpload> assetsToUpload = createAssetsForAssays(isaToFileMap, new ArrayList<>());

    Map<String, Set<String>> datasetIDsWithEndpoints = new HashMap<>();

    for(AssetToUpload asset : assetsToUpload) {
      String endpointWithoutBlob = blobEndpointToAssetURL(asset.getBlobEndpoint());
      String dsCode = asset.getDataSetCode();
      if(datasetIDsWithEndpoints.containsKey(dsCode)) {
        datasetIDsWithEndpoints.get(dsCode).add(endpointWithoutBlob);
      } else {
        datasetIDsWithEndpoints.put(dsCode, new HashSet<>(
            List.of(endpointWithoutBlob)));
      }
    }
    return new SeekStructurePostRegistrationInformation(assetsToUpload, sampleIDsWithEndpoints,
        datasetIDsWithEndpoints);
  }

  public SeekStructurePostRegistrationInformation createStandaloneAssets(
      Map<GenericSeekAsset, DataSetFile> isaToFileMap)
      throws IOException, URISyntaxException, InterruptedException {

    List<AssetToUpload> assetsToUpload = createAssetsForAssays(isaToFileMap, new ArrayList<>());

    Map<String, Set<String>> datasetIDsWithEndpoints = new HashMap<>();

    for(AssetToUpload asset : assetsToUpload) {
      String endpointWithoutBlob = blobEndpointToAssetURL(asset.getBlobEndpoint());
      String dsCode = asset.getDataSetCode();
      if(datasetIDsWithEndpoints.containsKey(dsCode)) {
        datasetIDsWithEndpoints.get(dsCode).add(endpointWithoutBlob);
      } else {
        datasetIDsWithEndpoints.put(dsCode, new HashSet<>(
            List.of(endpointWithoutBlob)));
      }
    }
    return new SeekStructurePostRegistrationInformation(assetsToUpload, datasetIDsWithEndpoints);
  }

  public OpenbisSeekTranslator getTranslator() {
    return translator;
  }

  public static class AssetToUpload {

    private final String blobEndpoint;
    private final String filePath;
    private final String openBISDataSetCode;
    private final long fileSizeInBytes;

    public AssetToUpload(String blobEndpoint, String filePath, String openBISDataSetCode,
        long fileSizeInBytes) {
      this.blobEndpoint = blobEndpoint;
      this.filePath = filePath;
      this.openBISDataSetCode = openBISDataSetCode;
      this.fileSizeInBytes = fileSizeInBytes;
    }

    public long getFileSizeInBytes() {
      return fileSizeInBytes;
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

  public class SeekStructurePostRegistrationInformation {

    private final List<AssetToUpload> assetsToUpload;
    private Optional<Pair<String, String>> experimentIDWithEndpoint;
    private final Map<String, String> sampleIDsWithEndpoints;
    private final Map<String, Set<String>> datasetIDsWithEndpoints;

    public SeekStructurePostRegistrationInformation(List<AssetToUpload> assetsToUpload,
        Map<String, String> sampleIDsWithEndpoints,
        Map<String, Set<String>> datasetIDsWithEndpoints) {
      this.assetsToUpload = assetsToUpload;
      this.sampleIDsWithEndpoints = sampleIDsWithEndpoints;
      this.datasetIDsWithEndpoints = datasetIDsWithEndpoints;
      this.experimentIDWithEndpoint = Optional.empty();
    }

    public SeekStructurePostRegistrationInformation(List<AssetToUpload> assetsToUpload,
        Map<String, Set<String>> datasetIDsWithEndpoints) {
      this.sampleIDsWithEndpoints = new HashMap<>();
      this.datasetIDsWithEndpoints = datasetIDsWithEndpoints;
      this.assetsToUpload = assetsToUpload;
      this.experimentIDWithEndpoint = Optional.empty();
    }

    public void setExperimentIDWithEndpoint(Pair<String, String> experimentIDWithEndpoint) {
      this.experimentIDWithEndpoint = Optional.of(experimentIDWithEndpoint);
    }

    public List<AssetToUpload> getAssetsToUpload() {
      return assetsToUpload;
    }

    public Optional<Pair<String, String>> getExperimentIDWithEndpoint() {
      return experimentIDWithEndpoint;
    }

    public Map<String, String> getSampleIDsWithEndpoints() {
      return sampleIDsWithEndpoints;
    }

    public Map<String, Set<String>> getDatasetIDsWithEndpoints() {
      return datasetIDsWithEndpoints;
    }

  }
}
