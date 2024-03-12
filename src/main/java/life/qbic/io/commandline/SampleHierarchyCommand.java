package life.qbic.io.commandline;

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
import life.qbic.model.download.Authentication;
import life.qbic.model.download.FileSystemWriter;
import life.qbic.model.download.ModelReporter;
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
          summary.add("Querying samples in space: "+space+"...\n");
          spaces.add(space);
        } else {
          summary.add("Querying samples in all available spaces...\n");
        }
        Authentication authentication = App.loginToOpenBIS(auth.getPassword(), auth.user, auth.as_url);
        OpenbisConnector openbis = new OpenbisConnector(auth.as_url, authentication.getSessionToken());
        Map<SampleTypeConnection, Integer> hierarchy = openbis.queryFullSampleHierarchy(spaces);

        hierarchy.entrySet().stream()
            .sorted(Entry.comparingByValue())
            .forEach(entry -> summary.add(entry.getKey()+" ("+entry.getValue()+")"));

      for(String s : summary) {
        System.out.println(s);
      }
      Path outputPath = Paths.get(Configuration.LOG_PATH.toString(),
          "summary_model_"+getTimeStamp()+".txt");
      if(outpath!=null) {
        outputPath = Paths.get(outpath);
      }
      ModelReporter modelReporter = new FileSystemWriter(outputPath);
      try {
        modelReporter.reportSummary(summary);
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
