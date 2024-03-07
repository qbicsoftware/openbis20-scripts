package life.qbic.model.download;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

interface ModelReporter {

  void reportSummary(List<String> summary);
}
