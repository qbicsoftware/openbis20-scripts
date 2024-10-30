package life.qbic.io.commandline;

import static java.util.Objects.nonNull;
import static picocli.CommandLine.ArgGroup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringJoiner;
import life.qbic.App;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class OpenbisAuthenticationOptions {
  private static final Logger log = LogManager.getLogger(OpenbisAuthenticationOptions.class);

  @Option(
      names = {"-u", "--user"},
      description = "openBIS user name")
  private String openbisUser;
  @ArgGroup(multiplicity = "1") // ensures the password is provided once with at least one of the possible options.
  OpenbisPasswordOptions openbisPasswordOptions;

  @Option(
      names = {"-as", "-as_url"},
      description = "OpenBIS ApplicationServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String as_url;

  @Option(
      names = {"-dss", "--dss_url"},
      description = "OpenBIS DatastoreServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String dss_url;

  public String getOpenbisUser() {
    if(openbisUser == null && App.configProperties.containsKey("user")) {
      openbisUser = App.configProperties.get("user");
    }
    if(openbisUser == null) {
      log.error("No openBIS user provided.");
      System.exit(2);
    }
    return openbisUser;
  }

  public String getOpenbisDSS() {
    if(dss_url == null && App.configProperties.containsKey("dss")) {
      dss_url = App.configProperties.get("dss");
    }
    if(dss_url == null) {
      log.error("No openBIS datastore server URL provided.");
      System.exit(2);
    }
    return dss_url;
  }

  public String getOpenbisAS() {
    if(as_url == null && App.configProperties.containsKey("as")) {
      as_url = App.configProperties.get("as");
    }
    if(as_url == null) {
      log.error("No openBIS application server URL provided.");
      System.exit(2);
    }
    return as_url;
  }

  public char[] getOpenbisPassword() {
    return openbisPasswordOptions.getPassword();
  }

  public String getOpenbisBaseURL() throws MalformedURLException {
    URL asURL = new URL(as_url);
    String res = asURL.getProtocol()+ "://" +asURL.getHost();
    if(asURL.getPort()!=-1) {
      res+=":"+asURL.getPort();
    }
    return res;
  }

  /**
   * <a href="https://picocli.info/#_optionally_interactive">official picocli documentation example</a>
   */
  static class OpenbisPasswordOptions {
    @Option(names = "--openbis-pw:env", arity = "1", paramLabel = "<environment-variable>",
        description = "provide the name of an environment variable to read in your password from")
    protected String passwordEnvironmentVariable = "";

    @Option(names = "--openbis-pw:prop", arity = "1", paramLabel = "<system-property>",
        description = "provide the name of a system property to read in your password from")
    protected String passwordProperty = "";

    @Option(names = "--openbis-pw", arity = "0",
        description = "please provide your openBIS password", interactive = true)
    protected char[] password = null;

    /**
     * Gets the password. If no password is provided, the program exits.
     * @return the password provided by the user.
     */
    char[] getPassword() {
      if (nonNull(password)) {
        return password;
      }
      // System.getProperty(String key) does not work for empty or blank keys.
      if (!passwordProperty.isBlank()) {
        String systemProperty = System.getProperty(passwordProperty);
        if (nonNull(systemProperty)) {
          return systemProperty.toCharArray();
        }
      }
      String environmentVariable = System.getenv(passwordEnvironmentVariable);
      if (nonNull(environmentVariable) && !environmentVariable.isBlank()) {
        return environmentVariable.toCharArray();
      }
      log.error("No password provided. Please provide your password.");
      System.exit(2);
      return null; // not reachable due to System.exit in previous line
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", OpenbisAuthenticationOptions.class.getSimpleName() + "[", "]")
        .add("user='" + openbisUser + "'")
        .toString();
    //ATTENTION: do not expose the password here!
  }

}