package life.qbic.io.commandline;

import static java.util.Objects.nonNull;
import static picocli.CommandLine.ArgGroup;

import java.util.StringJoiner;
import life.qbic.App;
import life.qbic.io.PropertyReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class SeekAuthenticationOptions {
  private static final Logger log = LogManager.getLogger(SeekAuthenticationOptions.class);

  @ArgGroup(multiplicity = "1")
  SeekPasswordOptions seekPasswordOptions;

  @Option(
      names = {"-su", "--seek-user"},
      description = "Seek user name (email)",
      scope = CommandLine.ScopeType.INHERIT)
  private String seekUser;
  @Option(
      names = {"-seek-server", "-seek_url"},
      description = "SEEK API URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String seek_url;

  public String getSeekUser() {
    if(seekUser == null && App.configProperties.containsKey("seek_user")) {
      seekUser = App.configProperties.get("seek_user");
    } else {
      log.error("No SEEK user/email provided.");
      System.exit(2);
    }
    return seekUser;
  }

  public String getSeekURL() {
    if(seek_url == null && App.configProperties.containsKey("seek_url")) {
      seek_url = App.configProperties.get("seek_url");
    } else {
      log.error("No URL to the SEEK address provided.");
      System.exit(2);
    }
    return seek_url;
  }

  public char[] getSeekPassword() {
    return seekPasswordOptions.getPassword();
  }

  static class SeekPasswordOptions {
    @Option(names = "--seek-pw:env", arity = "1", paramLabel = "<environment-variable>", description = "provide the name of an environment variable to read in your password from")
    protected String passwordEnvironmentVariable = "";

    @Option(names = "--seek-pw:prop", arity = "1", paramLabel = "<system-property>", description = "provide the name of a system property to read in your password from")
    protected String passwordProperty = "";

    @Option(names = "--seek-pw", arity = "0", description = "please provide your SEEK password", interactive = true)
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
    return new StringJoiner(", ", SeekAuthenticationOptions.class.getSimpleName() + "[", "]")
        .add("user='" + seekUser + "'")
        .toString();
    //ATTENTION: do not expose the password here!
  }
}