package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.App;
import life.qbic.model.OpenbisExperimentWithDescendants;
import life.qbic.model.OpenbisSeekTranslator;
import life.qbic.model.SeekStructure;
import life.qbic.model.download.OpenbisConnector;
import life.qbic.model.download.SEEKConnector;
import life.qbic.model.download.SEEKConnector.AssetToUpload;
import org.apache.commons.codec.binary.Base64;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "openbis-to-seek",
    description =
        "Transfers metadata and (optionally) data from openBIS to SEEK. Experiments, samples and "
            + "dataset information are always transferred together (as assays, samples and one of "
            + "several data types in SEEK). Dataset info always links back to the openBIS path of "
            + "the respective dataset. The data itself will can transferred and stored in SEEK "
            + "using the '-d' flag."
            + "To completely exclude some dataset information from being transferred, a "
            + "file ('--blacklist') containing dataset codes (from openBIS) can be specified."
            + "Unless otherwise specified ('--no-update' flag), the command will try to update "
            + "existing nodes in SEEK (recognized by openBIS identifiers in their metadata).")
public class TransferDataToSeekCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "openbis id", description = "The identifier of the "
      + "experiment, sample or dataset to transfer.")
  private String objectID;
  @Option(names = "--blacklist", description = "Path to file specifying by dataset "
      + "dataset code which openBIS datasets not to transfer to SEEK. The file must contain one code "
      + "per line.")
  private String blacklistFile;
  @Option(names = "--no-update", description = "Use to specify that existing "
      + "information in SEEK for the specified openBIS input should not be updated, but new nodes "
      + "created.")
  private boolean noUpdate;
  /*@Option(names = {"-sn", "--seek-node"}, paramLabel = "seek node", description =
      "The target node in SEEK to transfer to. Must correspond to "
          + "the type of oopenBIS identifier chosen: experiment - assay; sample - sample; dataset - any of the data types. If no node is specified, "
          + "a new data structure will be created in SEEK, starting from the related experiment.")
  private String seekNode;
   */
  @Option(names = {"-d", "--data"}, description =
      "Transfers the data itself to SEEK along with the metadata. "
          + "Otherwise only the link(s) to the openBIS object will be created in SEEK.")
  private boolean transferData;
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();
  OpenbisConnector openbis;
  SEEKConnector seek;
  OpenbisSeekTranslator translator = new OpenbisSeekTranslator();

  @Override
  public void run() {
    System.out.println("auth...");

    OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(),
        auth.getAS(), auth.getDSS());
    System.out.println("openbis...");

    openbis = new OpenbisConnector(authentication);

    boolean isSample = false;
    boolean isDataSet = false;
    System.out.println("search for experiment...");

    boolean isExperiment = experimentExists(objectID);
    if (!isExperiment && sampleExists(objectID)) {
      isSample = true;
    }

    if (!isExperiment && !isSample && datasetsExist(Arrays.asList(objectID))) {
      isDataSet = true;
    }

    if (!isSample && !isExperiment && !isDataSet) {
      System.out.printf(
          "%s could not be found in openBIS. Make sure you either specify an experiment, sample or dataset%n",
          objectID);
      return;
    }
    System.out.println("Searching done...");
