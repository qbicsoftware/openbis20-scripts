package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.App;
import life.qbic.model.DatasetWithProperties;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "list-data",
    description = "lists datasets and their details for a given experiment code")
public class FindDatasetsCommand implements Runnable {

  @Parameters(arity = "1", paramLabel = "experiment", description = "The code of the experiment data is attached to")
  private String experimentCode;
  @Option(arity = "1", paramLabel = "<space>", description = "Optional openBIS spaces to filter samples", names = {"-s", "--space"})
  private String space;
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();

    @Override
    public void run() {
      List<String> spaces = new ArrayList<>();
      if (space != null) {
        System.out.println("Querying experiment in space: " + space + "...");
        spaces.add(space);
      } else {
        System.out.println("Querying experiment in all available spaces...");
      }
      OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(), auth.getOpenbisAS());
      OpenbisConnector openbis = new OpenbisConnector(authentication);
      List<DataSet> datasets = openbis.listDatasetsOfExperiment(spaces, experimentCode).stream()
          .sorted(Comparator.comparing(
              (DataSet d) -> d.getExperiment().getProject().getSpace().getCode())).collect(
              Collectors.toList());
      Map<String, String> properties = new HashMap<>();
      if (!datasets.isEmpty()) {
        Optional<String> patientID = openbis.findPropertyInSampleHierarchy("PATIENT_DKFZ_ID",
            datasets.get(0).getExperiment().getIdentifier());
        patientID.ifPresent(s -> properties.put("Patient ID", s));
      }
      List<DatasetWithProperties> datasetWithProperties = datasets.stream().map(dataSet -> {
        DatasetWithProperties ds = new DatasetWithProperties(dataSet);
        for (String key : properties.keySet()) {
          ds.addProperty(key, properties.get(key));
        }
        return ds;
      }).collect(Collectors.toList());
      int datasetIndex = 0;
      System.out.println();
      System.out.printf("Found %s datasets for experiment %s:%n", datasets.size(), experimentCode);
      for (DatasetWithProperties dataSet : datasetWithProperties) {
        datasetIndex++;
        System.out.println("["+datasetIndex+"]");
        for(String key : dataSet.getProperties().keySet()) {
          System.out.println(key+ ": "+properties.get(key));
        }
        System.out.printf("ID: %s (%s)%n", dataSet.getCode(), dataSet.getExperiment().getIdentifier());
        System.out.println("Type: "+dataSet.getType().getCode());
        Person person = dataSet.getRegistrator();
        String simpleTime = new SimpleDateFormat("MM-dd-yy HH:mm:ss").format(dataSet.getRegistrationDate());
        String name = person.getFirstName() +" "+ person.getLastName();
        String uploadedBy = "Uploaded by "+name+" ("+simpleTime+")";
        System.out.println(uploadedBy);
        System.out.println();
      }
    }

}
