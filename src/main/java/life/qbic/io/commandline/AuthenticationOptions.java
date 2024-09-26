package life.qbic.io.commandline;

import static java.util.Objects.nonNull;
import static picocli.CommandLine.ArgGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class AuthenticationOptions {
  private static final Logger log = LogManager.getLogger(AuthenticationOptions.class);

  @Option(
      names = {"-u", "--user"},
      description = "openBIS user name")
  private String openbisUser;
  @ArgGroup(multiplicity = "1") // ensures the password is provided once with at least one of the possible options.
  OpenbisPasswordOptions openbisPasswordOptions;
  @ArgGroup(multiplicity = "1")
  SeekPasswordOptions seekPasswordOptions;

  @Option(
      names = {"-as", "-as_url"},
      description = "ApplicationServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String as_url;

  @Option(
      names = {"-dss", "--dss_url"},
      description = "DatastoreServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String dss_url;

  @Option(
      names = {"-config", "--config_file"},
      description = "Config file path to provide server and user information.",
      scope = CommandLine.ScopeType.INHERIT)
  public String configPath;
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

  public String getOpenbisUser() {
    if(openbisUser == null & configPath!=null && !configPath.isBlank()) {
      openbisUser = ReadProperties.getProperties(configPath).get("user");
    } else {
      log.error("No openBIS user provided.");
      System.exit(2);
    }
    return openbisUser;
  }

  public String getSeekUser() {
    if(seekUser == null & configPath!=null && !configPath.isBlank()) {
      seekUser = ReadProperties.getProperties(configPath).get("seek_user");
    } else {
      log.error("No SEEK user/email provided.");
      System.exit(2);
    }
    return seekUser;
  }

  public String getSeekURL() {
    if(seek_url == null & configPath!=null && !configPath.isBlank()) {
      seek_url = ReadProperties.getProperties(configPath).get("seek_url");
    } else {
      log.error("No URL to the SEEK address provided.");
      System.exit(2);
    }
    return seek_url;
  }

  public String getDSS() {
    if(dss_url == null & configPath!=null && !configPath.isBlank()) {
      dss_url = ReadProperties.getProperties(configPath).get("dss");
    }
    return dss_url;
  }

  public String getAS() {
    if(as_url == null & configPath!=null && !configPath.isBlank()) {
      as_url = ReadProperties.getProperties(configPath).get("as");
    }
    return as_url;
  }

  public char[] getSeekPassword() {
    return seekPasswordOptions.getPassword();
  }

  public char[] getOpenbisPassword() {
    return openbisPasswordOptions.getPassword();
  }

  /**
   * <a href="https://picocli.info/#_optionally_interactive">official picocli documentation example</a>
   */
  static class OpenbisPasswordOptions {
    @Option(names = "--openbis-pw:env", arity = "1", paramLabel = "<environment-variable>", description = "provide the name of an environment variable to read in your password from")
    protected String passwordEnvironmentVariable = "";

    @Option(names = "--openbis-pw:prop", arity = "1", paramLabel = "<system-property>", description = "provide the name of a system property to read in your password from")
    protected String passwordProperty = "";

    @Option(names = "--openbis-pw", arity = "0", description = "please provide your openBIS password", interactive = true)
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
    return new StringJoiner(", ", AuthenticationOptions.class.getSimpleName() + "[", "]")
        .add("user='" + openbisUser + "'")
        .toString();
    //ATTENTION: do not expose the password here!
  }

  public static class ReadProperties {

    public static TreeMap<String, String> getProperties(String infile) {

      TreeMap<String, String> properties = new TreeMap<>();
      BufferedReader  bfr = null;
      try {
        bfr = new BufferedReader(new FileReader(new File(infile)));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }

      String line;
      while (true) {
        try {
          if ((line = bfr.readLine()) == null)
            break;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (!line.startsWith("#") && !line.isEmpty()) {
          String[] property = line.trim().split("=");
          properties.put(property[0].trim(), property[1].trim());
        }
      }

      try {
        bfr.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return(properties);
    }
  }
}