/*
    if (isExperiment) {
      if (seekNode != null && !seekNode.contains("assays")) {
        System.out.printf(
            "Seek node %s does not correspond to the provided openBIS experiment identifier. "
                + "Please select an assay node.%n", objectID);
        return;
      }
    }
    if (isSample) {
      if (seekNode != null && !seekNode.contains("samples")) {
        System.out.printf(
            "Seek node %s does not correspond to the provided openBIS sample identifier. "
                + "Please select a sample node.%n", objectID);
        return;
      }
    }

 */

    byte[] httpCredentials = Base64.encodeBase64(
        (auth.getSeekUser() + ":" + new String(auth.getSeekPassword())).getBytes());
    try {
      seek = new SEEKConnector(auth.getSeekURL(), httpCredentials, "Default Project",
          "lisym default study");
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    /*
    if (seekNode != null) {
      try {
        if (!seek.endPointExists(seekNode)) {
          System.out.println(seekNode + " could not be found");
          return;
        } else {
          if (isExperiment) {
            Experiment experimentWithSamplesAndDatasets = openbis.getExperimentWithDescendants(
                objectID);
            //SeekStructure seekStructure = translator.translate(seekNode, experimentWithSamplesAndDatasets);
            //seek.fillAssayWithSamplesAndDatasets(seekNode, seekStructure);
          }
        }
      } catch (URISyntaxException | InterruptedException | IOException e) {
        throw new RuntimeException(e);
      }
    }

     */
    try {
      OpenbisExperimentWithDescendants experiment = openbis.getExperimentWithDescendants(objectID);
      String assayID = getAssayIDForOpenBISExperiment(experiment.getExperiment());
      List<String> blacklist = parseBlackList(blacklistFile);
      if(assayID.isBlank()) {
        createNewNodes(experiment, blacklist);
      } else {
        updateNodes(experiment, assayID, blacklist);
      }
    } catch (URISyntaxException | InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }

      /*

      List<DataSet> datasets = openbis.findDataSets(Collections.singletonList(datasetCode));

      if(datasets.isEmpty()) {
        System.out.println(datasetCode+" not found");
        return;
      }

      DatasetWithProperties result = new DatasetWithProperties(datasets.get(0));
      Optional<String> patientID = openbis.findPropertyInSampleHierarchy("PATIENT_DKFZ_ID",
          result.getExperiment().getIdentifier());
      patientID.ifPresent(s -> result.addProperty("patientID", s));

      System.out.println("Found dataset, downloading.");
      System.out.println();

      final String tmpPath = "tmp/";

      File downloadFolder = openbis.downloadDataset(tmpPath, datasetCode);



      cleanupTemp(new File(tmpPath));
*/
    System.out.println("Done");
  }

  private List<String> parseBlackList(String blacklistFile) {
    List<String> result = new ArrayList<>();
    if(blacklistFile == null) {
      return result;
    }
    // trim whitespace, skip empty lines
    try (Stream<String> lines = Files.lines(Paths.get(blacklistFile))
        .map(String::trim)
        .filter(s -> !s.isBlank())) {
      return lines.collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(blacklistFile+" could not be found or read.");
    }
  }

  private void updateNodes(OpenbisExperimentWithDescendants experiment, String assayID, List<String> blacklist) {
    System.err.println("updating nodes of assay "+assayID);
    SeekStructure nodeWithChildren = translator.translate(experiment, blacklist);
    String updatedEndpoint = seek.updateNode(nodeWithChildren, assayID, transferData);
    System.out.printf("%s was successfully updated.%n", endpoint);
  }

  private void createNewNodes(OpenbisExperimentWithDescendants experiment, List<String> blacklist)
      throws URISyntaxException, IOException, InterruptedException {
    System.err.println("creating new nodes");
    SeekStructure nodeWithChildren = translator.translate(experiment, blacklist);
    List<AssetToUpload> assetsToUpload = seek.createNode(nodeWithChildren, transferData);
    if(transferData) {
      for(AssetToUpload asset : assetsToUpload) {
        System.out.printf("Streaming file %s from openBIS to SEEK...%n", asset.getFilePath());
        String fileURL = seek.uploadStreamContent(asset.getBlobEndpoint(),
            () -> openbis.streamDataset(asset.getDataSetCode(), asset.getFilePath()));
        System.out.printf("File stored here: %s%n", fileURL);
      }
    }
    System.out.printf("%s was successfully created.%n", endpoint);
  }

  private boolean sampleExists(String objectID) {
    return openbis.sampleExists(objectID);
  }

  private boolean datasetsExist(List<String> datasetCodes) {
    return openbis.findDataSets(datasetCodes).size() == datasetCodes.size();
  }

  private boolean experimentExists(String experimentID) {
    return openbis.experimentExists(experimentID);
  }

  private String getAssayIDForOpenBISExperiment(Experiment experiment)
      throws URISyntaxException, IOException, InterruptedException {
    // the perm id is unique and afaik not used by scientists. it is highly unlikely that it would
    // "accidentally" be part of another title or description. however, care should be taken here,
    // because if a perm id is found in the wrong SEEK node, meta-information in SEEK could be
    // overwritten or samples/data added to the wrong assay.
    String permID = experiment.getPermId().getPermId();
    List<String> assayIDs = seek.searchAssaysContainingKeyword(permID);
    if(assayIDs.isEmpty()) {
      return "";
    }
    if(assayIDs.size() == 1) {
      return assayIDs.get(0);
    }
    throw new RuntimeException("Search term "+permID+ " was found in more than one assay: "+assayIDs);
  }

  private void sendDatasetToSeek(String datasetCode, String assayID)
      throws URISyntaxException, IOException, InterruptedException {
    assayID = "3";
    System.out.println("Searching dataset in openBIS...");
    List<DataSet> datasets = openbis.findDataSets(
        Arrays.asList(datasetCode));
    if(datasets.isEmpty()) {
      return;
    }
    DataSet dataset = datasets.get(0);
    List<DataSetFile> files = openbis.getDatasetFiles(dataset);
    List<AssetToUpload> assets = seek.createAssets(files, dataset.getType().getCode(),
        Arrays.asList(assayID));
    for(AssetToUpload asset : assets) {
      System.out.printf("Streaming file %s from openBIS to SEEK...%n", asset.getFilePath());
      String fileURL = seek.uploadStreamContent(asset.getBlobEndpoint(),
          () -> openbis.streamDataset(datasetCode, asset.getFilePath()));
      System.out.printf("File stored here: %s%n", fileURL);
    }
  }

  private void cleanupTemp(File tmpFolder) {
    File[] files = tmpFolder.listFiles();
    if (files != null) { //some JVMs return null for empty dirs
      for (File f : files) {
        if (f.isDirectory()) {
          cleanupTemp(f);
        } else {
          f.delete();
        }
      }
    }
  }

}
