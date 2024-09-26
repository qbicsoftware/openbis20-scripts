package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import life.qbic.App;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "upload-data",
    description = "uploads a dataset and attaches it to an experiment and (optionally) other datasets")
public class UploadDatasetCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "file/folder", description = "The path to the file or folder to upload")
  private String dataPath;
  @Parameters(arity = "1", paramLabel = "object ID", description = "The full identifier of the experiment or sample the data should be attached to. "
      + "The identifier must be of the format: /space/project/experiment for experiments or /space/sample for samples")
  private String objectID;
  @Option(arity = "1..*", paramLabel = "<parent_datasets>", description = "Optional list of dataset codes to act"
      + " as parents for the upload. E.g. when this dataset has been generated using these datasets as input.", names = {"-pa", "--parents"})
  private List<String> parents = new ArrayList<>();
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();

  private OpenbisConnector openbis;

    @Override
    public void run() {
      OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(), auth.getAS(), auth.getDSS());
      openbis = new OpenbisConnector(authentication);

      if(!pathValid(dataPath)) {
        System.out.printf("Path %s could not be found%n", dataPath);
        return;
      }
      boolean attachToSample = false;
      boolean attachToExperiment = experimentExists(objectID);
      if(sampleExists(objectID)) {
        attachToSample = true;
      }
      if(!attachToSample && !attachToExperiment) {
        System.out.printf("%s could not be found in openBIS.%n", objectID);
        return;
      }
      if(!datasetsExist(parents)) {
        System.out.printf("One or more datasets %s could not be found%n", parents);
        return;
      }
      System.out.println();
      System.out.println("Parameters verified, uploading dataset...");
      System.out.println();
      if(attachToExperiment) {
        DataSetPermId result = openbis.registerDatasetForExperiment(Path.of(dataPath), objectID, parents);
        System.out.printf("Dataset %s was successfully attached to experiment%n", result.getPermId());
      } else {
        DataSetPermId result = openbis.registerDatasetForSample(Path.of(dataPath), objectID, parents);
        System.out.printf("Dataset %s was successfully attached to sample%n", result.getPermId());
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

  private boolean pathValid(String dataPath) {
      return new File(dataPath).exists();
  }

  
}
