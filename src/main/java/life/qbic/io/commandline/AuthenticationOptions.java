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
  private String user;
  @ArgGroup(multiplicity = "1") // ensures the password is provided once with at least one of the possible options.
  PasswordOptions passwordOptions;

  @Option(
      names = {"-as", "-as_url"},
      description = "ApplicationServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String as_url;

  @Option(
      names = {"-dss", "-dss_url"},
      description = "DatastoreServer URL",
      scope = CommandLine.ScopeType.INHERIT)
  private String dss_url;

  @Option(
      names = {"-config", "-config_file"},
      description = "Config file path to provide openbis server information.",
      scope = CommandLine.ScopeType.INHERIT)
  public String configPath;

  public String getUser() {
    if(user == null & configPath!=null && !configPath.isBlank()) {
      user = ReadProperties.getProperties(configPath).get("user");
    }
    return user;
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

  public char[] getPassword() {
    return passwordOptions.getPassword();
  }

  /**
   * <a href="https://picocli.info/#_optionally_interactive">official picocli documentation example</a>
   */
  static class PasswordOptions {
    @Option(names = "--password:env", arity = "1", paramLabel = "<environment-variable>", description = "provide the name of an environment variable to read in your password from")
    protected String passwordEnvironmentVariable = "";

    @Option(names = "--password:prop", arity = "1", paramLabel = "<system-property>", description = "provide the name of a system property to read in your password from")
    protected String passwordProperty = "";

    @Option(names = "--password", arity = "0", description = "please provide your password", interactive = true)
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
        .add("user='" + user + "'")
        .toString();
    //ATTENTION: do not expose the password here!
  }

  public static class ReadProperties {

    public static TreeMap<String, String> getProperties(String infile) {
      final int lhs = 0;
      final int rhs = 1;

      TreeMap<String, String> map = new TreeMap<String, String>();
      BufferedReader  bfr = null;
      try {
        bfr = new BufferedReader(new FileReader(new File(infile)));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }

      String line;
      while (true) {
        try {
          if (!((line = bfr.readLine()) != null))
            break;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (!line.startsWith("#") && !line.isEmpty()) {
          String[] pair = line.trim().split("=");
          map.put(pair[lhs].trim(), pair[rhs].trim());
        }
      }

      try {
        bfr.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return(map);
    }
  }
}