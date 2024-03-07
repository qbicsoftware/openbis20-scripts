package life.qbic.model.download;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * File system implementation of the ModelReporter interface.
 *
 * Provides methods to write the queried openBIS model to a file on the local file system.
 *
 * @author: Sven Fillinger, Andreas Friedrich
 */
class FileSystemWriter implements ModelReporter {

  /**
   * File that stores the summary report content for valid checksums.
   */
  final private File matchingSummaryFile;


  /**
   * FileSystemWriter constructor with the paths for the summary files.     *
   *
   * @param matchingSummaryFile The path where to write the summary
   */
  FileSystemWriter(Path matchingSummaryFile) {
    this.matchingSummaryFile = new File(matchingSummaryFile.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reportSummary(List<String> summary) {
    //TODO
  }

}
