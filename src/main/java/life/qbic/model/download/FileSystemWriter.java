package life.qbic.model.download;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * File system implementation of the ModelReporter interface.
 *
 * Provides methods to write the queried openBIS model to a file on the local file system.
 *
 * @author: Sven Fillinger, Andreas Friedrich
 */
public class FileSystemWriter implements SummaryWriter {

  /**
   * File that stores the summary report content for valid checksums.
   */
  final private File summaryFile;


  /**
   * FileSystemWriter constructor with the paths for the summary files.
   *
   * @param summaryFile The path where to write the summary
   */
  public FileSystemWriter(Path summaryFile) {
    this.summaryFile = new File(summaryFile.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reportSummary(List<String> summary) throws IOException {
    if (!summaryFile.exists()) {
      summaryFile.createNewFile();
      //file exists or could not be created
      if (!summaryFile.exists()) {
        throw new IOException("The file " + summaryFile.getAbsoluteFile() + " could not be created.");
      }
    }
    BufferedWriter writer = new BufferedWriter(new FileWriter(summaryFile, true));
    for(String line : summary) {
      writer.append(line+"\n");
    }
    writer.close();
  }

}
