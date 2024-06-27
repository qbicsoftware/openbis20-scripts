package life.qbic;

import ch.ethz.sis.openbis.generic.OpenBIS;
import life.qbic.io.commandline.CommandLineOptions;
import life.qbic.model.Configuration;
import life.qbic.model.download.AuthenticationException;
import life.qbic.model.download.ConnectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;

/**
 * Scripts to perform different openBIS queries
 */
public class App {

  private static final Logger LOG = LogManager.getLogger(App.class);

  public static void main(String[] args) {
    LOG.debug("command line arguments: " + Arrays.deepToString(args));
    CommandLine cmd = new CommandLine(new CommandLineOptions());
    int exitCode = cmd.execute(args);
    System.exit(exitCode);
  }

  /**
   * checks if the commandline parameter for reading out the password from the environment variable
   * is correctly provided
   */
  private static Boolean isNotNullOrEmpty(String envVariableCommandLineParameter) {
    return envVariableCommandLineParameter != null && !envVariableCommandLineParameter.isEmpty();
  }

  /**
   * Logs into OpenBIS, asks for and verifies password.
   *
   * @return An instance of the Authentication class.
   */
  public static OpenBIS loginToOpenBIS(
      char[] password, String user, String url) {
    //setupLog();

    OpenBIS authentication = new OpenBIS(url);

    return tryLogin(authentication, user, password);
  }

  /**
   * Logs into OpenBIS, asks for and verifies password, includes Datastore Server connection.
   *
   * @return An instance of the Authentication class.
   */
  public static OpenBIS loginToOpenBIS(
      char[] password, String user, String url, String dssUrl) {
    //setupLog();

    OpenBIS authentication = new OpenBIS(url, dssUrl);

    return tryLogin(authentication, user, password);
  }

  private static OpenBIS tryLogin(OpenBIS authentication, String user, char[] password) {
    try {
      authentication.login(user, new String(password));
    } catch (ConnectionException e) {
      LOG.error(e.getMessage(), e);
      LOG.error("Could not connect to QBiC's data source. Have you requested access to the "
          + "server? If not please write to support@qbic.zendesk.com");
      System.exit(1);
    } catch (AuthenticationException e) {
      LOG.error(e.getMessage());
      System.exit(1);
    }
    return authentication;
  }

  private static void setupLog() {
    // Ensure 'logs' folder is created
    File logFolder = new File(Configuration.LOG_PATH.toAbsolutePath().toString());
    if (!logFolder.exists()) {
      boolean logFolderCreated = logFolder.mkdirs();
      if (!logFolderCreated) {
        LOG.error("Could not create log folder '" + logFolder.getAbsolutePath() + "'");
        System.exit(1);
      }
    }
  }
}
