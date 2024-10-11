package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import life.qbic.App;
import life.qbic.model.AssayWithQueuedAssets;
import life.qbic.model.OpenbisExperimentWithDescendants;
import life.qbic.model.OpenbisSeekTranslator;
import life.qbic.model.download.SEEKConnector.SeekStructurePostRegistrationInformation;
import life.qbic.model.isa.SeekStructure;
import life.qbic.model.download.OpenbisConnector;
import life.qbic.model.download.SEEKConnector;
import life.qbic.model.download.SEEKConnector.AssetToUpload;
import org.apache.commons.codec.binary.Base64;
import org.xml.sax.SAXException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "openbis-to-seek",
    description =
        "Transfers metadata and (optionally) data from openBIS to SEEK. Experiments, samples and "
            + "dataset information are always transferred together (as assays, samples and one of "
            + "several data types in SEEK). Dataset info always links back to the openBIS path of "
            + "the respective dataset. The data itself can be transferred and stored in SEEK "
            + "using the '-d' flag."
            + "To completely exclude some dataset information from being transferred, a "
            + "file ('--blacklist') containing dataset codes (from openBIS) can be specified."
            + "Unless otherwise specified ('--no-update' flag), the command will try to update "
            + "existing nodes in SEEK (recognized by openBIS identifiers in their metadata).")
