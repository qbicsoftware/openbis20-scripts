package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import life.qbic.App;
import life.qbic.model.DatasetWithProperties;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "openbis-to-seek",
    description = "Transfers data or metadata from openBIS to SEEK.")
public class TransferDataToSeekCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "dataset id", description = "The code of the dataset (or its metadata) to transfer. Can be found via list-data.")
  private String datasetCode;
  @Parameters(arity = "1", paramLabel = "seek node", description = "The node in SEEK to which to transfer the dataset.")
  private String seekNode;
  @Option(names = { "-d", "--data"}, usageHelp = true, description = "Transfers the data itself to SEEK along with the metadata")
  private boolean transferData;
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();

    @Override
    public void run() {
      OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(), auth.getAS(), auth.getDSS());
      OpenbisConnector openbis = new OpenbisConnector(authentication);

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

      System.out.println("Done");
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
