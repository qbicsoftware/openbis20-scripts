package life.qbic.model.download;

import java.io.IOException;
import java.util.List;

public interface SummaryWriter {

  void reportSummary(List<String> summary) throws IOException;
}