public class TransferDataToSeekCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "openbis id", description = "The identifier of the "
      + "experiment, sample or dataset to transfer.")
  private String objectID;
  @Parameters(arity = "1", paramLabel = "seek study", description = "Title of the study in SEEK"
      + "where nodes should be added.")
  private String studyTitle;
  @Option(names = "--seek-project", description = "Title of the project in SEEK where nodes should"
      + "be added. Can alternatively be provided via the config file as 'seek_default_project'.")
  private String projectTitle;
  @Option(names = "--blacklist", description = "Path to file specifying by dataset "
      + "dataset code which openBIS datasets not to transfer to SEEK. The file must contain one code "
      + "per line.")
  private String blacklistFile;
  @Option(names = "--no-update", description = "Use to specify that existing "
      + "information in SEEK for the specified openBIS input should not be updated, but new nodes "
      + "created.")
  private boolean noUpdate;
  @Option(names = {"-d", "--data"}, description =
      "Transfers the data itself to SEEK along with the metadata. "
          + "Otherwise only the link(s) to the openBIS object will be created in SEEK.")
  private boolean transferData;
  @Mixin
  SeekAuthenticationOptions seekAuth = new SeekAuthenticationOptions();
  @Mixin
  OpenbisAuthenticationOptions openbisAuth = new OpenbisAuthenticationOptions();
  OpenbisConnector openbis;
  SEEKConnector seek;
  OpenbisSeekTranslator translator;
  //500 MB - user will be informed that the transfer will take a while, for each file larger than this
  private final long FILE_WARNING_SIZE = 500*1024*1024;

  @Override
  public void run() {
    App.readConfig();
    System.out.printf("Transfer openBIS -> SEEK started.%n");
    System.out.printf("Provided openBIS object: %s%n", objectID);
    System.out.printf("Provided SEEK study title: %s%n", studyTitle);
    if(projectTitle!=null && !projectTitle.isBlank()) {
      System.out.printf("Provided SEEK project title: %s%n", projectTitle);
    } else {
      System.out.printf("No SEEK project title provided, will search config file.%n");
    }
    System.out.printf("Provided SEEK study title: %s%n", studyTitle);
    System.out.printf("Transfer datasets to SEEK? %s%n", transferData);
    System.out.printf("Update existing assay if found? %s%n", !noUpdate);
    if(blacklistFile!=null && !blacklistFile.isBlank()) {
      System.out.printf("File with datasets codes that won't be transferred: %s%n", blacklistFile);
    }

    System.out.println("Connecting to openBIS...");

    OpenBIS authentication = App.loginToOpenBIS(openbisAuth.getOpenbisPassword(),
        openbisAuth.getOpenbisUser(), openbisAuth.getOpenbisAS(), openbisAuth.getOpenbisDSS());

    openbis = new OpenbisConnector(authentication);

    boolean isSample = false;
    boolean isDataSet = false;

    System.out.println("Searching for specified object in openBIS...");

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
    System.out.println("Search successful.");
    System.out.println("Connecting to SEEK...");

    byte[] httpCredentials = Base64.encodeBase64(
        (seekAuth.getSeekUser() + ":" + new String(seekAuth.getSeekPassword())).getBytes());
    try {
      String project = App.configProperties.get("seek_default_project");
      if(project == null || project.isBlank()) {
        throw new RuntimeException("a default project must be provided via config "+
        "('seek_default_project') or parameter.");
      }
      seek = new SEEKConnector(seekAuth.getSeekURL(), httpCredentials,
          openbisAuth.getOpenbisBaseURL(), App.configProperties.get("seek_default_project"));
      seek.setDefaultStudy(studyTitle);
      translator = seek.getTranslator();
    } catch (URISyntaxException | IOException | InterruptedException |
             ParserConfigurationException | SAXException e) {
      throw new RuntimeException(e);
    }

    try {
      System.out.println("Collecting information from openBIS...");
      OpenbisExperimentWithDescendants experiment = openbis.getExperimentWithDescendants(objectID);
      System.out.println("Trying to find existing corresponding assay in SEEK...");
      Optional<String> assayID = getAssayIDForOpenBISExperiment(experiment.getExperiment());
      assayID.ifPresent(x -> System.out.println("Found assay with id "+assayID.get()));
      Set<String> blacklist = parseBlackList(blacklistFile);
      System.out.println("Translating openBIS property codes to SEEK names...");
      Map<String, String> sampleTypesToIds = seek.getSampleTypeNamesToIDs();
      System.out.println("Creating SEEK structure...");
      SeekStructure nodeWithChildren = translator.translate(experiment, sampleTypesToIds, blacklist,
          transferData);

      if(assayID.isEmpty() || noUpdate) {
        System.out.println("Creating new node(s)...");
        SeekStructurePostRegistrationInformation postRegInfo = createNewNodes(nodeWithChildren);
        System.out.println("Creating links to SEEK objects in openBIS...");
        openbis.createSeekLinks(postRegInfo);
      } else {
        System.out.println("Updating nodes...");
        SeekStructurePostRegistrationInformation postRegInfo = updateNodes(nodeWithChildren,
            assayID.get());
        System.out.println("Updating links to SEEK objects in openBIS...");
        openbis.updateSeekLinks(postRegInfo);
      }
    } catch (URISyntaxException | InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Done");
  }

  private Set<String> parseBlackList(String blacklistFile) {
    if(blacklistFile == null) {
      return new HashSet<>();
    }
    // trim whitespace, skip empty lines
    try (Stream<String> lines = Files.lines(Paths.get(blacklistFile))
        .map(String::trim)
        .filter(s -> !s.isBlank())) {

      Set<String> codes = lines.collect(Collectors.toSet());

      for(String code : codes) {
        if(!OpenbisConnector.datasetCodePattern.matcher(code).matches()) {
          throw new RuntimeException("Invalid dataset code: " + code+". Please make sure to use valid"
              + " dataset codes in the blacklist file.");
        }
      }
      return codes;
    } catch (IOException e) {
      throw new RuntimeException(blacklistFile+" could not be found or read.");
    }
  }

  private SeekStructurePostRegistrationInformation updateNodes(SeekStructure nodeWithChildren,
      String assayID) throws URISyntaxException, IOException, InterruptedException {
    SeekStructurePostRegistrationInformation postRegInfo = seek.updateNode(nodeWithChildren, assayID);
    System.out.printf("%s was successfully updated.%n", postRegInfo.getExperimentIDWithEndpoint().getRight());
    return postRegInfo;
  }

  private SeekStructurePostRegistrationInformation createNewNodes(SeekStructure nodeWithChildren)
      throws URISyntaxException, IOException, InterruptedException {
    SeekStructurePostRegistrationInformation postRegistrationInformation =
        seek.createNode(nodeWithChildren);
    AssayWithQueuedAssets assetsOfAssayToUpload = postRegistrationInformation.getAssayWithQueuedAssets();
    if(transferData) {
      final String tmpFolderPath = "tmp/";
      for(AssetToUpload asset : assetsOfAssayToUpload.getAssets()) {
        String filePath = asset.getFilePath();
        String dsCode = asset.getDataSetCode();
        if(asset.getFileSizeInBytes() > 1000*1024*1024) {
          System.out.printf("Skipping %s due to size...%n",
              filePath);
        } else if (asset.getFileSizeInBytes() > 300 * 1024 * 1024) {
          System.out.printf("File is %s MB...streaming might take a while%n",
              asset.getFileSizeInBytes() / (1024 * 1024));
          System.out.printf("Downloading file %s from openBIS to tmp folder due to size...%n",
              filePath);
          File tmpFile = openbis.downloadDataset(tmpFolderPath, dsCode, filePath);

          System.out.printf("Uploading file to SEEK...%n");
          String fileURL = seek.uploadFileContent(asset.getBlobEndpoint(), tmpFile.getAbsolutePath());
          System.out.printf("File stored here: %s%n", fileURL);
        } else {
          System.out.printf("Streaming file %s from openBIS to SEEK...%n", asset.getFilePath());

          String fileURL = seek.uploadStreamContent(asset.getBlobEndpoint(),
              () -> openbis.streamDataset(asset.getDataSetCode(), asset.getFilePath()));
          System.out.printf("File stored here: %s%n", fileURL);
        }
      }
      System.out.printf("Cleaning up temp folder%n");
      cleanupTemp(new File(tmpFolderPath));
    }

    System.out.printf("%s was successfully created.%n", assetsOfAssayToUpload.getAssayEndpoint());
    return postRegistrationInformation;
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

  private Optional<String> getAssayIDForOpenBISExperiment(Experiment experiment)
      throws URISyntaxException, IOException, InterruptedException {
    // the perm id is unique and afaik not used by scientists. it is highly unlikely that it would
    // "accidentally" be part of another title or description. however, care should be taken here,
    // because if a perm id is found in the wrong SEEK node, meta-information in SEEK could be
    // overwritten or samples/data added to the wrong assay.
    String permID = experiment.getPermId().getPermId();
    List<String> assayIDs = seek.searchAssaysContainingKeyword(permID);
    if(assayIDs.isEmpty()) {
      System.err.println("no assay found containing "+permID);
      return Optional.empty();
    }
    if(assayIDs.size() == 1) {
      return Optional.of(assayIDs.get(0));
    }
    throw new RuntimeException("Experiment identifier "+permID+ " was found in more than one assay: "+assayIDs);
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
