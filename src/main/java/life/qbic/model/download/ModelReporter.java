package life.qbic.model.download;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public interface ModelReporter {

  void reportSummary(List<String> summary) throws IOException;
}
