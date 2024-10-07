package life.qbic.io.commandline;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import java.io.IOException;
import java.net.URISyntaxException;
import life.qbic.App;
import life.qbic.model.OpenbisSeekTranslator;
import life.qbic.model.SampleTypesAndMaterials;
import life.qbic.model.download.OpenbisConnector;
import life.qbic.model.download.SEEKConnector;
import org.apache.commons.codec.binary.Base64;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "sample-type-transfer",
    description =
        "Transfers sample types from openBIS to SEEK.")
public class TransferSampleTypesToSeekCommand implements Runnable {
  @Mixin
  AuthenticationOptions auth = new AuthenticationOptions();
  OpenbisConnector openbis;
  SEEKConnector seek;
  OpenbisSeekTranslator translator;

  @Override
  public void run() {
    System.out.println("auth...");

    OpenBIS authentication = App.loginToOpenBIS(auth.getOpenbisPassword(), auth.getOpenbisUser(),
        auth.getOpenbisAS(), auth.getOpenbisDSS());
    System.out.println("openbis...");

    openbis = new OpenbisConnector(authentication);

    byte[] httpCredentials = Base64.encodeBase64(
        (auth.getSeekUser() + ":" + new String(auth.getSeekPassword())).getBytes());
    try {
      seek = new SEEKConnector(auth.getSeekURL(), httpCredentials, auth.getOpenbisBaseURL(),
          "seek_test", "lisym default study");
      translator = seek.getTranslator();
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    SampleTypesAndMaterials types = openbis.getSampleTypesWithMaterials();

    try {
      for(SampleType type : types.getSampleTypes()) {
        System.err.println("creating "+type.getCode());
        String sampleTypeId = seek.createSampleType(translator.translate(type));
        System.err.println("created "+sampleTypeId);
      }
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    System.out.println("Done");
  }

}
