package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
  AuthenticationOptions auth = new AuthenticationOptions();

    @Override
    public void run() {
      OpenBIS authentication = App.loginToOpenBIS(auth.getPassword(), auth.getUser(), auth.getAS(), auth.getDSS());
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
      result.addProperty("openbisID", datasetCode);

      System.out.println("Found dataset, downloading.");
      System.out.println();

      openbis.downloadDataset(outputPath, datasetCode);

      System.out.println("Adding additional information to petab.yaml");
      System.out.println();

      PetabParser parser = new PetabParser();
      parser.addParameters(outputPath, result.getProperties());
      System.out.println("Done");
    }

}
