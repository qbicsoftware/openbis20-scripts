package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import life.qbic.App;
import life.qbic.model.Configuration;
import life.qbic.model.SampleTypeConnection;
import life.qbic.model.download.FileSystemWriter;
import life.qbic.model.download.SummaryWriter;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "sample-types",
    description = "lists sample types with children sample types and how often they are found in the openbis instance")
public class SampleHierarchyCommand implements Runnable {

  @Option(arity = "1", paramLabel = "<space>", description = "optional openBIS spaces to filter samples", names = {"-s", "--space"})
  private String space;
  @Option(arity = "1", paramLabel = "<output file path>", description = "optional output path", names = {"-o", "--out"})
  private String outpath;
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();

    @Override
    public void run() {
      List<String> summary = new ArrayList<>();
      List<String> spaces = new ArrayList<>();
        if(space!=null) {
          summary.add("Querying samples in space: "+space+"...");
          spaces.add(space);
        } else {
          summary.add("Querying samples in all available spaces...");
        }
        OpenBIS authentication = App.loginToOpenBIS(auth.getPassword(), auth.getUser(), auth.getAS());
        OpenbisConnector openbis = new OpenbisConnector(authentication);
        Map<SampleTypeConnection, Integer> hierarchy = openbis.queryFullSampleHierarchy(spaces);

        hierarchy.entrySet().stream()
            .sorted(Entry.comparingByValue())
            .forEach(entry -> summary.add(entry.getKey()+" ("+entry.getValue()+")"));

      for(String s : summary) {
        System.out.println(s);
      }
      Path outputPath = Paths.get(Configuration.LOG_PATH.toString(),
          "sample_model_summary"+getTimeStamp()+".txt");
      if(outpath!=null) {
        outputPath = Paths.get(outpath);
      }
      SummaryWriter summaryWriter = new FileSystemWriter(outputPath);
      try {
        summaryWriter.reportSummary(summary);
      } catch (IOException e) {
        throw new RuntimeException("Could not write summary file.");
      }
    }

    private String getTimeStamp() {
      final String PATTERN_FORMAT = "YYYY-MM-dd_HHmmss";
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT);
      return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(formatter).toString();
    }
}
