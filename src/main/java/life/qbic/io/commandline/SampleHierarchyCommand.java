package life.qbic.io.commandline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import life.qbic.App;
import life.qbic.model.SampleTypeConnection;
import life.qbic.model.download.Authentication;
import life.qbic.model.download.OpenbisConnector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "sample-types",
    description = "lists sample types with children sample types and how often they are found in the openbis instance")
public class SampleHierarchyCommand implements Runnable {

  @Option(arity = "1", paramLabel = "<space>", description = "optional openBIS spaces to filter samples", names = {"-s", "--space"})
  private String space;
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();

    @Override
    public void run() {
        List<String> spaces = new ArrayList<>();
        if(space!=null) {
          spaces.add(space);
        }
        Authentication authentication = App.loginToOpenBIS(auth.getPassword(), auth.user, auth.as_url);
        OpenbisConnector openbis = new OpenbisConnector(auth.as_url, authentication.getSessionToken(), "/Users/afriedrich/Downloads/");
        Map<SampleTypeConnection, Integer> hierarchy =     openbis.queryFullSampleHierarchy(spaces);
        hierarchy.entrySet().stream()
            .sorted(Entry.comparingByValue())
            .forEach(entry -> System.err.println(entry.getKey()+" ("+entry.getValue()+")"));
    }
}
