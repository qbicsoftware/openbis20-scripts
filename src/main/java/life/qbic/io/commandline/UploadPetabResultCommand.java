package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import life.qbic.App;
import life.qbic.io.PetabParser;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "upload-petab-result",
    description = "uploads a petab based on other PETab downloaded from openbis and attaches it to a provided experiment and any datasets referenced in the PETab metadata.")
public class UploadPetabResultCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "file/folder", description = "The path to the file or folder to upload")
  private String dataPath;
  @Parameters(arity = "1", paramLabel = "experiment ID", description = "The full identifier of the experiment the data should be attached to. "
      + "The identifier must be of the format: /space/project/experiment")
  private String experimentID;
  @Option(arity = "1..*", paramLabel = "<parent_datasets>", description = "Optional list of dataset codes to act"
      + " as parents for the upload. E.g. when this dataset has been generated using these datasets as input.", names = {"-pa", "--parents"})
  private List<String> parents = new ArrayList<>();
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();

  private OpenbisConnector openbis;
  private PetabParser petabParser = new PetabParser();

    @Override
    public void run() {
      OpenBIS authentication = App.loginToOpenBIS(auth.getPassword(), auth.getUser(), auth.getAS(), auth.getDSS());
      openbis = new OpenbisConnector(authentication);

      if(!pathValid(dataPath)) {
        System.out.printf("Path %s could not be found%n", dataPath);
        return;
      }
      if(!experimentExists(experimentID)) {
        System.out.printf("Experiment %s could not be found%n", experimentID);
        return;
      }
      parents = petabParser.parse(dataPath).getSourcePetabReferences();
      if(!datasetsExist(parents)) {
        System.out.printf("One or more datasets %s could not be found%n", parents);
        return;
      }
      System.out.println();
      System.out.println("Parameters verified, uploading dataset...");
      System.out.println();//TODO copy and remove source references
      DataSetPermId result = openbis.registerDataset(Path.of(dataPath), experimentID, parents);
      System.out.printf("Dataset %s was successfully created%n", result.getPermId());
    }

  private boolean datasetsExist(List<String> datasetCodes) {
      return openbis.findDataSets(datasetCodes).size() == datasetCodes.size();
  }

  private boolean experimentExists(String experimentID) {
      return openbis.experimentExists(experimentID);
  }

  private boolean pathValid(String dataPath) {
      return new File(dataPath).exists();
  }

}
