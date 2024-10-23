package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import life.qbic.App;
import life.qbic.model.DatasetWithProperties;
import life.qbic.model.OpenbisExperimentWithDescendants;
import life.qbic.model.OpenbisSeekTranslator;
import life.qbic.model.download.OpenbisConnector;
import life.qbic.model.isa.GenericSeekAsset;
import life.qbic.model.isa.ISAAssay;
import life.qbic.model.isa.ISASample;
import life.qbic.model.isa.NodeType;
import life.qbic.model.isa.SeekStructure;
import org.xml.sax.SAXException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ro-crate",
    description =
        "Transfers metadata and (optionally) data from openBIS to an RO-Crate-like structure that is "
            + "based on assays, samples and one of several data types in SEEK). The data itself can "
            + "be put into the crate using the '-d' flag. To completely exclude some dataset "
            + "information from being transferred, a file ('--blacklist') containing dataset codes "
            + "can be specified. The crate is not zipped at the moment.")
public class CreateROCrate implements Runnable {

  @Parameters(arity = "1", paramLabel = "openbis id", description = "The identifier of the "
      + "experiment, sample or dataset to transfer.")
  private String objectID;
  @Parameters(arity = "1", paramLabel = "ro-path", description = "Path to the output folder")
  private String roPath;
  @Option(names = "--blacklist", description = "Path to file specifying by dataset "
      + "dataset code which openBIS datasets not to transfer to SEEK. The file must contain one code "
      + "per line.")
  private String blacklistFile;
  @Option(names = {"-d", "--data"}, description =
      "Transfers the data itself to SEEK along with the metadata. "
          + "Otherwise only the link(s) to the openBIS object will be created in SEEK.")
  private boolean transferData;
  @Mixin
  OpenbisAuthenticationOptions openbisAuth = new OpenbisAuthenticationOptions();
  OpenbisConnector openbis;
  OpenbisSeekTranslator translator;

