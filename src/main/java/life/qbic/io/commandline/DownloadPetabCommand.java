package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import life.qbic.App;
import life.qbic.io.PetabParser;
import life.qbic.model.DatasetWithProperties;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = "download-petab",
    description = "Downloads PEtab dataset and stores some additional information from openbis in the petab.yaml")
public class DownloadPetabCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "dataset id", description = "The code of the dataset to download. Can be found via list-data.")
  private String datasetCode;
  @Parameters(arity = "1", paramLabel = "download path", description = "The local path where to store the downloaded data")
  private String outputPath;
  @Mixin
  OpenbisAuthenticationOptions auth = new OpenbisAuthenticationOptions();

    @Override
    public void run() {
      OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(),
          auth.getOpenbisAS(), auth.getOpenbisDSS());
      OpenbisConnector openbis = new OpenbisConnector(authentication);

      List<DataSet> datasets = openbis.findDataSets(Collections.singletonList(datasetCode));

      if(datasets.isEmpty()) {
        System.out.println(datasetCode+" not found");
        return;
      }
      DatasetWithProperties result = new DatasetWithProperties(datasets.get(0));
      Set<String> patientIDs = openbis.findPropertiesInSampleHierarchy("PATIENT_DKFZ_ID",
          result.getExperiment().getIdentifier());
      if(!patientIDs.isEmpty()) {
        result.addProperty("patientIDs", String.join(",", patientIDs));
      }

      System.out.println("Found dataset, downloading.");
      System.out.println();

      openbis.downloadDataset(outputPath, datasetCode, "");

      PetabParser parser = new PetabParser();
      try {
        System.out.println("Adding dataset identifier to metaInformation.yaml.");
        parser.addDatasetId(outputPath, datasetCode);
      } catch (IOException e) {
        System.out.println("Could not add dataset identifier.");
        throw new RuntimeException(e);
      }
      System.out.println("Done");
    }

}
