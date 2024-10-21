package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import life.qbic.App;
import life.qbic.model.Configuration;
import life.qbic.model.download.FileSystemWriter;
import life.qbic.model.download.SummaryWriter;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * The Statistics command can be used to list the number of collections, sample objects and attached
 * datasets by type for one or all spaces accessible by the user.
 * The --space command can be used to only show the objects in a specific openBIS space.
 * An output file for the resulting list can be specified using the --out command.
 * By default, openBIS settings objects and material spaces are ignored. This can be overwritten
 * using --show-settings.
 */
@Command(name = "statistics",
    description = "lists the number of collections, sample objects and attached datasets (by type)"
        + "for one or all spaces accessible by the user")
public class SpaceStatisticsCommand implements Runnable {

  @Option(arity = "1", paramLabel = "<space>", description = "optional openBIS space to filter "
      + "samples", names = {"-s", "--space"})
  private String space;
  @Option(arity = "1", paramLabel = "<output file path>", description = "optional output path",
      names = {"-o", "--out"})
  private String outpath;
  @Option(arity = "0", description = "shows results for openBIS settings and material spaces. "
      + "Ignored if a specific space is selected.",
      names = {"--show-settings"})
  private boolean allSpaces;
  @Mixin
  OpenbisAuthenticationOptions auth = new OpenbisAuthenticationOptions();

    @Override
    public void run() {
      App.readConfig();
      List<String> summary = new ArrayList<>();
      List<String> blackList = new ArrayList<>(Arrays.asList("ELN_SETTINGS", "MATERIAL.GLOBAL"));
      List<String> spaces = new ArrayList<>();
      if (space != null) {
        summary.add("Querying samples in space: " + space);
        spaces.add(space);
      } else {
        summary.add("Querying samples in all available spaces...");
      }
      OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(), auth.getOpenbisAS());
      OpenbisConnector openbis = new OpenbisConnector(authentication);

      if (spaces.isEmpty()) {
        spaces = openbis.getSpaces();
        if(!allSpaces) {
          spaces.removeAll(blackList);
        }
      }

      Map<String, Map<String, List<Experiment>>> experiments = openbis.getExperimentsByTypeAndSpace(spaces);
      Map<String, Map<String, List<Sample>>> samples = openbis.getSamplesByTypeAndSpace(spaces);
      Map<String, Map<String, List<DataSet>>> datasets = openbis.getDatasetsByTypeAndSpace(spaces);

      for(String space : spaces) {
        summary.add("-----");
        summary.add("Summary for "+space);
        summary.add("-----");
        int numExps = 0;
        if (experiments.containsKey(space)) {
          numExps = experiments.get(space).values().stream().mapToInt(List::size).sum();
        }
        summary.add("Experiments ("+numExps+"):");
        summary.add("");
        if(!experiments.isEmpty()) {
          Map<String, List<Experiment>> exps = experiments.get(space);
          for (String type : exps.keySet()) {
            summary.add(type + ": " + exps.get(type).size());
          }
        }
        summary.add("");
        int numSamples = 0;
        if (samples.containsKey(space)) {
          numSamples = samples.get(space).values().stream().mapToInt(List::size).sum();
        }
        summary.add("Samples ("+numSamples+"):");
        summary.add("");
        if(!samples.isEmpty()) {
          Map<String, List<Sample>> samps = samples.get(space);
          for (String type : samps.keySet()) {
            summary.add(type + ": " + samps.get(type).size());
          }
        }
        summary.add("");
        int numData = 0;
        if (datasets.containsKey(space)) {
          numData = datasets.get(space).values().stream().mapToInt(List::size).sum();
        }
        summary.add("Attached datasets (" + numData + "):");
        summary.add("");
        if (datasets.get(space) != null) {
          Map<String, List<DataSet>> dsets = datasets.get(space);
          for (String dataType : dsets.keySet()) {
            summary.add(dataType + ": " + dsets.get(dataType).size());
          }
        }

        summary.add("");
      }

      for(String line : summary) {
        System.out.println(line);
      }

      Path outputPath = Paths.get(Configuration.LOG_PATH.toString(),
          "spaces_summary_"+getTimeStamp()+".txt");
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
