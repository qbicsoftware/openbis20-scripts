package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.App;
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
      OpenBIS authentication = App.loginToOpenBIS(auth.getPassword(), auth.getUser(), auth.getAS());
      OpenbisConnector openbis = new OpenbisConnector(authentication);
      List<DataSet> datasets = openbis.listDatasetsOfExperiment(spaces, experimentCode).stream()
          .sorted(Comparator.comparing(
              (DataSet d) -> d.getExperiment().getProject().getSpace().getCode())).collect(
              Collectors.toList());
      int i = 0;
      System.out.println();
      System.out.printf("Found %s datasets for experiment %s:%n", datasets.size(), experimentCode);
      for (DataSet dataSet : datasets) {
        i++;
        System.out.println("["+i+"]");
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

    private String getTimeStamp() {
      final String PATTERN_FORMAT = "YYYY-MM-dd_HHmmss";
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT);
      return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(formatter);
    }
}