  @Override
  public void run() {
    App.readConfig();
    System.out.printf("Transfer openBIS -> RO-crate started.%n");
    System.out.printf("Provided openBIS object: %s%n", objectID);
    System.out.printf("Pack datasets into crate? %s%n", transferData);
    if(blacklistFile!=null && !blacklistFile.isBlank()) {
      System.out.printf("File with datasets codes that won't be transferred: %s%n", blacklistFile);
    }

    System.out.println("Connecting to openBIS...");

    OpenBIS authentication = App.loginToOpenBIS(openbisAuth.getOpenbisPassword(),
        openbisAuth.getOpenbisUser(), openbisAuth.getOpenbisAS(), openbisAuth.getOpenbisDSS());

    this.openbis = new OpenbisConnector(authentication);

    System.out.println("Searching for specified object in openBIS...");

    boolean isExperiment = experimentExists(objectID);
    NodeType nodeType = NodeType.ASSAY;

    if (!isExperiment && sampleExists(objectID)) {
      nodeType = NodeType.SAMPLE;
    }

    if (!isExperiment && !nodeType.equals(NodeType.SAMPLE) && datasetsExist(
        Arrays.asList(objectID))) {
      nodeType = NodeType.ASSET;
    }

    if (nodeType.equals(NodeType.ASSAY) && !isExperiment) {
      System.out.printf(
          "%s could not be found in openBIS. Make sure you either specify an experiment, sample or dataset%n",
          objectID);
      return;
    }
    System.out.println("Search successful.");

    try {
      translator = new OpenbisSeekTranslator(openbisAuth.getOpenbisBaseURL());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new RuntimeException(e);
    }
    OpenbisExperimentWithDescendants structure;
    System.out.println("Collecting information from openBIS...");
    switch (nodeType) {
      case ASSAY:
        structure = openbis.getExperimentWithDescendants(objectID);
        break;
      case SAMPLE:
        structure = openbis.getExperimentAndDataFromSample(objectID);
        break;
      case ASSET:
        structure = openbis.getExperimentStructureFromDataset(objectID);
        break;
      default:
        throw new RuntimeException("Handling of node type " + nodeType + " is not supported.");
    }
    Set<String> blacklist = parseBlackList(blacklistFile);
    System.out.println("Translating openBIS structure to ISA structure...");
    try {
      SeekStructure nodeWithChildren = translator.translateForRO(structure, blacklist, transferData);
      String experimentID = nodeWithChildren.getAssayWithOpenBISReference().getRight();
      ISAAssay assay = nodeWithChildren.getAssayWithOpenBISReference().getLeft();
      String assayFileName = openbisIDToFileName(experimentID);

      String assayPath = Path.of(roPath, assayFileName).toString();
      new File(assayPath).mkdirs();

      System.out.printf("Writing assay json for %s.%n", experimentID);
      writeFile(Path.of(assayPath, assayFileName)+".json", assay.toJson());

      for(ISASample sample : nodeWithChildren.getSamplesWithOpenBISReference().keySet()) {
        String sampleID = nodeWithChildren.getSamplesWithOpenBISReference().get(sample);
        String sampleFileName = openbisIDToFileName(sampleID);
        String samplePath = Path.of(assayPath, sampleFileName).toString();
        new File(samplePath).mkdirs();

        System.out.printf("Writing sample json for %s.%n", sampleID);
        writeFile(Path.of(samplePath, sampleFileName)+".json", sample.toJson());
      }

      Map<String, String> datasetIDToDataFolder = new HashMap<>();

      for(DatasetWithProperties dwp : structure.getDatasets()) {
        String sourceID = dwp.getClosestSourceID();
        String code = dwp.getCode();
        if(sourceID.equals(experimentID)) {
          Path folderPath = Path.of(assayPath, code);
          File dataFolder = new File(folderPath.toString());
          datasetIDToDataFolder.put(dwp.getCode(), dataFolder.getAbsolutePath());
        } else {
          Path samplePath = Path.of(assayPath, openbisIDToFileName(sourceID), code);
          File dataFolder = new File(samplePath.toString());
          datasetIDToDataFolder.put(dwp.getCode(), dataFolder.getAbsolutePath());
        }
      }

      for(GenericSeekAsset asset : nodeWithChildren.getISAFileToDatasetFiles().keySet()) {
        DataSetFile file = nodeWithChildren.getISAFileToDatasetFiles().get(asset);
        String datasetID = file.getDataSetPermId().getPermId();
        String dataFolderPath = datasetIDToDataFolder.get(datasetID);
        String assetJson = asset.toJson();
        String assetWithoutOriginFolder = asset.getFileName().replace("original","");
        File assetFolder = Path.of(dataFolderPath, assetWithoutOriginFolder).getParent().toFile();
        assetFolder.mkdirs();

        String assetPath = Path.of(dataFolderPath, assetWithoutOriginFolder+".json").toString();
        System.out.printf("Writing asset json for file in dataset %s.%n", datasetID);
        writeFile(assetPath, assetJson);
        if(transferData) {
          System.out.printf("Downloading dataset file to asset folder.%n");
          openbis.downloadDataset(dataFolderPath, datasetID, asset.getFileName());
        }
      }
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println("Done");
  }

  private String openbisIDToFileName(String id) {
    id = id.replace("/","_");
    if(id.startsWith("_")) {
      return id.substring(1);
    } else {
      return id;
    }
  }

  private void writeFile(String path, String content) throws IOException {
    FileWriter file = new FileWriter(path);
    file.write(content);
    file.close();
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

  private boolean sampleExists(String objectID) {
    return openbis.sampleExists(objectID);
  }

  private boolean datasetsExist(List<String> datasetCodes) {
    return openbis.findDataSets(datasetCodes).size() == datasetCodes.size();
  }

  private boolean experimentExists(String experimentID) {
    return openbis.experimentExists(experimentID);
  }

}
